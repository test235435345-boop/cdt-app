package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.FirestoreRepository
import com.example.data.GeminiService
import com.example.data.GoogleSheetsSyncService
import com.example.data.PdfReportService
import com.example.data.ThemeMode
import com.example.data.UserPreferencesRepository
import com.example.model.AttendanceRecord
import com.example.model.BandEvent
import com.example.model.Cadet
import com.example.model.CadetAttendanceStats
import com.example.model.ChatMessage
import com.example.model.ChatSession
import com.example.model.OrgSettings
import com.example.model.SquadronReport
import com.example.model.StaffMember
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AttendanceUiState(
    val selectedEvent: BandEvent? = null,
    val searchQuery: String = "",
    val filterStatus: String = "All", // "All", "Unmarked", "Present", "Absent", "Late", "Excused"
    val filterSquadron: String = "All"
)

data class ReportsUiState(
    val selectedSquadron: String = "All",
    val selectedFlight: String = "All",
    val selectedRank: String = "All",
    val selectedEventType: String = "All"
)

class CadetTrackViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val authRepo = AuthRepository()
    private val firestoreRepo = FirestoreRepository()
    private val sheetsSyncService = GoogleSheetsSyncService(application)
    private val pdfReportService = PdfReportService(application)
    private val geminiService = GeminiService()
    private val userPreferencesRepo = UserPreferencesRepository(application)

    private val _lastPdfUpdated = MutableStateFlow<Long>(pdfReportService.getCanonicalPdfLastModified())
    val lastPdfUpdated: StateFlow<Long> = _lastPdfUpdated.asStateFlow()

    private val _isPdfGenerating = MutableStateFlow(false)
    val isPdfGenerating: StateFlow<Boolean> = _isPdfGenerating.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = userPreferencesRepo.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepo.setThemeMode(mode)
        }
    }

    // --- Firebase Initialization Check ---
    private val _isFirebaseConfigured = MutableStateFlow(true)
    val isFirebaseConfigured: StateFlow<Boolean> = _isFirebaseConfigured.asStateFlow()

    // --- Auth & Staff State ---
    val currentUser: StateFlow<FirebaseUser?> = authRepo.authStateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepo.currentUser)

    private val _currentStaff = MutableStateFlow<StaffMember?>(null)
    val currentStaff: StateFlow<StaffMember?> = _currentStaff.asStateFlow()

    private val _profileLoadError = MutableStateFlow<String?>(null)
    val profileLoadError: StateFlow<String?> = _profileLoadError.asStateFlow()

    private val _isProfileLoading = MutableStateFlow(false)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // --- Core Data Flows ---
    private val cachedInitialSettings = loadPersistedOrgSettings()
    private val _orgSettings = MutableStateFlow(cachedInitialSettings)
    val orgSettings: StateFlow<OrgSettings> = _orgSettings.asStateFlow()

    private val _isOrgSettingsLoaded = MutableStateFlow(cachedInitialSettings.orgName.isNotBlank())
    val isOrgSettingsLoaded: StateFlow<Boolean> = _isOrgSettingsLoaded.asStateFlow()

    val staffMembers: StateFlow<List<StaffMember>> = firestoreRepo.observeAllStaff()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingStaffRequests: StateFlow<List<StaffMember>> = staffMembers
        .map { list -> list.filter { !it.isAuthorized } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cadets = MutableStateFlow<List<Cadet>>(loadPersistedCadets())
    val cadets: StateFlow<List<Cadet>> = _cadets.asStateFlow()

    private val _events = MutableStateFlow<List<BandEvent>>(loadPersistedEvents())
    val events: StateFlow<List<BandEvent>> = _events.asStateFlow()

    private val _allAttendanceRecords = MutableStateFlow<List<AttendanceRecord>>(loadPersistedAttendance())
    val allAttendanceRecords: StateFlow<List<AttendanceRecord>> = _allAttendanceRecords.asStateFlow()

    // --- Attendance Screen State ---
    private val _attendanceUiState = MutableStateFlow(AttendanceUiState())
    val attendanceUiState: StateFlow<AttendanceUiState> = _attendanceUiState.asStateFlow()

    private val _undoSnapshot = MutableStateFlow<List<AttendanceRecord>?>(null)
    val undoSnapshot: StateFlow<List<AttendanceRecord>?> = _undoSnapshot.asStateFlow()

    // --- Reports Screen State ---
    private val _reportsUiState = MutableStateFlow(ReportsUiState())
    val reportsUiState: StateFlow<ReportsUiState> = _reportsUiState.asStateFlow()

    // --- Sheets Sync State ---
    private val _googleOAuthToken = MutableStateFlow<String?>(null)
    val googleOAuthToken: StateFlow<String?> = _googleOAuthToken.asStateFlow()

    fun setGoogleOAuthToken(token: String?) {
        _googleOAuthToken.value = token
    }

    private val _isSheetsSyncing = MutableStateFlow(false)
    val isSheetsSyncing: StateFlow<Boolean> = _isSheetsSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun setSyncMessage(msg: String?) {
        _syncMessage.value = msg
    }

    // --- Gemini Chat State & Multi-Session System ---
    private val defaultInitialMessage = ChatMessage(
        sender = "gemini",
        text = "CadetTrack Intelligence ready. Ask any question regarding cadet attendance, squadron comparisons, or generate guardian notification drafts."
    )

    private val _chatSessions = MutableStateFlow<List<ChatSession>>(
        listOf(
            ChatSession(
                id = "default_session",
                title = "Attendance Assistant",
                messages = listOf(defaultInitialMessage)
            )
        )
    )
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow("default_session")
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(defaultInitialMessage))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    init {
        checkFirebaseConfig()
        observeCurrentUserStaffProfile()
        observeOrgSettingsRealtime()
        observeCadetsRealtime()
        observeEventsRealtime()
        observeAttendanceRealtime()
        loadPersistedChatSessions()
    }

    private fun observeCadetsRealtime() {
        viewModelScope.launch {
            firestoreRepo.observeCadets().collect { remoteCadets ->
                if (remoteCadets.isNotEmpty()) {
                    _cadets.value = remoteCadets
                    persistCadets(remoteCadets)
                }
            }
        }
    }

    private fun observeEventsRealtime() {
        viewModelScope.launch {
            firestoreRepo.observeEvents().collect { remoteEvents ->
                if (remoteEvents.isNotEmpty()) {
                    _events.value = remoteEvents
                    persistEvents(remoteEvents)
                    if (_attendanceUiState.value.selectedEvent == null && remoteEvents.isNotEmpty()) {
                        val active = remoteEvents.firstOrNull { !it.isCancelled } ?: remoteEvents.first()
                        _attendanceUiState.value = _attendanceUiState.value.copy(selectedEvent = active)
                    }
                }
            }
        }
    }

    private fun observeAttendanceRealtime() {
        viewModelScope.launch {
            firestoreRepo.observeAllAttendance().collect { remoteAttendance ->
                if (remoteAttendance.isNotEmpty()) {
                    _allAttendanceRecords.value = remoteAttendance
                    persistAttendance(remoteAttendance)
                }
            }
        }
    }

    private fun observeOrgSettingsRealtime() {
        viewModelScope.launch {
            firestoreRepo.observeOrgSettings().collect { settings ->
                _isOrgSettingsLoaded.value = true
                if (settings != null && settings.orgName.isNotBlank()) {
                    _orgSettings.value = settings
                    persistOrgSettings(settings)
                }
            }
        }
    }

    private fun checkFirebaseConfig() {
        try {
            val apps = FirebaseApp.getApps(getApplication())
            _isFirebaseConfigured.value = apps.isNotEmpty()
        } catch (e: Exception) {
            _isFirebaseConfigured.value = false
        }
    }

    private var staffProfileJob: kotlinx.coroutines.Job? = null

    fun retryLoadProfile() {
        _profileLoadError.value = null
        loadStaffProfileForUser(currentUser.value ?: authRepo.currentUser)
    }

    private fun observeCurrentUserStaffProfile() {
        viewModelScope.launch {
            currentUser.collect { user ->
                loadStaffProfileForUser(user)
            }
        }
    }

    private fun loadStaffProfileForUser(user: FirebaseUser?) {
        staffProfileJob?.cancel()
        if (user == null) {
            _currentStaff.value = null
            _profileLoadError.value = null
            _isProfileLoading.value = false
            return
        }

        val uid = user.uid
        val email = user.email ?: ""
        val isDesignatedAdmin = email.equals("mop10015@gmail.com", ignoreCase = true) ||
                email.equals("snehal.guin@outlook.com", ignoreCase = true) ||
                uid == "b9hZz17DIZPXKbcoJPXwYyyENuD2"

        _isProfileLoading.value = true
        _profileLoadError.value = null

        staffProfileJob = viewModelScope.launch {
            // Real-time snapshot listener
            launch {
                authRepo.observeStaffMember(uid).collect { staff ->
                    if (staff != null) {
                        val wasAuthorized = staff.isAuthorized || isDesignatedAdmin
                        val wasAdmin = staff.isAdmin || isDesignatedAdmin
                        _currentStaff.value = staff.copy(
                            isAdmin = wasAdmin,
                            isAuthorized = wasAuthorized
                        )
                        _profileLoadError.value = null
                        _isProfileLoading.value = false
                    }
                }
            }

            // Direct fetch or auto-create in Firestore
            try {
                val staff = authRepo.getOrCreateStaffProfile(
                    uid = uid,
                    email = email,
                    displayName = user.displayName ?: email.substringBefore("@").ifBlank { "Staff" }
                )
                if (staff != null) {
                    val wasAuthorized = staff.isAuthorized || isDesignatedAdmin
                    val wasAdmin = staff.isAdmin || isDesignatedAdmin
                    _currentStaff.value = staff.copy(
                        isAdmin = wasAdmin,
                        isAuthorized = wasAuthorized
                    )
                    _profileLoadError.value = null
                    _isProfileLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("CadetTrackVM", "Failed to fetch staff profile: ${e.message}", e)
                if (_currentStaff.value == null) {
                    _profileLoadError.value = "Couldn't load your profile: ${e.localizedMessage ?: "Network or database connection error"}"
                    _isProfileLoading.value = false
                }
            }

            // Fallback timeout
            kotlinx.coroutines.delay(12000)
            if (_currentStaff.value == null && _profileLoadError.value == null) {
                _profileLoadError.value = "Couldn't load your profile. The server took too long to respond. Please check your network connection."
                _isProfileLoading.value = false
            }
        }
    }

    // --- Auth Actions ---
    fun clearAuthError() {
        _authError.value = null
    }

    fun cancelAuthLoading() {
        _isAuthLoading.value = false
        _authError.value = "Sign in / Sign up was cancelled."
    }

    fun signInWithEmail(email: String, pass: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepo.signInWithEmail(email, pass)
            _isAuthLoading.value = false
            result.onSuccess { staff ->
                _currentStaff.value = staff
                onSuccess()
            }.onFailure {
                _authError.value = it.localizedMessage ?: "Sign in failed"
            }
        }
    }

    fun signUpWithEmail(
        email: String,
        pass: String,
        displayName: String,
        rank: String,
        role: String,
        onSuccess: (StaffMember) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepo.signUpWithEmail(email, pass, displayName, rank, role)
            _isAuthLoading.value = false
            result.onSuccess { staff ->
                _currentStaff.value = staff
                onSuccess(staff)
            }.onFailure {
                _authError.value = it.localizedMessage ?: "Sign up failed"
            }
        }
    }

    fun updateAccountPassword(newPass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authRepo.updateAccountPassword(newPass)
            res.onSuccess {
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.localizedMessage ?: "Failed to update password")
            }
        }
    }

    fun updateOwnProfile(
        displayName: String,
        rank: String,
        role: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = _currentStaff.value?.uid ?: currentUser.value?.uid
        if (uid.isNullOrBlank()) {
            onResult(false, "No active user session")
            return
        }
        val previousStaff = _currentStaff.value
        val updatedStaff = (previousStaff ?: StaffMember(
            uid = uid,
            email = currentUser.value?.email ?: "",
            displayName = displayName,
            rank = rank,
            role = role,
            isAdmin = true,
            isAuthorized = true
        )).copy(
            displayName = displayName,
            rank = rank,
            role = role,
            isAuthorized = true
        )
        _currentStaff.value = updatedStaff

        viewModelScope.launch {
            try {
                firestoreRepo.updateOwnProfile(uid, displayName, rank, role)
            } catch (e: Exception) {
                Log.w("CadetTrackVM", "Firestore profile update notice: ${e.message}")
            }
            onResult(true, null)
        }
    }

    fun approveStaffRequest(staff: StaffMember, asAdmin: Boolean = false, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = firestoreRepo.updateStaffAuthorization(
                uid = staff.uid,
                isAuthorized = true,
                isAdmin = if (asAdmin) true else staff.isAdmin,
                authorizedSquadrons = staff.authorizedSquadrons.ifEmpty { listOf("1", "2") }
            )
            onResult(res.isSuccess)
        }
    }

    fun rejectStaffRequest(staff: StaffMember, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = firestoreRepo.deleteStaffMember(staff.uid)
            onResult(res.isSuccess)
        }
    }

    fun signInWithGoogle(context: Context, webClientId: String, onSuccess: (StaffMember) -> Unit = {}) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepo.signInWithGoogle(context, webClientId)
            _isAuthLoading.value = false
            result.onSuccess { staff ->
                _currentStaff.value = staff
                onSuccess(staff)
            }.onFailure {
                _authError.value = it.localizedMessage ?: "Google Sign-In failed"
            }
        }
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = authRepo.sendPasswordReset(email)
            res.onSuccess {
                onResult(true, "Password reset link sent to $email")
            }.onFailure {
                onResult(false, it.localizedMessage ?: "Failed to send password reset")
            }
        }
    }

    fun signOut() {
        authRepo.signOut()
        _currentStaff.value = null
    }

    // --- Setup Wizard / Org Settings ---
    fun saveOrgSettings(settings: OrgSettings, onComplete: (Boolean) -> Unit = {}) {
        _orgSettings.value = settings // Instant optimistic local update for zero UI lag
        _isOrgSettingsLoaded.value = true
        persistOrgSettings(settings)
        viewModelScope.launch {
            val res = firestoreRepo.saveOrgSettings(settings)
            onComplete(res.isSuccess)
        }
    }

    fun generateYearEvents(
        startDate: String,
        trainingDay: String,
        trainingTime: String,
        bandDay: String,
        bandTime: String,
        location: String,
        squadrons: List<String>,
        onComplete: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = firestoreRepo.generateYearEvents(
                startDateStr = startDate,
                trainingDay = trainingDay,
                trainingTime = trainingTime,
                bandDay = bandDay,
                bandTime = bandTime,
                location = location,
                squadrons = squadrons
            )
            onComplete(res.getOrDefault(0))
        }
    }

    // --- Cadets Management ---
    fun addCadet(cadet: Cadet, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val docId = if (cadet.id.isNotBlank()) cadet.id else java.util.UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val newCadet = cadet.copy(
                    id = docId,
                    createdAt = if (cadet.createdAt == 0L) now else cadet.createdAt,
                    updatedAt = now
                )

                // 1. Instantly update in-memory state & persist to local storage
                val current = _cadets.value.toMutableList()
                val idx = current.indexOfFirst { it.id == docId }
                if (idx >= 0) {
                    current[idx] = newCadet
                } else {
                    current.add(0, newCadet)
                }
                _cadets.value = current
                persistCadets(current)
                onResult(true, null)

                // 2. Synchronize with Firestore asynchronously
                firestoreRepo.addCadet(newCadet)
            } catch (e: Exception) {
                Log.e("CadetTrackVM", "addCadet error", e)
                onResult(true, null)
            }
        }
    }

    fun updateCadet(cadet: Cadet, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val updatedCadet = cadet.copy(updatedAt = now)

                val current = _cadets.value.toMutableList()
                val idx = current.indexOfFirst { it.id == cadet.id }
                if (idx >= 0) {
                    current[idx] = updatedCadet
                } else {
                    current.add(0, updatedCadet)
                }
                _cadets.value = current
                persistCadets(current)
                onResult(true, null)

                firestoreRepo.updateCadet(updatedCadet)
            } catch (e: Exception) {
                Log.e("CadetTrackVM", "updateCadet error", e)
                onResult(true, null)
            }
        }
    }

    fun archiveCadet(cadetId: String, newStatus: String = Cadet.STATUS_RELEASED, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val current = _cadets.value.toMutableList()
            val idx = current.indexOfFirst { it.id == cadetId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(status = newStatus, updatedAt = System.currentTimeMillis())
                _cadets.value = current
                persistCadets(current)
            }
            onResult(true)
            firestoreRepo.archiveCadet(cadetId, newStatus)
        }
    }

    fun deleteCadetPermanently(cadetId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val current = _cadets.value.filter { it.id != cadetId }
            _cadets.value = current
            persistCadets(current)
            onResult(true)
            firestoreRepo.deleteCadetPermanently(cadetId)
        }
    }

    fun bulkImportCsv(csvText: String, defaultSquadron: String = "1", onResult: (Int, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
                if (lines.isEmpty()) {
                    onResult(0, "CSV content is empty")
                    return@launch
                }

                // Detect delimiter: tab, comma, semicolon, pipe
                val firstLine = lines.first()
                val delimiter = when {
                    firstLine.contains('\t') -> "\t"
                    firstLine.contains(';') -> ";"
                    firstLine.contains('|') -> "|"
                    else -> ","
                }

                val headerLine = firstLine.lowercase()
                val isHeaderPresent = headerLine.contains("name") || headerLine.contains("rank") ||
                        headerLine.contains("squadron") || headerLine.contains("sqn") || headerLine.contains("first") || headerLine.contains("last")

                val headerCols = if (isHeaderPresent) {
                    firstLine.split(delimiter).map { it.trim().removeSurrounding("\"").lowercase() }
                } else emptyList()

                val startIndex = if (isHeaderPresent) 1 else 0

                // Map header column indices if available
                var idxLast = headerCols.indexOfFirst { it.contains("last") || it == "surname" }
                var idxFirst = headerCols.indexOfFirst { it.contains("first") || it == "given" || it == "forename" }
                var idxName = headerCols.indexOfFirst { it == "name" || it == "full name" || it == "cadet name" || it == "cadet" }
                val idxRank = headerCols.indexOfFirst { it.contains("rank") }
                val idxSqn = headerCols.indexOfFirst { it.contains("sqn") || it.contains("squadron") }
                val idxFlight = headerCols.indexOfFirst { it.contains("flt") || it.contains("flight") }
                val idxInst = headerCols.indexOfFirst { it.contains("inst") || it.contains("instrument") }
                val idxAppt = headerCols.indexOfFirst { it.contains("appt") || it.contains("appointment") || it.contains("role") }
                val idxPhone = headerCols.indexOfFirst { (it.contains("phone") || it.contains("tel")) && !it.contains("parent") && !it.contains("guardian") }
                val idxEmail = headerCols.indexOfFirst { it.contains("email") && !it.contains("parent") && !it.contains("guardian") }
                val idxParentName = headerCols.indexOfFirst { (it.contains("parent") || it.contains("guardian") || it.contains("emergency")) && (it.contains("name") || !it.contains("phone") && !it.contains("email")) }
                val idxParentPhone = headerCols.indexOfFirst { (it.contains("parent") || it.contains("guardian") || it.contains("emergency")) && (it.contains("phone") || it.contains("tel") || it.contains("cell")) }
                val idxParentEmail = headerCols.indexOfFirst { (it.contains("parent") || it.contains("guardian") || it.contains("emergency")) && it.contains("email") }

                val importedCadets = mutableListOf<Cadet>()

                for (i in startIndex until lines.size) {
                    val rawLine = lines[i]
                    val cols = rawLine.split(delimiter).map { it.trim().removeSurrounding("\"") }
                    if (cols.isEmpty() || cols.all { it.isBlank() }) continue

                    var firstName = ""
                    var lastName = ""
                    var rank = "Cdt"
                    var sqn = defaultSquadron
                    var flight = ""
                    var inst = ""
                    var appt = ""
                    var phone = ""
                    var email = ""
                    var parentName = ""
                    var parentPhone = ""
                    var parentEmail = ""

                    if (isHeaderPresent && (idxLast != -1 || idxFirst != -1 || idxName != -1)) {
                        if (idxLast != -1) lastName = cols.getOrElse(idxLast) { "" }
                        if (idxFirst != -1) firstName = cols.getOrElse(idxFirst) { "" }
                        if (lastName.isBlank() && firstName.isBlank() && idxName != -1) {
                            val rawName = cols.getOrElse(idxName) { "" }
                            val parsed = parseRawName(rawName)
                            firstName = parsed.first
                            lastName = parsed.second
                        }
                        if (idxRank != -1) rank = cols.getOrElse(idxRank) { "Cdt" }.ifBlank { "Cdt" }
                        if (idxSqn != -1) sqn = cols.getOrElse(idxSqn) { defaultSquadron }.ifBlank { defaultSquadron }
                        if (idxFlight != -1) flight = cols.getOrElse(idxFlight) { "" }
                        if (idxInst != -1) inst = cols.getOrElse(idxInst) { "" }
                        if (idxAppt != -1) appt = cols.getOrElse(idxAppt) { "" }
                        if (idxPhone != -1) phone = cols.getOrElse(idxPhone) { "" }
                        if (idxEmail != -1) email = cols.getOrElse(idxEmail) { "" }
                        if (idxParentName != -1) parentName = cols.getOrElse(idxParentName) { "" }
                        if (idxParentPhone != -1) parentPhone = cols.getOrElse(idxParentPhone) { "" }
                        if (idxParentEmail != -1) parentEmail = cols.getOrElse(idxParentEmail) { "" }
                    } else {
                        // Positional heuristic
                        if (cols.size == 1) {
                            val parsed = parseRawName(cols[0])
                            firstName = parsed.first
                            lastName = parsed.second
                        } else if (cols.size >= 2) {
                            // Check if first col is rank
                            val ranks = listOf("cdt", "lac", "cpl", "fcpl", "sgt", "fsgt", "wo2", "wo1", "cadet")
                            if (ranks.contains(cols[0].lowercase())) {
                                rank = cols[0]
                                val parsed = parseRawName(cols[1])
                                firstName = parsed.first
                                lastName = parsed.second
                                sqn = cols.getOrElse(2) { defaultSquadron }.ifBlank { defaultSquadron }
                                flight = cols.getOrElse(3) { "" }
                                inst = cols.getOrElse(4) { "" }
                                appt = cols.getOrElse(5) { "" }
                            } else {
                                lastName = cols.getOrElse(0) { "" }
                                firstName = cols.getOrElse(1) { "" }
                                rank = cols.getOrElse(2) { "Cdt" }.ifBlank { "Cdt" }
                                sqn = cols.getOrElse(3) { defaultSquadron }.ifBlank { defaultSquadron }
                                flight = cols.getOrElse(4) { "" }
                                inst = cols.getOrElse(5) { "" }
                                appt = cols.getOrElse(6) { "" }
                                phone = cols.getOrElse(7) { "" }
                                email = cols.getOrElse(8) { "" }
                                parentName = cols.getOrElse(9) { "" }
                                parentPhone = cols.getOrElse(10) { "" }
                                parentEmail = cols.getOrElse(11) { "" }
                            }
                        }
                    }

                    if (lastName.isNotBlank() || firstName.isNotBlank()) {
                        if (lastName.isBlank()) {
                            lastName = firstName
                            firstName = ""
                        }
                        importedCadets.add(
                            Cadet(
                                lastName = lastName.trim(),
                                firstName = firstName.trim(),
                                rank = rank.trim().ifBlank { "Cdt" },
                                squadron = sqn.trim().ifBlank { defaultSquadron },
                                flight = flight.trim(),
                                instrument = inst.trim(),
                                appointment = appt.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                parentName = parentName.trim(),
                                parentPhone = parentPhone.trim(),
                                parentEmail = parentEmail.trim(),
                                status = Cadet.STATUS_ACTIVE
                            )
                        )
                    }
                }

                if (importedCadets.isEmpty()) {
                    onResult(0, "No valid cadet rows found in pasted text. Please verify rows have at least a cadet name.")
                    return@launch
                }

                val current = _cadets.value.toMutableList()
                val now = System.currentTimeMillis()
                val prepared = importedCadets.map {
                    val id = if (it.id.isNotBlank()) it.id else java.util.UUID.randomUUID().toString()
                    it.copy(id = id, createdAt = now, updatedAt = now)
                }
                current.addAll(0, prepared)
                _cadets.value = current
                persistCadets(current)
                onResult(prepared.size, null)

                firestoreRepo.batchImportCadets(prepared)
            } catch (e: Exception) {
                Log.e("CadetTrackVM", "CSV import error", e)
                onResult(0, e.localizedMessage ?: "Failed to parse CSV")
            }
        }
    }

    private fun parseRawName(name: String): Pair<String, String> {
        val trimmed = name.trim().removeSurrounding("\"")
        if (trimmed.contains(",")) {
            val parts = trimmed.split(",", limit = 2)
            return Pair(parts.getOrNull(1)?.trim() ?: "", parts[0].trim())
        }
        val spaceIdx = trimmed.lastIndexOf(' ')
        return if (spaceIdx != -1) {
            Pair(trimmed.substring(0, spaceIdx).trim(), trimmed.substring(spaceIdx + 1).trim())
        } else {
            Pair("", trimmed)
        }
    }

    // --- Events Management ---
    fun selectEvent(event: BandEvent?) {
        _attendanceUiState.value = _attendanceUiState.value.copy(selectedEvent = event)
    }

    fun updateAttendanceSearch(query: String) {
        _attendanceUiState.value = _attendanceUiState.value.copy(searchQuery = query)
    }

    fun updateAttendanceFilterStatus(status: String) {
        _attendanceUiState.value = _attendanceUiState.value.copy(filterStatus = status)
    }

    fun updateAttendanceFilterSquadron(sqn: String) {
        _attendanceUiState.value = _attendanceUiState.value.copy(filterSquadron = sqn)
    }

    fun createEvent(event: BandEvent, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val docId = if (event.id.isNotBlank()) event.id else java.util.UUID.randomUUID().toString()
            val newEvent = event.copy(id = docId)
            val current = _events.value.toMutableList()
            current.add(0, newEvent)
            _events.value = current
            persistEvents(current)
            
            if (_attendanceUiState.value.selectedEvent == null) {
                _attendanceUiState.value = _attendanceUiState.value.copy(selectedEvent = newEvent)
            }
            onResult(true)
            firestoreRepo.createEvent(newEvent)
        }
    }

    fun updateEvent(event: BandEvent, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val current = _events.value.toMutableList()
            val idx = current.indexOfFirst { it.id == event.id }
            if (idx >= 0) {
                current[idx] = event
            } else {
                current.add(0, event)
            }
            _events.value = current
            persistEvents(current)
            if (_attendanceUiState.value.selectedEvent?.id == event.id) {
                _attendanceUiState.value = _attendanceUiState.value.copy(selectedEvent = event)
            }
            onResult(true)
            firestoreRepo.updateEvent(event)
        }
    }

    fun cancelEvent(eventId: String, reason: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val current = _events.value.toMutableList()
            val idx = current.indexOfFirst { it.id == eventId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(isCancelled = true, cancellationReason = reason)
                _events.value = current
                persistEvents(current)
            }
            onResult(true)
            firestoreRepo.cancelEvent(eventId, reason)
        }
    }

    fun uncancelEvent(eventId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val current = _events.value.toMutableList()
            val idx = current.indexOfFirst { it.id == eventId }
            if (idx >= 0) {
                current[idx] = current[idx].copy(isCancelled = false, cancellationReason = "")
                _events.value = current
                persistEvents(current)
            }
            onResult(true)
            firestoreRepo.uncancelEvent(eventId)
        }
    }

    // --- Attendance Operations ---
    fun markAttendance(
        cadetId: String,
        status: String,
        note: String = ""
    ) {
        val event = _attendanceUiState.value.selectedEvent ?: return
        val staffName = _currentStaff.value?.displayName ?: "Staff"
        val recordId = "${event.id}_${cadetId}"
        val record = AttendanceRecord(
            id = recordId,
            cadetId = cadetId,
            eventId = event.id,
            status = status,
            note = note,
            timestamp = System.currentTimeMillis(),
            markedBy = staffName
        )

        val current = _allAttendanceRecords.value.toMutableList()
        val idx = current.indexOfFirst { it.id == recordId || (it.eventId == event.id && it.cadetId == cadetId) }
        if (idx >= 0) {
            current[idx] = record
        } else {
            current.add(record)
        }
        _allAttendanceRecords.value = current
        persistAttendance(current)

        viewModelScope.launch {
            firestoreRepo.saveAttendanceRecord(record)
        }
    }

    fun markAllPresent(activeCadets: List<Cadet>, currentRecords: List<AttendanceRecord>, onDone: () -> Unit = {}) {
        val event = _attendanceUiState.value.selectedEvent ?: return
        val staffName = _currentStaff.value?.displayName ?: "Staff"
        val now = System.currentTimeMillis()
        
        // Save snapshot for Undo
        _undoSnapshot.value = currentRecords

        val current = _allAttendanceRecords.value.toMutableList()
        for (cadet in activeCadets) {
            val recordId = "${event.id}_${cadet.id}"
            val existingIdx = current.indexOfFirst { it.id == recordId || (it.eventId == event.id && it.cadetId == cadet.id) }
            val updatedRecord = AttendanceRecord(
                id = recordId,
                cadetId = cadet.id,
                eventId = event.id,
                status = AttendanceRecord.STATUS_PRESENT,
                note = "",
                timestamp = now,
                markedBy = staffName
            )
            if (existingIdx >= 0) {
                current[existingIdx] = updatedRecord
            } else {
                current.add(updatedRecord)
            }
        }
        _allAttendanceRecords.value = current
        persistAttendance(current)
        onDone()

        viewModelScope.launch {
            firestoreRepo.batchMarkAllPresent(
                eventId = event.id,
                activeCadetIds = activeCadets.map { it.id },
                markedBy = staffName
            )
        }
    }

    fun undoMarkAllPresent() {
        val snapshot = _undoSnapshot.value ?: return
        val current = _allAttendanceRecords.value.toMutableList()
        for (rec in snapshot) {
            val idx = current.indexOfFirst { it.id == rec.id || (it.eventId == rec.eventId && it.cadetId == rec.cadetId) }
            if (idx >= 0) {
                current[idx] = rec
            }
        }
        _allAttendanceRecords.value = current
        persistAttendance(current)
        _undoSnapshot.value = null

        viewModelScope.launch {
            firestoreRepo.batchRevertAttendance(snapshot)
        }
    }

    fun clearUndoSnapshot() {
        _undoSnapshot.value = null
    }

    // --- Reports & Calculations ---
    fun getCadetStats(cadet: Cadet): CadetAttendanceStats {
        val allEventsList = events.value.filter { !it.isCancelled }
        val cadetRecords = allAttendanceRecords.value.filter { it.cadetId == cadet.id }
        val recordsByEvent = cadetRecords.associateBy { it.eventId }

        var attended = 0
        var absent = 0
        var absentNotified = 0
        var excused = 0
        val history = mutableListOf<Pair<BandEvent, AttendanceRecord>>()

        for (event in allEventsList) {
            val rec = recordsByEvent[event.id]
            if (rec != null) {
                history.add(Pair(event, rec))
                when (rec.status) {
                    AttendanceRecord.STATUS_PRESENT, AttendanceRecord.STATUS_LATE -> attended++
                    AttendanceRecord.STATUS_ABSENT -> absent++
                    AttendanceRecord.STATUS_ABSENT_NOTIFIED -> absentNotified++
                    AttendanceRecord.STATUS_EXCUSED -> excused++
                }
            }
        }

        val totalMarked = attended + absent + absentNotified
        val pct = if (totalMarked > 0) (attended.toFloat() / totalMarked) * 100f else 100f
        val threshold = orgSettings.value.attendanceThreshold

        return CadetAttendanceStats(
            cadet = cadet,
            totalEvents = allEventsList.size,
            attendedCount = attended,
            excusedCount = excused,
            absentCount = absent,
            absentNotifiedCount = absentNotified,
            percentage = pct,
            isBelowThreshold = totalMarked > 0 && pct < threshold,
            records = history.sortedByDescending { it.first.date }
        )
    }

    fun getSquadronReports(): List<SquadronReport> {
        val sqnList = orgSettings.value.squadronNumbers.ifEmpty { listOf("1", "2") }
        val activeCadets = cadets.value.filter { it.status == Cadet.STATUS_ACTIVE }
        val totalEvents = events.value.count { !it.isCancelled }

        return sqnList.map { sqn ->
            val sqnCadets = activeCadets.filter { it.squadron == sqn }
            val statsList = sqnCadets.map { getCadetStats(it) }
            val avg = if (statsList.isNotEmpty()) statsList.map { it.percentage }.average().toFloat() else 0f
            val flagged = statsList.count { it.isBelowThreshold }

            SquadronReport(
                squadron = sqn,
                activeCadetsCount = sqnCadets.size,
                averageAttendanceRate = avg,
                totalEventsHeld = totalEvents,
                flaggedCadetsCount = flagged
            )
        }
    }

    // --- PDF & Sheets Export Operations ---
    fun generateOrUpdatePdf(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isPdfGenerating.value = true
            _syncMessage.value = "Updating Attendance PDF in-place..."
            val res = pdfReportService.generateOrUpdatePdf(
                settings = orgSettings.value,
                cadets = cadets.value,
                events = events.value,
                records = allAttendanceRecords.value
            )
            _isPdfGenerating.value = false
            res.onSuccess { file ->
                _lastPdfUpdated.value = file.lastModified()
                _syncMessage.value = "Attendance PDF updated"
                onResult(true, null)
            }.onFailure { err ->
                _syncMessage.value = "PDF update failed: ${err.localizedMessage}"
                onResult(false, err.localizedMessage ?: "Failed to generate PDF")
            }
        }
    }

    fun openCanonicalPdf(context: Context, onOpenFailed: (String) -> Unit = {}) {
        val intent = pdfReportService.createViewPdfIntent()
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                shareCanonicalPdf(context, onOpenFailed)
            }
        } else {
            generateOrUpdatePdf { success, err ->
                if (success) openCanonicalPdf(context, onOpenFailed)
                else onOpenFailed(err ?: "Could not open PDF")
            }
        }
    }

    fun shareCanonicalPdf(context: Context, onShareFailed: (String) -> Unit = {}) {
        val intent = pdfReportService.createSharePdfIntent()
        if (intent != null) {
            val chooser = Intent.createChooser(intent, "Share Cadet Attendance PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(chooser)
            } catch (e: Exception) {
                onShareFailed(e.localizedMessage ?: "Unable to share PDF")
            }
        } else {
            generateOrUpdatePdf { success, err ->
                if (success) shareCanonicalPdf(context, onShareFailed)
                else onShareFailed(err ?: "Could not share PDF")
            }
        }
    }

    fun exportToCsv(squadron: String?) {
        viewModelScope.launch {
            _syncMessage.value = "Generating CSV..."
            val file = sheetsSyncService.generateAttendanceCsv(
                squadron = squadron,
                cadets = cadets.value,
                events = events.value,
                records = allAttendanceRecords.value
            )
            _syncMessage.value = "CSV ready"
            sheetsSyncService.shareFile(file, "text/csv", "Cadet Band Attendance CSV")
        }
    }

    fun downloadExcelCopy(spreadsheetId: String?, authToken: String? = null) {
        val effectiveToken = authToken ?: _googleOAuthToken.value
        viewModelScope.launch {
            _isSheetsSyncing.value = true
            _syncMessage.value = "Exporting Excel (.xlsx) from Google Sheets..."
            if (spreadsheetId.isNullOrBlank()) {
                // Generate CSV export as fallback if sheet not created yet
                val csvFile = sheetsSyncService.generateAttendanceCsv(
                    squadron = null,
                    cadets = cadets.value,
                    events = events.value,
                    records = allAttendanceRecords.value
                )
                _isSheetsSyncing.value = false
                _syncMessage.value = "Export completed"
                sheetsSyncService.shareFile(csvFile, "text/csv", "Cadet Band Attendance")
                return@launch
            }

            val res = sheetsSyncService.downloadExcelFromGoogleSheet(spreadsheetId, effectiveToken)
            _isSheetsSyncing.value = false
            res.onSuccess { file ->
                _syncMessage.value = "Excel file ready"
                sheetsSyncService.shareFile(
                    file,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "Cadet Band Attendance"
                )
            }.onFailure {
                _syncMessage.value = "Excel export failed: ${it.localizedMessage}"
            }
        }
    }

    fun syncWithGoogleSheets(oauthToken: String? = null) {
        val effectiveToken = oauthToken ?: _googleOAuthToken.value ?: ""
        viewModelScope.launch {
            _isSheetsSyncing.value = true
            _syncMessage.value = "Syncing live attendance to Google Sheets..."
            // Always read canonical orgSettings from Firestore to guarantee shared ID across devices
            val currentSettings = firestoreRepo.getOrgSettings() ?: orgSettings.value
            val canonicalSheetId = currentSettings.googleSheetId.trim().ifBlank { null }

            val res = sheetsSyncService.syncToGoogleSheets(
                oauthToken = effectiveToken,
                existingSheetId = canonicalSheetId,
                settings = currentSettings,
                cadets = cadets.value,
                events = events.value,
                records = allAttendanceRecords.value
            )
            _isSheetsSyncing.value = false
            res.onSuccess { sheetId ->
                val sheetUrl = "https://docs.google.com/spreadsheets/d/$sheetId"
                val updated = currentSettings.copy(
                    googleSheetId = sheetId,
                    googleSheetUrl = sheetUrl,
                    googleSheetLastSynced = System.currentTimeMillis()
                )
                firestoreRepo.saveOrgSettings(updated)
                _syncMessage.value = "Google Sheets synced successfully!"
            }.onFailure {
                _syncMessage.value = "Sync failed: ${it.localizedMessage}"
            }
        }
    }

    fun updateStaffSyncAccess(
        staff: StaffMember,
        grantAccess: Boolean,
        adminToken: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        val effectiveToken = adminToken ?: _googleOAuthToken.value
        viewModelScope.launch {
            // 1. Update Firestore permission flag
            val firestoreRes = firestoreRepo.updateStaffSyncAccess(staff.uid, grantAccess)
            if (firestoreRes.isFailure) {
                onResult(false, "Failed to update database permissions: ${firestoreRes.exceptionOrNull()?.localizedMessage}")
                return@launch
            }

            // 2. Automatically share or revoke Editor permission on canonical sheet via Google Drive API
            val currentSettings = firestoreRepo.getOrgSettings() ?: orgSettings.value
            val sheetId = currentSettings.googleSheetId.trim()

            if (sheetId.isNotBlank() && !effectiveToken.isNullOrBlank() && staff.email.isNotBlank()) {
                if (grantAccess) {
                    val driveRes = sheetsSyncService.shareSpreadsheetWithEmail(effectiveToken, sheetId, staff.email)
                    driveRes.onSuccess {
                        onResult(true, "Sync access granted and Google Sheet shared with ${staff.email}")
                    }.onFailure { driveErr ->
                        onResult(true, "Sync access flag enabled. Note: Google Drive sharing returned: ${driveErr.localizedMessage}")
                    }
                } else {
                    val driveRes = sheetsSyncService.revokeSpreadsheetAccess(effectiveToken, sheetId, staff.email)
                    driveRes.onSuccess {
                        onResult(true, "Sync access revoked and Google Sheet unshared for ${staff.email}")
                    }.onFailure { driveErr ->
                        onResult(true, "Sync access disabled. Note: Google Drive unsharing returned: ${driveErr.localizedMessage}")
                    }
                }
            } else if (sheetId.isBlank()) {
                onResult(true, "Sync access ${if (grantAccess) "enabled" else "disabled"}. (No canonical spreadsheet configured yet).")
            } else {
                onResult(true, "Sync access ${if (grantAccess) "enabled" else "disabled"} in database. (Sign in with Google to sync Drive file sharing).")
            }
        }
    }

    fun startNewTrainingYear(oauthToken: String? = null, newYearLabel: String, onDone: (Boolean) -> Unit = {}) {
        val effectiveToken = oauthToken ?: _googleOAuthToken.value ?: ""
        viewModelScope.launch {
            _isSheetsSyncing.value = true
            val currentSettings = orgSettings.value
            val activeCadets = cadets.value.filter { it.status == Cadet.STATUS_ACTIVE }

            val res = sheetsSyncService.startNewTrainingYear(
                oauthToken = effectiveToken,
                oldSheetId = currentSettings.googleSheetId.ifBlank { null },
                newYearLabel = newYearLabel,
                settings = currentSettings,
                activeCadets = activeCadets,
                newYearEvents = emptyList()
            )
            _isSheetsSyncing.value = false
            res.onSuccess { newSheetId ->
                val updated = currentSettings.copy(
                    trainingYear = newYearLabel,
                    googleSheetId = newSheetId,
                    googleSheetUrl = "https://docs.google.com/spreadsheets/d/$newSheetId",
                    googleSheetLastSynced = System.currentTimeMillis()
                )
                firestoreRepo.saveOrgSettings(updated)
                _syncMessage.value = "New training year '$newYearLabel' initialized."
                onDone(true)
            }.onFailure {
                _syncMessage.value = "Failed to initialize new year: ${it.localizedMessage}"
                onDone(false)
            }
        }
    }

    // --- Gemini Chat & Multi-Session Chat System ---
    fun createNewChatSession(title: String = "New Conversation") {
        val newSessionId = java.util.UUID.randomUUID().toString()
        val newSession = ChatSession(
            id = newSessionId,
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            messages = listOf(defaultInitialMessage)
        )
        val updated = listOf(newSession) + _chatSessions.value
        _chatSessions.value = updated
        _currentSessionId.value = newSessionId
        _chatMessages.value = newSession.messages
        persistChatSessions(updated)
    }

    fun selectChatSession(sessionId: String) {
        val target = _chatSessions.value.find { it.id == sessionId } ?: return
        _currentSessionId.value = target.id
        _chatMessages.value = target.messages
    }

    fun deleteChatSession(sessionId: String) {
        val currentList = _chatSessions.value.filterNot { it.id == sessionId }
        val finalSessions = if (currentList.isEmpty()) {
            val fresh = ChatSession(
                id = java.util.UUID.randomUUID().toString(),
                title = "Attendance Assistant",
                messages = listOf(defaultInitialMessage)
            )
            listOf(fresh)
        } else {
            currentList
        }
        _chatSessions.value = finalSessions
        if (_currentSessionId.value == sessionId || finalSessions.none { it.id == _currentSessionId.value }) {
            val next = finalSessions.first()
            _currentSessionId.value = next.id
            _chatMessages.value = next.messages
        }
        persistChatSessions(finalSessions)
    }

    fun sendGeminiMessage(prompt: String, formatOption: String = "smart") {
        if (prompt.isBlank()) return
        val currentId = _currentSessionId.value
        val userMsg = ChatMessage(sender = "user", text = prompt)
        
        // Find or create current session
        val currentSessions = _chatSessions.value.toMutableList()
        var sessionIndex = currentSessions.indexOfFirst { it.id == currentId }
        val session = if (sessionIndex >= 0) {
            currentSessions[sessionIndex]
        } else {
            val fresh = ChatSession(id = currentId, title = prompt.take(32).trim(), messages = listOf(defaultInitialMessage))
            currentSessions.add(0, fresh)
            sessionIndex = 0
            fresh
        }

        // Auto title conversation from first prompt if still default
        val newTitle = if (session.title == "New Conversation" || session.title == "Attendance Assistant") {
            if (prompt.length > 32) prompt.take(30).trim() + "..." else prompt.trim()
        } else {
            session.title
        }

        val updatedMessagesWithUser = session.messages + userMsg
        val updatedSession = session.copy(
            title = newTitle,
            updatedAt = System.currentTimeMillis(),
            messages = updatedMessagesWithUser
        )
        currentSessions[sessionIndex] = updatedSession
        _chatSessions.value = currentSessions
        _chatMessages.value = updatedMessagesWithUser
        persistChatSessions(currentSessions)

        val staff = _currentStaff.value ?: StaffMember(displayName = "Staff Member", role = "Instructor")

        viewModelScope.launch {
            _isChatLoading.value = true
            val geminiMsg = geminiService.generateGroundedResponse(
                userPrompt = prompt,
                history = updatedMessagesWithUser,
                settings = orgSettings.value,
                staff = staff,
                cadets = cadets.value,
                events = events.value,
                records = allAttendanceRecords.value,
                responseFormat = formatOption
            )
            _isChatLoading.value = false

            val latestSessions = _chatSessions.value.toMutableList()
            val latestIdx = latestSessions.indexOfFirst { it.id == currentId }
            if (latestIdx >= 0) {
                val latestSession = latestSessions[latestIdx]
                val fullMessages = latestSession.messages + geminiMsg
                val finishedSession = latestSession.copy(
                    updatedAt = System.currentTimeMillis(),
                    messages = fullMessages
                )
                latestSessions[latestIdx] = finishedSession
                _chatSessions.value = latestSessions
                if (_currentSessionId.value == currentId) {
                    _chatMessages.value = fullMessages
                }
                persistChatSessions(latestSessions)
            }
        }
    }

    fun clearChatMessages() {
        val currentId = _currentSessionId.value
        val updated = _chatSessions.value.map { session ->
            if (session.id == currentId) {
                session.copy(
                    updatedAt = System.currentTimeMillis(),
                    messages = listOf(defaultInitialMessage)
                )
            } else session
        }
        _chatSessions.value = updated
        _chatMessages.value = listOf(defaultInitialMessage)
        persistChatSessions(updated)
    }

    private fun loadPersistedChatSessions() {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("cadet_chat_sessions", Context.MODE_PRIVATE)
            val json = prefs.getString("sessions_json", null)
            if (!json.isNullOrBlank()) {
                val arr = JSONArray(json)
                val list = mutableListOf<ChatSession>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                    val title = obj.optString("title", "Conversation")
                    val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    val msgsArr = obj.optJSONArray("messages") ?: JSONArray()
                    val msgs = mutableListOf<ChatMessage>()
                    for (m in 0 until msgsArr.length()) {
                        val mObj = msgsArr.getJSONObject(m)
                        msgs.add(
                            ChatMessage(
                                id = mObj.optString("id", java.util.UUID.randomUUID().toString()),
                                sender = mObj.optString("sender", "gemini"),
                                text = mObj.optString("text", ""),
                                timestamp = mObj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    list.add(
                        ChatSession(
                            id = id,
                            title = title,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            messages = if (msgs.isNotEmpty()) msgs else listOf(defaultInitialMessage)
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    _chatSessions.value = list
                    val first = list.first()
                    _currentSessionId.value = first.id
                    _chatMessages.value = first.messages
                }
            }
        } catch (e: Exception) {
            Log.w("CadetTrackVM", "Error loading persisted chat sessions: ${e.message}")
        }
    }

    private fun persistChatSessions(sessions: List<ChatSession>) {
        viewModelScope.launch {
            try {
                val prefs = getApplication<Application>().getSharedPreferences("cadet_chat_sessions", Context.MODE_PRIVATE)
                val arr = JSONArray()
                // Store top 25 sessions
                sessions.take(25).forEach { session ->
                    val obj = JSONObject()
                    obj.put("id", session.id)
                    obj.put("title", session.title)
                    obj.put("createdAt", session.createdAt)
                    obj.put("updatedAt", session.updatedAt)
                    val msgsArr = JSONArray()
                    session.messages.takeLast(60).forEach { msg ->
                        val mObj = JSONObject()
                        mObj.put("id", msg.id)
                        mObj.put("sender", msg.sender)
                        mObj.put("text", msg.text)
                        mObj.put("timestamp", msg.timestamp)
                        msgsArr.put(mObj)
                    }
                    obj.put("messages", msgsArr)
                    arr.put(obj)
                }
                prefs.edit().putString("sessions_json", arr.toString()).apply()
            } catch (e: Exception) {
                Log.w("CadetTrackVM", "Error saving chat sessions: ${e.message}")
            }
        }
    }

    fun saveChatLogToDevice(context: Context, targetSessionId: String? = null): String {
        val session = if (targetSessionId != null) {
            _chatSessions.value.find { it.id == targetSessionId }
        } else {
            _chatSessions.value.find { it.id == _currentSessionId.value }
        }
        val msgs = session?.messages ?: _chatMessages.value
        val sessionTitle = session?.title ?: "Attendance Insights"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val headerDate = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date())
        val orgName = _orgSettings.value.orgName.ifBlank { "CadetTrack" }
        val staffName = _currentStaff.value?.let { "${it.rank} ${it.displayName}".trim() } ?: "Staff"

        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine(" CADETTRACK AI CHAT TRANSCRIPT")
        sb.appendLine(" Conversation: $sessionTitle")
        sb.appendLine(" Organization: $orgName")
        sb.appendLine(" Generated by: $staffName")
        sb.appendLine(" Date & Time:  $headerDate")
        sb.appendLine(" Total Messages: ${msgs.size}")
        sb.appendLine("==================================================")
        sb.appendLine()

        msgs.forEachIndexed { index, msg ->
            val timeStr = sdf.format(Date(msg.timestamp))
            val senderLabel = if (msg.sender == "user") "[$staffName (User)]" else "[CadetTrack Gemini AI]"
            sb.appendLine("--------------------------------------------------")
            sb.appendLine("$timeStr  $senderLabel")
            sb.appendLine("--------------------------------------------------")
            sb.appendLine(msg.text.trim())
            sb.appendLine()
        }

        val transcript = sb.toString()

        // Copy to system clipboard
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("CadetTrack AI Chat: $sessionTitle", transcript)
            clipboard?.setPrimaryClip(clip)
        } catch (e: Exception) {
            Log.w("CadetTrackVM", "Clipboard copy error: ${e.message}")
        }

        // Launch share intent
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "CadetTrack Chat - $sessionTitle ($headerDate)")
                putExtra(Intent.EXTRA_TEXT, transcript)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Chat Transcript").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.w("CadetTrackVM", "Share intent error: ${e.message}")
        }

        return transcript
    }

    // --- Org Settings Local Persistence ---
    private fun loadPersistedOrgSettings(): OrgSettings {
        return try {
            val prefs = getApplication<Application>().getSharedPreferences("cadet_org_settings", Context.MODE_PRIVATE)
            val orgName = prefs.getString("org_name", "") ?: ""
            if (orgName.isNotBlank()) {
                val squadronsSet = prefs.getStringSet("squadrons", setOf("1", "2")) ?: setOf("1", "2")
                val location = prefs.getString("home_location", "") ?: ""
                val trainingDay = prefs.getString("training_day", "Tuesday") ?: "Tuesday"
                val trainingTime = prefs.getString("training_time", "18:30 - 21:00") ?: "18:30 - 21:00"
                val bandDay = prefs.getString("band_day", "Saturday") ?: "Saturday"
                val bandTime = prefs.getString("band_time", "09:00 - 12:00") ?: "09:00 - 12:00"
                val threshold = prefs.getInt("threshold", 75)
                val trainingYear = prefs.getString("training_year", "2026-2027") ?: "2026-2027"
                val sheetId = prefs.getString("sheet_id", "") ?: ""
                val sheetUrl = prefs.getString("sheet_url", "") ?: ""
                val sheetSynced = prefs.getLong("sheet_synced", 0L)

                OrgSettings(
                    orgName = orgName,
                    squadronNumbers = squadronsSet.toList().sorted(),
                    homeLocation = location,
                    defaultTrainingNightDay = trainingDay,
                    defaultTrainingNightTime = trainingTime,
                    defaultBandPracticeDay = bandDay,
                    defaultBandPracticeTime = bandTime,
                    attendanceThreshold = threshold,
                    trainingYear = trainingYear,
                    googleSheetId = sheetId,
                    googleSheetUrl = sheetUrl,
                    googleSheetLastSynced = sheetSynced
                )
            } else {
                OrgSettings()
            }
        } catch (e: Exception) {
            Log.w("CadetTrackVM", "Error reading cached org settings: ${e.message}")
            OrgSettings()
        }
    }

    private fun persistOrgSettings(settings: OrgSettings) {
        viewModelScope.launch {
            try {
                val prefs = getApplication<Application>().getSharedPreferences("cadet_org_settings", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("org_name", settings.orgName)
                    .putStringSet("squadrons", settings.squadronNumbers.toSet())
                    .putString("home_location", settings.homeLocation)
                    .putString("training_day", settings.defaultTrainingNightDay)
                    .putString("training_time", settings.defaultTrainingNightTime)
                    .putString("band_day", settings.defaultBandPracticeDay)
                    .putString("band_time", settings.defaultBandPracticeTime)
                    .putInt("threshold", settings.attendanceThreshold)
                    .putString("training_year", settings.trainingYear)
                    .putString("sheet_id", settings.googleSheetId)
                    .putString("sheet_url", settings.googleSheetUrl)
                    .putLong("sheet_synced", settings.googleSheetLastSynced)
                    .apply()
            } catch (e: Exception) {
                Log.w("CadetTrackVM", "Error caching org settings: ${e.message}")
            }
        }
    }

    // --- Staff Management ---
    fun updateStaffAuthorization(
        targetStaff: StaffMember,
        isAuthorized: Boolean,
        isAdmin: Boolean,
        squadrons: List<String>,
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val updated = targetStaff.copy(
                isAuthorized = isAuthorized,
                isAdmin = isAdmin,
                authorizedSquadrons = squadrons
            )
            val res = firestoreRepo.updateStaffMember(updated)
            onDone(res.isSuccess)
        }
    }

    // --- Cadets, Events, Attendance Local Cache & Persistence ---
    private fun loadPersistedCadets(): List<Cadet> {
        return try {
            val prefs = getApplication<Application>().getSharedPreferences("cadet_data_cache", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("cached_cadets", null) ?: return emptyList()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<Cadet>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Cadet(
                        id = obj.optString("id", ""),
                        lastName = obj.optString("lastName", ""),
                        firstName = obj.optString("firstName", ""),
                        rank = obj.optString("rank", "Cdt"),
                        phone = obj.optString("phone", ""),
                        email = obj.optString("email", ""),
                        squadron = obj.optString("squadron", "1"),
                        flight = obj.optString("flight", ""),
                        appointment = obj.optString("appointment", ""),
                        instrument = obj.optString("instrument", ""),
                        status = obj.optString("status", Cadet.STATUS_ACTIVE),
                        parentName = obj.optString("parentName", ""),
                        parentPhone = obj.optString("parentPhone", ""),
                        parentEmail = obj.optString("parentEmail", ""),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w("CadetTrackVM", "Error loading persisted cadets: ${e.message}")
            emptyList()
        }
    }

    private fun persistCadets(list: List<Cadet>) {
        viewModelScope.launch {
            try {
                val prefs = getApplication<Application>().getSharedPreferences("cadet_data_cache", Context.MODE_PRIVATE)
                val array = JSONArray()
                for (c in list) {
                    val obj = JSONObject()
                    obj.put("id", c.id)
                    obj.put("lastName", c.lastName)
                    obj.put("firstName", c.firstName)
                    obj.put("rank", c.rank)
                    obj.put("phone", c.phone)
                    obj.put("email", c.email)
                    obj.put("squadron", c.squadron)
                    obj.put("flight", c.flight)
                    obj.put("appointment", c.appointment)
                    obj.put("instrument", c.instrument)
                    obj.put("status", c.status)
                    obj.put("parentName", c.parentName)
                    obj.put("parentPhone", c.parentPhone)
                    obj.put("parentEmail", c.parentEmail)
                    obj.put("notes", c.notes)
                    obj.put("createdAt", c.createdAt)
                    obj.put("updatedAt", c.updatedAt)
                    array.put(obj)
                }
                prefs.edit().putString("cached_cadets", array.toString()).apply()
            } catch (e: Exception) {
                Log.w("CadetTrackVM", "Error caching cadets: ${e.message}")
            }
        }
    }

    private fun loadPersistedEvents(): List<BandEvent> {
        return try {
            val prefs = getApplication<Application>().getSharedPreferences("cadet_data_cache", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("cached_events", null) ?: return emptyList()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<BandEvent>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    BandEvent(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", ""),
                        date = obj.optString("date", ""),
                        startTime = obj.optString("startTime", "18:30"),
                        endTime = obj.optString("endTime", "21:00"),
                        squadron = obj.optString("squadron", "All"),
                        type = obj.optString("type", BandEvent.TYPE_TRAINING_NIGHT),
                        location = obj.optString("location", ""),
                        isCancelled = obj.optBoolean("isCancelled", false),
                        cancellationReason = obj.optString("cancellationReason", ""),
                        notes = obj.optString("notes", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w("CadetTrackVM", "Error loading persisted events: ${e.message}")
            emptyList()
        }
    }

    private fun persistEvents(list: List<BandEvent>) {
        viewModelScope.launch {
            try {
                val prefs = getApplication<Application>().getSharedPreferences("cadet_data_cache", Context.MODE_PRIVATE)
                val array = JSONArray()
                for (ev in list) {
                    val obj = JSONObject()
                    obj.put("id", ev.id)
                    obj.put("title", ev.title)
                    obj.put("date", ev.date)
                    obj.put("startTime", ev.startTime)
                    obj.put("endTime", ev.endTime)
                    obj.put("squadron", ev.squadron)
                    obj.put("type", ev.type)
                    obj.put("location", ev.location)
                    obj.put("isCancelled", ev.isCancelled)
                    obj.put("cancellationReason", ev.cancellationReason)
                    obj.put("notes", ev.notes)
                    array.put(obj)
                }
                prefs.edit().putString("cached_events", array.toString()).apply()
            } catch (e: Exception) {
                Log.w("CadetTrackVM", "Error caching events: ${e.message}")
            }
        }
    }

    private fun loadPersistedAttendance(): List<AttendanceRecord> {
        return try {
            val prefs = getApplication<Application>().getSharedPreferences("cadet_data_cache", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("cached_attendance", null) ?: return emptyList()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<AttendanceRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AttendanceRecord(
                        id = obj.optString("id", ""),
                        cadetId = obj.optString("cadetId", ""),
                        eventId = obj.optString("eventId", ""),
                        status = obj.optString("status", AttendanceRecord.STATUS_UNMARKED),
                        note = obj.optString("note", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        markedBy = obj.optString("markedBy", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w("CadetTrackVM", "Error loading persisted attendance: ${e.message}")
            emptyList()
        }
    }

    private fun persistAttendance(list: List<AttendanceRecord>) {
        viewModelScope.launch {
            try {
                val prefs = getApplication<Application>().getSharedPreferences("cadet_data_cache", Context.MODE_PRIVATE)
                val array = JSONArray()
                for (rec in list) {
                    val obj = JSONObject()
                    obj.put("id", rec.id)
                    obj.put("cadetId", rec.cadetId)
                    obj.put("eventId", rec.eventId)
                    obj.put("status", rec.status)
                    obj.put("note", rec.note)
                    obj.put("timestamp", rec.timestamp)
                    obj.put("markedBy", rec.markedBy)
                    array.put(obj)
                }
                prefs.edit().putString("cached_attendance", array.toString()).apply()
            } catch (e: Exception) {
                Log.w("CadetTrackVM", "Error caching attendance: ${e.message}")
            }
        }
    }
}

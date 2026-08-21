package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.FirestoreRepository
import com.example.data.GeminiService
import com.example.data.GoogleSheetsSyncService
import com.example.data.ThemeMode
import com.example.data.UserPreferencesRepository
import com.example.model.AttendanceRecord
import com.example.model.BandEvent
import com.example.model.Cadet
import com.example.model.CadetAttendanceStats
import com.example.model.ChatMessage
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
import java.io.File

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
    private val geminiService = GeminiService()
    private val userPreferencesRepo = UserPreferencesRepository(application)

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

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // --- Core Data Flows ---
    val orgSettings: StateFlow<OrgSettings> = firestoreRepo.observeOrgSettings()
        .map { it ?: OrgSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OrgSettings())

    val staffMembers: StateFlow<List<StaffMember>> = firestoreRepo.observeAllStaff()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cadets: StateFlow<List<Cadet>> = firestoreRepo.observeCadets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val events: StateFlow<List<BandEvent>> = firestoreRepo.observeEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendanceRecords: StateFlow<List<AttendanceRecord>> = firestoreRepo.observeAllAttendance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Attendance Screen State ---
    private val _attendanceUiState = MutableStateFlow(AttendanceUiState())
    val attendanceUiState: StateFlow<AttendanceUiState> = _attendanceUiState.asStateFlow()

    private val _undoSnapshot = MutableStateFlow<List<AttendanceRecord>?>(null)
    val undoSnapshot: StateFlow<List<AttendanceRecord>?> = _undoSnapshot.asStateFlow()

    // --- Reports Screen State ---
    private val _reportsUiState = MutableStateFlow(ReportsUiState())
    val reportsUiState: StateFlow<ReportsUiState> = _reportsUiState.asStateFlow()

    // --- Sheets Sync State ---
    private val _isSheetsSyncing = MutableStateFlow(false)
    val isSheetsSyncing: StateFlow<Boolean> = _isSheetsSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    // --- Gemini Chat State ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "gemini",
                text = "CadetTrack Intelligence ready. Ask any question regarding cadet attendance, squadron comparisons, or generate guardian notification drafts."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    init {
        checkFirebaseConfig()
        observeCurrentUserStaffProfile()
    }

    private fun checkFirebaseConfig() {
        try {
            val apps = FirebaseApp.getApps(getApplication())
            _isFirebaseConfigured.value = apps.isNotEmpty()
        } catch (e: Exception) {
            _isFirebaseConfigured.value = false
        }
    }

    private fun observeCurrentUserStaffProfile() {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    authRepo.observeStaffMember(user.uid).collect { staff ->
                        _currentStaff.value = staff
                    }
                } else {
                    _currentStaff.value = null
                }
            }
        }
    }

    // --- Auth Actions ---
    fun clearAuthError() {
        _authError.value = null
    }

    fun signInWithEmail(email: String, pass: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepo.signInWithEmail(email, pass)
            _isAuthLoading.value = false
            result.onSuccess {
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
        role: String,
        onSuccess: (StaffMember) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepo.signUpWithEmail(email, pass, displayName, role)
            _isAuthLoading.value = false
            result.onSuccess { staff ->
                _currentStaff.value = staff
                onSuccess(staff)
            }.onFailure {
                _authError.value = it.localizedMessage ?: "Sign up failed"
            }
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
    fun addCadet(cadet: Cadet, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = firestoreRepo.addCadet(cadet)
            onResult(res.isSuccess)
        }
    }

    fun updateCadet(cadet: Cadet, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = firestoreRepo.updateCadet(cadet)
            onResult(res.isSuccess)
        }
    }

    fun archiveCadet(cadetId: String, newStatus: String = Cadet.STATUS_RELEASED, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = firestoreRepo.archiveCadet(cadetId, newStatus)
            onResult(res.isSuccess)
        }
    }

    fun bulkImportCsv(csvText: String, defaultSquadron: String = "1", onResult: (Int, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val lines = csvText.lines().filter { it.isNotBlank() }
                if (lines.isEmpty()) {
                    onResult(0, "CSV content is empty")
                    return@launch
                }

                val importedCadets = mutableListOf<Cadet>()
                // Check if first line is header
                val startIndex = if (lines[0].contains("Last Name", ignoreCase = true) || lines[0].contains("Name", ignoreCase = true)) 1 else 0

                for (i in startIndex until lines.size) {
                    val line = lines[i]
                    val cols = line.split(",").map { it.trim().removeSurrounding("\"") }
                    if (cols.size >= 2) {
                        val lastName = cols.getOrElse(0) { "" }
                        val firstName = cols.getOrElse(1) { "" }
                        val rank = cols.getOrElse(2) { "Cdt" }.ifBlank { "Cdt" }
                        val sqn = cols.getOrElse(3) { defaultSquadron }.ifBlank { defaultSquadron }
                        val flight = cols.getOrElse(4) { "" }
                        val inst = cols.getOrElse(5) { "" }
                        val appt = cols.getOrElse(6) { "" }
                        val phone = cols.getOrElse(7) { "" }
                        val email = cols.getOrElse(8) { "" }
                        val parentName = cols.getOrElse(9) { "" }
                        val parentPhone = cols.getOrElse(10) { "" }
                        val parentEmail = cols.getOrElse(11) { "" }

                        if (lastName.isNotBlank() && firstName.isNotBlank()) {
                            importedCadets.add(
                                Cadet(
                                    lastName = lastName,
                                    firstName = firstName,
                                    rank = rank,
                                    squadron = sqn,
                                    flight = flight,
                                    instrument = inst,
                                    appointment = appt,
                                    phone = phone,
                                    email = email,
                                    parentName = parentName,
                                    parentPhone = parentPhone,
                                    parentEmail = parentEmail,
                                    status = Cadet.STATUS_ACTIVE
                                )
                            )
                        }
                    }
                }

                if (importedCadets.isEmpty()) {
                    onResult(0, "No valid cadet records found in CSV")
                    return@launch
                }

                val res = firestoreRepo.batchImportCadets(importedCadets)
                onResult(res.getOrDefault(0), null)
            } catch (e: Exception) {
                onResult(0, e.localizedMessage)
            }
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
            val res = firestoreRepo.createEvent(event)
            onResult(res.isSuccess)
        }
    }

    fun updateEvent(event: BandEvent, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = firestoreRepo.updateEvent(event)
            onResult(res.isSuccess)
        }
    }

    fun cancelEvent(eventId: String, reason: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = firestoreRepo.cancelEvent(eventId, reason)
            onResult(res.isSuccess)
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
        viewModelScope.launch {
            val record = AttendanceRecord(
                id = "${event.id}_${cadetId}",
                cadetId = cadetId,
                eventId = event.id,
                status = status,
                note = note,
                timestamp = System.currentTimeMillis(),
                markedBy = staffName
            )
            firestoreRepo.saveAttendanceRecord(record)
        }
    }

    fun markAllPresent(activeCadets: List<Cadet>, currentRecords: List<AttendanceRecord>, onDone: () -> Unit = {}) {
        val event = _attendanceUiState.value.selectedEvent ?: return
        val staffName = _currentStaff.value?.displayName ?: "Staff"
        viewModelScope.launch {
            // Save snapshot for Undo
            _undoSnapshot.value = currentRecords
            firestoreRepo.batchMarkAllPresent(
                eventId = event.id,
                activeCadetIds = activeCadets.map { it.id },
                markedBy = staffName
            )
            onDone()
        }
    }

    fun undoMarkAllPresent() {
        val snapshot = _undoSnapshot.value ?: return
        viewModelScope.launch {
            firestoreRepo.batchRevertAttendance(snapshot)
            _undoSnapshot.value = null
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

    // --- Sheets & Export Operations ---
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

    fun downloadExcelCopy(spreadsheetId: String?, authToken: String?) {
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

            val res = sheetsSyncService.downloadExcelFromGoogleSheet(spreadsheetId, authToken)
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

    fun syncWithGoogleSheets(oauthToken: String) {
        viewModelScope.launch {
            _isSheetsSyncing.value = true
            _syncMessage.value = "Syncing live attendance to Google Sheets..."
            val currentSettings = orgSettings.value
            val res = sheetsSyncService.syncToGoogleSheets(
                oauthToken = oauthToken,
                existingSheetId = currentSettings.googleSheetId.ifBlank { null },
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

    fun startNewTrainingYear(oauthToken: String, newYearLabel: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isSheetsSyncing.value = true
            val currentSettings = orgSettings.value
            val activeCadets = cadets.value.filter { it.status == Cadet.STATUS_ACTIVE }

            val res = sheetsSyncService.startNewTrainingYear(
                oauthToken = oauthToken,
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

    // --- Gemini Chat ---
    fun sendGeminiMessage(prompt: String) {
        if (prompt.isBlank()) return
        val userMsg = ChatMessage(sender = "user", text = prompt)
        val currentList = _chatMessages.value.toMutableList().apply { add(userMsg) }
        _chatMessages.value = currentList

        val staff = _currentStaff.value ?: StaffMember(displayName = "Staff Member", role = "Instructor")

        viewModelScope.launch {
            _isChatLoading.value = true
            val geminiMsg = geminiService.generateGroundedResponse(
                userPrompt = prompt,
                history = currentList,
                settings = orgSettings.value,
                staff = staff,
                cadets = cadets.value,
                events = events.value,
                records = allAttendanceRecords.value
            )
            _isChatLoading.value = false
            _chatMessages.value = _chatMessages.value + geminiMsg
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
}

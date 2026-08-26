package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.ThemeMode
import com.example.model.BandEvent
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainAppScaffold
import com.example.ui.screens.PendingApprovalScreen
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.theme.CadetTrackTheme
import com.example.viewmodel.CadetTrackViewModel
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

class MainActivity : ComponentActivity() {

    private val viewModel: CadetTrackViewModel by viewModels()

    private var pendingOAuthCallback: ((String) -> Unit)? = null
    var oauthErrorInfo by mutableStateOf<String?>(null)

    private val authorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                val data = result.data
                val authResult = Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data)
                val token = authResult.accessToken
                if (!token.isNullOrBlank()) {
                    viewModel.setGoogleOAuthToken(token)
                    val callback = pendingOAuthCallback
                    pendingOAuthCallback = null
                    callback?.invoke(token)
                } else {
                    val errorDetail = "Google Sheets authorization did not return an access token.\n\n" +
                            "Please check:\n" +
                            "1. The SHA-1 signing fingerprint of this app is registered under your OAuth 2.0 Client ID in Google Cloud Console.\n" +
                            "2. Your Google account is listed in the OAuth Consent Screen's Test Users list."
                    oauthErrorInfo = errorDetail
                    viewModel.setSyncMessage("Auth incomplete (No token)")
                    pendingOAuthCallback = null
                }
            } catch (e: ApiException) {
                val errorDetail = "Google Sheets authorization API Error (Code: ${e.statusCode}): ${e.localizedMessage ?: "Authorization rejected"}\n\n" +
                        "Please check:\n" +
                        "1. Ensure the app's SHA-1 certificate fingerprint is registered in Google Cloud Console (Credentials > Android OAuth Client).\n" +
                        "2. Ensure your Google account is added to the Test Users list on the OAuth Consent Screen."
                oauthErrorInfo = errorDetail
                viewModel.setSyncMessage("Auth API Error (Code ${e.statusCode})")
                pendingOAuthCallback = null
            } catch (e: Exception) {
                val errorDetail = "Google Sheets authorization parse failed: ${e.localizedMessage ?: "Unknown error"}\n\n" +
                        "Please check:\n" +
                        "1. Ensure the SHA-1 signing fingerprint is registered in Google Cloud Console.\n" +
                        "2. Ensure your Google account is added as an OAuth Test User."
                oauthErrorInfo = errorDetail
                viewModel.setSyncMessage("Auth parse failed")
                pendingOAuthCallback = null
            }
        } else {
            val data = result.data
            var apiStatusCode: Int? = null
            var apiStatusMsg: String? = null
            if (data != null) {
                try {
                    val authResult = Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data)
                    val token = authResult.accessToken
                    if (!token.isNullOrBlank()) {
                        viewModel.setGoogleOAuthToken(token)
                    }
                } catch (e: ApiException) {
                    apiStatusCode = e.statusCode
                    apiStatusMsg = e.localizedMessage
                } catch (e: Exception) {
                    apiStatusMsg = e.localizedMessage
                }
            }

            val resultCodeName = when (result.resultCode) {
                RESULT_CANCELED -> "RESULT_CANCELED (0)"
                else -> "Result Code ${result.resultCode}"
            }

            val codeInfo = if (apiStatusCode != null) {
                "Google API Status Code: $apiStatusCode (${apiStatusMsg ?: "Cancelled or Not Configured"})\nActivity Result: $resultCodeName"
            } else if (!apiStatusMsg.isNullOrBlank()) {
                "Activity Result: $resultCodeName\nDetails: $apiStatusMsg"
            } else {
                "Activity Result: $resultCodeName"
            }

            val isLikelyUserCancel = result.resultCode == RESULT_CANCELED && apiStatusCode == null && apiStatusMsg == null

            val errorDetail = if (isLikelyUserCancel) {
                "Google Sheets authorization returned $resultCodeName.\n\n" +
                        "If you did not intentionally cancel the prompt, this is usually caused by configuration issues:\n" +
                        "1. Ensure the app's SHA-1 signing fingerprint is registered in Google Cloud Console.\n" +
                        "2. Ensure your Google account is added to the OAuth Consent Screen's Test Users list."
            } else {
                "Google Sheets authorization failed.\n$codeInfo\n\n" +
                        "Configuration Checklist:\n" +
                        "1. Ensure the app's SHA-1 signing fingerprint is registered in Google Cloud Console (APIs & Services > Credentials > OAuth 2.0 Client IDs).\n" +
                        "2. Ensure your Google account is listed on the OAuth Consent Screen's Test Users list."
            }

            oauthErrorInfo = errorDetail
            val shortSyncMsg = if (apiStatusCode != null) "Auth failed (code $apiStatusCode)" else "Auth failed ($resultCodeName)"
            viewModel.setSyncMessage(shortSyncMsg)
            pendingOAuthCallback = null
        }
    }

    fun requestGoogleOAuth(onAuthorized: (String) -> Unit) {
        val currentToken = viewModel.googleOAuthToken.value
        if (!currentToken.isNullOrBlank()) {
            onAuthorized(currentToken)
            return
        }

        val request = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope("https://www.googleapis.com/auth/spreadsheets"),
                    Scope("https://www.googleapis.com/auth/drive.file")
                )
            )
            .build()

        Identity.getAuthorizationClient(this)
            .authorize(request)
            .addOnSuccessListener { authResult ->
                if (authResult.hasResolution()) {
                    val pendingIntent = authResult.pendingIntent
                    if (pendingIntent != null) {
                        pendingOAuthCallback = onAuthorized
                        val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        authorizationLauncher.launch(intentSenderRequest)
                    } else {
                        val msg = "Google authorization resolution pending intent was missing."
                        oauthErrorInfo = msg
                        viewModel.setSyncMessage(msg)
                    }
                } else {
                    val token = authResult.accessToken
                    if (!token.isNullOrBlank()) {
                        viewModel.setGoogleOAuthToken(token)
                        onAuthorized(token)
                    } else {
                        val msg = "Authorization completed but returned an empty access token."
                        oauthErrorInfo = msg
                        viewModel.setSyncMessage(msg)
                    }
                }
            }
            .addOnFailureListener { e ->
                val status = (e as? ApiException)?.statusCode
                val codeStr = if (status != null) " (code $status)" else ""
                val errorDetail = "Google Sheets authorization failed$codeStr: ${e.localizedMessage}\n\n" +
                        "Configuration Checklist:\n" +
                        "1. Ensure the app's SHA-1 signing fingerprint is registered in Google Cloud Console.\n" +
                        "2. Ensure your Google account is added to the OAuth Consent Screen's Test Users list."
                oauthErrorInfo = errorDetail
                viewModel.setSyncMessage("Auth error$codeStr: ${e.localizedMessage}")
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            CadetTrackTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CadetTrackApp(viewModel = viewModel, activity = this)
                }
            }
        }
    }
}

@Composable
fun CadetTrackApp(
    viewModel: CadetTrackViewModel,
    activity: MainActivity
) {
    val isFirebaseConfigured by viewModel.isFirebaseConfigured.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentStaff by viewModel.currentStaff.collectAsState()
    val profileLoadError by viewModel.profileLoadError.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    val isOrgSettingsLoaded by viewModel.isOrgSettingsLoaded.collectAsState()
    val orgSettings by viewModel.orgSettings.collectAsState()
    val staffMembers by viewModel.staffMembers.collectAsState()
    val pendingStaffRequests by viewModel.pendingStaffRequests.collectAsState()
    val cadets by viewModel.cadets.collectAsState()
    val events by viewModel.events.collectAsState()
    val allRecords by viewModel.allAttendanceRecords.collectAsState()

    val attendanceUiState by viewModel.attendanceUiState.collectAsState()
    val undoSnapshot by viewModel.undoSnapshot.collectAsState()
    val reportsUiState by viewModel.reportsUiState.collectAsState()

    val chatMessages by viewModel.chatMessages.collectAsState()
    val chatSessions by viewModel.chatSessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()

    val isSheetsSyncing by viewModel.isSheetsSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    val themeMode by viewModel.themeMode.collectAsState()

    val squadronReports = viewModel.getSquadronReports()

    // Safe retrieval of Web Client ID from string resources
    val webClientId = try {
        val resId = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        if (resId != 0) activity.getString(resId) else "YOUR_WEB_CLIENT_ID_HERE"
    } catch (e: Exception) {
        "YOUR_WEB_CLIENT_ID_HERE"
    }

    val appScreen = when {
        currentUser == null -> 0 // Auth
        currentStaff == null || !isOrgSettingsLoaded -> 1 // Loading Profile
        !currentStaff!!.isAuthorized -> 2 // Pending Approval
        currentStaff!!.isAdmin && orgSettings.orgName.isBlank() -> 3 // Setup Wizard
        else -> 4 // Main App Scaffold
    }

    AnimatedContent(
        targetState = appScreen,
        transitionSpec = {
            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(180))
        },
        label = "AppScreenTransition"
    ) { screen ->
        when (screen) {
            0 -> {
                // Not signed in -> Show Login/Signup Screen
                AuthScreen(
                    isFirebaseConfigured = isFirebaseConfigured,
                    isLoading = isAuthLoading,
                    authError = authError,
                    onSignIn = { email, pass ->
                        viewModel.signInWithEmail(email, pass)
                    },
                    onSignUp = { email, pass, name, rank, role ->
                        viewModel.signUpWithEmail(email, pass, name, rank, role)
                    },
                    onGoogleSignIn = {
                        viewModel.signInWithGoogle(activity, webClientId)
                    },
                    onResetPassword = { email, callback ->
                        viewModel.sendPasswordReset(email, callback)
                    },
                    onCancelLoading = {
                        viewModel.cancelAuthLoading()
                    }
                )
            }

            1 -> {
                // Signed in, waiting for real staff database document fetch
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.widthIn(max = 420.dp)
                    ) {
                        if (profileLoadError != null) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                            Text(
                                text = "Couldn't load your profile",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = profileLoadError ?: "Failed to verify database authorization.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.size(24.dp))
                            Button(
                                onClick = { viewModel.retryLoadProfile() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Retry")
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            TextButton(
                                onClick = { viewModel.signOut() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Sign Out", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                            Text(
                                text = "Loading CadetTrack Profile...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Verifying database credentials and permissions...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.size(24.dp))
                            TextButton(
                                onClick = { viewModel.signOut() }
                            ) {
                                Text("Sign Out", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            2 -> {
                // Signed in, but not yet authorized by an admin
                PendingApprovalScreen(
                    staff = currentStaff,
                    onSignOut = { viewModel.signOut() }
                )
            }

            3 -> {
                // Bootstrap Admin on first run: Org settings not yet filled out -> First-Run Setup Wizard
                SetupWizardScreen(
                    onCompleteSetup = { newSettings, initialCadets, startDateStr ->
                        // Optimistically update settings immediately to transition to Main screen without delay
                        viewModel.saveOrgSettings(newSettings)
                        if (initialCadets.isNotEmpty()) {
                            for (cadet in initialCadets) {
                                viewModel.addCadet(cadet)
                            }
                        }
                        viewModel.generateYearEvents(
                            startDate = startDateStr,
                            trainingDay = newSettings.defaultTrainingNightDay,
                            trainingTime = newSettings.defaultTrainingNightTime,
                            bandDay = newSettings.defaultBandPracticeDay,
                            bandTime = newSettings.defaultBandPracticeTime,
                            location = newSettings.homeLocation,
                            squadrons = newSettings.squadronNumbers
                        )
                    }
                )
            }

            else -> {
                // Authorized staff -> Main App Screen with 4 tabs
                MainAppScaffold(
                    staff = currentStaff,
                    staffMembers = staffMembers,
                    pendingStaffRequests = pendingStaffRequests,
                    orgSettings = orgSettings,
                    cadets = cadets,
                    events = events,
                    allRecords = allRecords,
                    attendanceUiState = attendanceUiState,
                    undoSnapshot = undoSnapshot,
                    chatSessions = chatSessions,
                    currentSessionId = currentSessionId,
                    chatMessages = chatMessages,
                    isChatLoading = isChatLoading,
                    isSheetsSyncing = isSheetsSyncing,
                    syncMessage = syncMessage,
                    squadronReports = squadronReports,
                    currentThemeMode = themeMode,
                    onSetThemeMode = { viewModel.setThemeMode(it) },
                    onSelectEvent = { viewModel.selectEvent(it) },
                    onSearchChange = { viewModel.updateAttendanceSearch(it) },
                    onFilterStatusChange = { viewModel.updateAttendanceFilterStatus(it) },
                    onFilterSquadronChange = { viewModel.updateAttendanceFilterSquadron(it) },
                    onMarkStatus = { cadetId, status, note ->
                        viewModel.markAttendance(cadetId, status, note)
                    },
                    onMarkAllPresent = { activeCadetsList, currentRecs, onDone ->
                        viewModel.markAllPresent(activeCadetsList, currentRecs, onDone)
                    },
                    onUndoMarkAll = { viewModel.undoMarkAllPresent() },
                    onClearUndoSnapshot = { viewModel.clearUndoSnapshot() },
                    onCreateEvent = { viewModel.createEvent(it) },
                    onEditEvent = { viewModel.updateEvent(it) },
                    onCancelEvent = { eventId, reason ->
                        viewModel.cancelEvent(eventId, reason)
                    },
                    onUncancelEvent = { eventId ->
                        viewModel.uncancelEvent(eventId)
                    },
                    onAddCadet = { cadet, callback ->
                        viewModel.addCadet(cadet, callback)
                    },
                    onUpdateCadet = { cadet, callback ->
                        viewModel.updateCadet(cadet, callback)
                    },
                    onArchiveCadet = { id, status ->
                        viewModel.archiveCadet(id, status)
                    },
                    onDeleteCadetPermanently = { id ->
                        viewModel.deleteCadetPermanently(id)
                    },
                    onBulkImportCsv = { csvText, callback ->
                        viewModel.bulkImportCsv(csvText, onResult = callback)
                    },
                    onSyncSheets = {
                        activity.requestGoogleOAuth { token ->
                            viewModel.syncWithGoogleSheets(token)
                        }
                    },
                    onDownloadExcel = { sheetId, token ->
                        if (token != null) {
                            viewModel.downloadExcelCopy(sheetId, token)
                        } else {
                            activity.requestGoogleOAuth { realToken ->
                                viewModel.downloadExcelCopy(sheetId, realToken)
                            }
                        }
                    },
                    onExportCsv = { squadron ->
                        viewModel.exportToCsv(squadron)
                    },
                    onStartNewYear = { newYearLabel, onDone ->
                        activity.requestGoogleOAuth { token ->
                            viewModel.startNewTrainingYear(token, newYearLabel, onDone)
                        }
                    },
                    onSendMessage = { prompt, format -> viewModel.sendGeminiMessage(prompt, format) },
                    onSelectChatSession = { viewModel.selectChatSession(it) },
                    onNewChatSession = { viewModel.createNewChatSession() },
                    onDeleteChatSession = { viewModel.deleteChatSession(it) },
                    onSaveChatLog = { ctx -> viewModel.saveChatLogToDevice(ctx) },
                    onClearChat = { viewModel.clearChatMessages() },
                    onUpdateStaffAuth = { targetStaff, isAuth, isAdmin, sqns ->
                        viewModel.updateStaffAuthorization(targetStaff, isAuth, isAdmin, sqns)
                    },
                    onToggleSyncAccess = { targetStaff, grantAccess, callback ->
                        activity.requestGoogleOAuth { adminToken ->
                            viewModel.updateStaffSyncAccess(targetStaff, grantAccess, adminToken, callback)
                        }
                    },
                    onApproveStaff = { staffToApprove, asAdmin ->
                        viewModel.approveStaffRequest(staffToApprove, asAdmin)
                    },
                    onRejectStaff = { staffToReject ->
                        viewModel.rejectStaffRequest(staffToReject)
                    },
                    onUpdateOwnProfile = { name, rank, role, onDone ->
                        viewModel.updateOwnProfile(name, rank, role, onDone)
                    },
                    onUpdatePassword = { newPass, callback ->
                        viewModel.updateAccountPassword(newPass, callback)
                    },
                    onSendPasswordReset = { email, callback ->
                        viewModel.sendPasswordReset(email, callback)
                    },
                    onSaveOrgSettings = { settings ->
                        viewModel.saveOrgSettings(settings)
                    },
                    onSignOut = { viewModel.signOut() },
                    getCadetStats = { viewModel.getCadetStats(it) }
                )
            }
        }
    }

    if (activity.oauthErrorInfo != null) {
        AlertDialog(
            onDismissRequest = { activity.oauthErrorInfo = null },
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "OAuth Issue",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Google Sheets Authorization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = activity.oauthErrorInfo!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = { activity.oauthErrorInfo = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Understood")
                }
            }
        )
    }
}


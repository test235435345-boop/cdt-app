package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.ThemeMode
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainAppScaffold
import com.example.ui.screens.PendingApprovalScreen
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.theme.CadetTrackTheme
import com.example.viewmodel.CadetTrackViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CadetTrackViewModel by viewModels()

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
    activity: ComponentActivity
) {
    val isFirebaseConfigured by viewModel.isFirebaseConfigured.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentStaff by viewModel.currentStaff.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    val orgSettings by viewModel.orgSettings.collectAsState()
    val staffMembers by viewModel.staffMembers.collectAsState()
    val cadets by viewModel.cadets.collectAsState()
    val events by viewModel.events.collectAsState()
    val allRecords by viewModel.allAttendanceRecords.collectAsState()

    val attendanceUiState by viewModel.attendanceUiState.collectAsState()
    val undoSnapshot by viewModel.undoSnapshot.collectAsState()
    val reportsUiState by viewModel.reportsUiState.collectAsState()

    val chatMessages by viewModel.chatMessages.collectAsState()
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

    when {
        // Not signed in -> Show Login/Signup Screen
        currentUser == null -> {
            AuthScreen(
                isFirebaseConfigured = isFirebaseConfigured,
                isLoading = isAuthLoading,
                authError = authError,
                onSignIn = { email, pass ->
                    viewModel.signInWithEmail(email, pass)
                },
                onSignUp = { email, pass, name, role ->
                    viewModel.signUpWithEmail(email, pass, name, role)
                },
                onGoogleSignIn = {
                    viewModel.signInWithGoogle(activity, webClientId)
                },
                onResetPassword = { email, callback ->
                    viewModel.sendPasswordReset(email, callback)
                }
            )
        }

        // Signed in, waiting for staff document fetch
        currentStaff == null -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            }
        }

        // Signed in, but not yet authorized by an admin
        !currentStaff!!.isAuthorized -> {
            PendingApprovalScreen(
                staff = currentStaff,
                onSignOut = { viewModel.signOut() }
            )
        }

        // Bootstrap Admin on first run: Org settings not yet filled out -> First-Run Setup Wizard
        currentStaff!!.isAdmin && orgSettings.orgName.isBlank() -> {
            SetupWizardScreen(
                onCompleteSetup = { newSettings, initialCadets, startDateStr ->
                    viewModel.saveOrgSettings(newSettings) { success ->
                        if (success) {
                            // Import cadets if any were entered
                            if (initialCadets.isNotEmpty()) {
                                for (cadet in initialCadets) {
                                    viewModel.addCadet(cadet)
                                }
                            }
                            // Generate recurring empty dated event shells for the training year
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
                    }
                }
            )
        }

        // Authorized staff -> Main App Screen with 4 tabs
        else -> {
            MainAppScaffold(
                staff = currentStaff,
                staffMembers = staffMembers,
                orgSettings = orgSettings,
                cadets = cadets,
                events = events,
                allRecords = allRecords,
                attendanceUiState = attendanceUiState,
                undoSnapshot = undoSnapshot,
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
                onCancelEvent = { eventId, reason ->
                    viewModel.cancelEvent(eventId, reason)
                },
                onAddCadet = { viewModel.addCadet(it) },
                onUpdateCadet = { viewModel.updateCadet(it) },
                onArchiveCadet = { id, status ->
                    viewModel.archiveCadet(id, status)
                },
                onBulkImportCsv = { csvText, callback ->
                    viewModel.bulkImportCsv(csvText, onResult = callback)
                },
                onSyncSheets = { token ->
                    viewModel.syncWithGoogleSheets(token)
                },
                onDownloadExcel = { sheetId, token ->
                    viewModel.downloadExcelCopy(sheetId, token)
                },
                onExportCsv = { squadron ->
                    viewModel.exportToCsv(squadron)
                },
                onStartNewYear = { token, newYearLabel, onDone ->
                    viewModel.startNewTrainingYear(token, newYearLabel, onDone)
                },
                onSendMessage = { viewModel.sendGeminiMessage(it) },
                onUpdateStaffAuth = { targetStaff, isAuth, isAdmin, sqns ->
                    viewModel.updateStaffAuthorization(targetStaff, isAuth, isAdmin, sqns)
                },
                onSignOut = { viewModel.signOut() },
                getCadetStats = { viewModel.getCadetStats(it) }
            )
        }
    }
}


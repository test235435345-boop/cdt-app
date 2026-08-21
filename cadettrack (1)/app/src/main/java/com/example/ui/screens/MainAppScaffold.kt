package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ThemeMode
import com.example.model.AttendanceRecord
import com.example.model.BandEvent
import com.example.model.Cadet
import com.example.model.CadetAttendanceStats
import com.example.model.ChatMessage
import com.example.model.OrgSettings
import com.example.model.SquadronReport
import com.example.model.StaffMember
import com.example.ui.components.CadetBadgeLogo
import com.example.ui.components.ManageStaffDialog
import com.example.ui.components.SecurityRulesDialog
import com.example.viewmodel.AttendanceUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    staff: StaffMember?,
    staffMembers: List<StaffMember>,
    orgSettings: OrgSettings,
    cadets: List<Cadet>,
    events: List<BandEvent>,
    allRecords: List<AttendanceRecord>,
    attendanceUiState: AttendanceUiState,
    undoSnapshot: List<AttendanceRecord>?,
    chatMessages: List<ChatMessage>,
    isChatLoading: Boolean,
    isSheetsSyncing: Boolean,
    syncMessage: String?,
    squadronReports: List<SquadronReport>,
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onSetThemeMode: (ThemeMode) -> Unit = {},
    onSelectEvent: (BandEvent?) -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterStatusChange: (String) -> Unit,
    onFilterSquadronChange: (String) -> Unit,
    onMarkStatus: (cadetId: String, status: String, note: String) -> Unit,
    onMarkAllPresent: (List<Cadet>, List<AttendanceRecord>, () -> Unit) -> Unit,
    onUndoMarkAll: () -> Unit,
    onClearUndoSnapshot: () -> Unit,
    onCreateEvent: (BandEvent) -> Unit,
    onCancelEvent: (eventId: String, reason: String) -> Unit,
    onAddCadet: (Cadet) -> Unit,
    onUpdateCadet: (Cadet) -> Unit,
    onArchiveCadet: (cadetId: String, status: String) -> Unit,
    onBulkImportCsv: (csvText: String, (Int, String?) -> Unit) -> Unit,
    onSyncSheets: (token: String) -> Unit,
    onDownloadExcel: (sheetId: String?, token: String?) -> Unit,
    onExportCsv: (squadron: String?) -> Unit,
    onStartNewYear: (token: String, newYearLabel: String, (Boolean) -> Unit) -> Unit,
    onSendMessage: (String) -> Unit,
    onUpdateStaffAuth: (StaffMember, Boolean, Boolean, List<String>) -> Unit,
    onSignOut: () -> Unit,
    getCadetStats: (Cadet) -> CadetAttendanceStats
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Attendance, 1 = Cadets, 2 = Reports, 3 = Ask Gemini
    var showMenu by remember { mutableStateOf(false) }
    var showManageStaffDialog by remember { mutableStateOf(false) }
    var showSecurityRulesDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CadetBadgeLogo(size = 32.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CadetTrack",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (orgSettings.orgName.isNotBlank()) {
                                Text(
                                    text = orgSettings.orgName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = when (currentThemeMode) {
                                        ThemeMode.SYSTEM -> "Theme: Auto (System)"
                                        ThemeMode.LIGHT -> "Theme: Light"
                                        ThemeMode.DARK -> "Theme: Dark"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (currentThemeMode) {
                                        ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                        ThemeMode.LIGHT -> Icons.Default.Brightness7
                                        ThemeMode.DARK -> Icons.Default.Brightness4
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                val nextMode = when (currentThemeMode) {
                                    ThemeMode.SYSTEM -> ThemeMode.DARK
                                    ThemeMode.DARK -> ThemeMode.LIGHT
                                    ThemeMode.LIGHT -> ThemeMode.SYSTEM
                                }
                                onSetThemeMode(nextMode)
                            }
                        )

                        HorizontalDivider()

                        if (staff?.isAdmin == true) {
                            DropdownMenuItem(
                                text = { Text("Manage Staff") },
                                leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showMenu = false
                                    showManageStaffDialog = true
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("Firestore Rules") },
                            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showMenu = false
                                showSecurityRulesDialog = true
                            }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text("Signed in as ${staff?.displayName ?: "Staff"}") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                            onClick = {}
                        )

                        DropdownMenuItem(
                            text = { Text("Sign Out", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onSignOut()
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Attendance") },
                    label = { Text("Attendance", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.testTag("tab_attendance")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Cadets") },
                    label = { Text("Cadets", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.testTag("tab_cadets")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                    label = { Text("Reports", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.testTag("tab_reports")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Ask Gemini") },
                    label = { Text("Ask Gemini", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.testTag("tab_ask_gemini")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> AttendanceScreen(
                    orgSettings = orgSettings,
                    cadets = cadets,
                    events = events,
                    allRecords = allRecords,
                    uiState = attendanceUiState,
                    undoSnapshot = undoSnapshot,
                    onSelectEvent = onSelectEvent,
                    onSearchChange = onSearchChange,
                    onFilterStatusChange = onFilterStatusChange,
                    onFilterSquadronChange = onFilterSquadronChange,
                    onMarkStatus = onMarkStatus,
                    onMarkAllPresent = onMarkAllPresent,
                    onUndoMarkAll = onUndoMarkAll,
                    onClearUndoSnapshot = onClearUndoSnapshot,
                    onCreateEvent = onCreateEvent,
                    onCancelEvent = onCancelEvent,
                    onUpdateCadet = onUpdateCadet,
                    onArchiveCadet = onArchiveCadet,
                    getCadetStats = getCadetStats
                )

                1 -> CadetsScreen(
                    orgSettings = orgSettings,
                    cadets = cadets,
                    onAddCadet = onAddCadet,
                    onUpdateCadet = onUpdateCadet,
                    onArchiveCadet = onArchiveCadet,
                    onBulkImportCsv = onBulkImportCsv,
                    getCadetStats = getCadetStats
                )

                2 -> ReportsScreen(
                    orgSettings = orgSettings,
                    staff = staff,
                    cadets = cadets,
                    events = events,
                    allRecords = allRecords,
                    squadronReports = squadronReports,
                    isSheetsSyncing = isSheetsSyncing,
                    syncMessage = syncMessage,
                    onSyncSheets = onSyncSheets,
                    onDownloadExcel = onDownloadExcel,
                    onExportCsv = onExportCsv,
                    onStartNewYear = onStartNewYear,
                    getCadetStats = getCadetStats,
                    onUpdateCadet = onUpdateCadet,
                    onArchiveCadet = onArchiveCadet
                )

                3 -> GeminiChatScreen(
                    staff = staff,
                    cadets = cadets,
                    messages = chatMessages,
                    isLoading = isChatLoading,
                    onSendMessage = onSendMessage
                )
            }
        }
    }

    if (showManageStaffDialog) {
        ManageStaffDialog(
            staffMembers = staffMembers,
            currentStaff = staff,
            onUpdateStaff = { target, auth, admin, sqns ->
                onUpdateStaffAuth(target, auth, admin, sqns)
            },
            onDismiss = { showManageStaffDialog = false }
        )
    }

    if (showSecurityRulesDialog) {
        SecurityRulesDialog(
            onDismiss = { showSecurityRulesDialog = false }
        )
    }
}

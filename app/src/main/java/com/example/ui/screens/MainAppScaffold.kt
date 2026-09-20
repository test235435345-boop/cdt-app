package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.model.ChatSession
import com.example.model.OrgSettings
import com.example.model.SquadronReport
import com.example.model.StaffMember
import com.example.ui.components.CadetBadgeLogo
import com.example.ui.components.ManageStaffDialog
import com.example.ui.components.ManageSyncAccessDialog
import com.example.ui.components.MyProfileDialog
import com.example.ui.components.SecurityRulesDialog
import com.example.viewmodel.AttendanceUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    staff: StaffMember?,
    staffMembers: List<StaffMember>,
    pendingStaffRequests: List<StaffMember> = emptyList(),
    orgSettings: OrgSettings,
    cadets: List<Cadet>,
    events: List<BandEvent>,
    allRecords: List<AttendanceRecord>,
    attendanceUiState: AttendanceUiState,
    undoSnapshot: List<AttendanceRecord>?,
    chatSessions: List<ChatSession> = emptyList(),
    currentSessionId: String = "",
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
    onEditEvent: (BandEvent) -> Unit = {},
    onCancelEvent: (eventId: String, reason: String) -> Unit,
    onUncancelEvent: (eventId: String) -> Unit = {},
    onAddCadet: (Cadet, (Boolean, String?) -> Unit) -> Unit = { _, cb -> cb(true, null) },
    onUpdateCadet: (Cadet, (Boolean, String?) -> Unit) -> Unit = { _, cb -> cb(true, null) },
    onArchiveCadet: (cadetId: String, status: String) -> Unit,
    onDeleteCadetPermanently: (cadetId: String) -> Unit = {},
    onBulkImportCsv: (csvText: String, (Int, String?) -> Unit) -> Unit,
    onSyncSheets: () -> Unit,
    onDownloadExcel: (sheetId: String?, token: String?) -> Unit,
    onExportCsv: (squadron: String?) -> Unit,
    onStartNewYear: (newYearLabel: String, (Boolean) -> Unit) -> Unit,
    isPdfGenerating: Boolean = false,
    lastPdfUpdated: Long = 0L,
    onGenerateOrUpdatePdf: () -> Unit = {},
    onOpenPdf: () -> Unit = {},
    onSharePdf: () -> Unit = {},
    onSendMessage: (prompt: String, format: String) -> Unit,
    onSelectChatSession: ((String) -> Unit)? = null,
    onNewChatSession: (() -> Unit)? = null,
    onDeleteChatSession: ((String) -> Unit)? = null,
    onSaveChatLog: ((android.content.Context) -> Unit)? = null,
    onClearChat: (() -> Unit)? = null,
    onUpdateStaffAuth: (StaffMember, Boolean, Boolean, List<String>) -> Unit,
    onToggleSyncAccess: (StaffMember, Boolean, (Boolean, String?) -> Unit) -> Unit = { _, _, cb -> cb(true, null) },
    onApproveStaff: ((StaffMember, Boolean) -> Unit)? = null,
    onRejectStaff: ((StaffMember) -> Unit)? = null,
    onUpdateOwnProfile: (displayName: String, rank: String, role: String, (Boolean, String?) -> Unit) -> Unit = { _, _, _, _ -> },
    onUpdatePassword: ((String, (Boolean, String?) -> Unit) -> Unit)? = null,
    onSendPasswordReset: ((String, (Boolean, String) -> Unit) -> Unit)? = null,
    onSaveOrgSettings: ((OrgSettings) -> Unit)? = null,
    onSignOut: () -> Unit,
    getCadetStats: (Cadet) -> CadetAttendanceStats
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Attendance, 1 = Cadets, 2 = Reports, 3 = Ask Gemini
    var showMenu by remember { mutableStateOf(false) }
    var showManageStaffDialog by remember { mutableStateOf(false) }
    var showManageSyncAccessDialog by remember { mutableStateOf(false) }
    var showOrgSettingsDialog by remember { mutableStateOf(false) }
    var showSecurityRulesDialog by remember { mutableStateOf(false) }
    var showMyProfileDialog by remember { mutableStateOf(false) }

    val liveSheetUrl = remember(orgSettings.googleSheetUrl, orgSettings.googleSheetId) {
        if (orgSettings.googleSheetUrl.isNotBlank()) {
            orgSettings.googleSheetUrl
        } else if (orgSettings.googleSheetId.isNotBlank()) {
            "https://docs.google.com/spreadsheets/d/${orgSettings.googleSheetId}/edit"
        } else {
            ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CadetBadgeLogo(size = 34.dp)
                        Spacer(modifier = Modifier.width(10.dp))
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    if (staff?.isAdmin == true && pendingStaffRequests.isNotEmpty()) {
                        IconButton(onClick = { showManageStaffDialog = true }) {
                            Box {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Pending Requests",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${pendingStaffRequests.size}",
                                            color = MaterialTheme.colorScheme.onError,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IconButton(onClick = { showMyProfileDialog = true }) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "My Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

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

                        if (liveSheetUrl.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Open Google Sheet") },
                                leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showMenu = false
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(liveSheetUrl))
                                    context.startActivity(intent)
                                }
                            )
                        }

                        if (staff?.isAdmin == true) {
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Manage Staff")
                                        if (pendingStaffRequests.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.error,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "${pendingStaffRequests.size}",
                                                    color = MaterialTheme.colorScheme.onError,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showMenu = false
                                    showManageStaffDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Manage Sync Access") },
                                leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showMenu = false
                                    showManageSyncAccessDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Squadron & Unit Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Squadron & Unit Settings", tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showMenu = false
                                    showOrgSettingsDialog = true
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
                            text = { Text("My Profile (${staff?.displayName ?: "Staff"})") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "My Profile", tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showMenu = false
                                showMyProfileDialog = true
                            },
                            modifier = Modifier.testTag("menu_my_profile")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Persistent In-App Notification Alert for Pending Account Creation Requests with smooth animation
            AnimatedVisibility(
                visible = staff?.isAdmin == true && pendingStaffRequests.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (pendingStaffRequests.isNotEmpty()) {
                    val topRequest = pendingStaffRequests.first()
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (pendingStaffRequests.size == 1) "1 New Account Request" else "${pendingStaffRequests.size} New Account Requests",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                TextButton(
                                    onClick = { showManageStaffDialog = true },
                                    modifier = Modifier.padding(0.dp)
                                ) {
                                    Text("View All", fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = "${topRequest.displayName} (${topRequest.email}) is requesting staff access.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        onApproveStaff?.invoke(topRequest, false)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Approve Staff", style = MaterialTheme.typography.labelSmall)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onApproveStaff?.invoke(topRequest, true)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Approve Admin", style = MaterialTheme.typography.labelSmall)
                                }

                                if (onRejectStaff != null) {
                                    OutlinedButton(
                                        onClick = { onRejectStaff.invoke(topRequest) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(0.7f)
                                    ) {
                                        Text("Deny", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(animationSpec = tween(220)) { width -> width / 4 } + fadeIn(animationSpec = tween(220)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { width -> -width / 4 } + fadeOut(animationSpec = tween(180)))
                        } else {
                            (slideInHorizontally(animationSpec = tween(220)) { width -> -width / 4 } + fadeIn(animationSpec = tween(220)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { width -> width / 4 } + fadeOut(animationSpec = tween(180)))
                        }
                    },
                    label = "TabContentTransition"
                ) { tab ->
                    when (tab) {
                        0 -> AttendanceScreen(
                            orgSettings = orgSettings,
                            cadets = cadets,
                            events = events,
                            allRecords = allRecords,
                            uiState = attendanceUiState,
                            undoSnapshot = undoSnapshot,
                            isAdmin = staff?.isAdmin == true,
                            onSelectEvent = onSelectEvent,
                            onSearchChange = onSearchChange,
                            onFilterStatusChange = onFilterStatusChange,
                            onFilterSquadronChange = onFilterSquadronChange,
                            onMarkStatus = onMarkStatus,
                            onMarkAllPresent = onMarkAllPresent,
                            onUndoMarkAll = onUndoMarkAll,
                            onClearUndoSnapshot = onClearUndoSnapshot,
                            onCreateEvent = onCreateEvent,
                            onEditEvent = onEditEvent,
                            onCancelEvent = onCancelEvent,
                            onUncancelEvent = onUncancelEvent,
                            onUpdateCadet = onUpdateCadet,
                            onArchiveCadet = onArchiveCadet,
                            onDeleteCadetPermanently = onDeleteCadetPermanently,
                            getCadetStats = getCadetStats
                        )

                        1 -> CadetsScreen(
                            orgSettings = orgSettings,
                            cadets = cadets,
                            isAdmin = staff?.isAdmin == true,
                            onAddCadet = onAddCadet,
                            onUpdateCadet = onUpdateCadet,
                            onArchiveCadet = onArchiveCadet,
                            onDeleteCadetPermanently = onDeleteCadetPermanently,
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
                            onDeleteCadetPermanently = onDeleteCadetPermanently,
                            isPdfGenerating = isPdfGenerating,
                            lastPdfUpdated = lastPdfUpdated,
                            onGenerateOrUpdatePdf = onGenerateOrUpdatePdf,
                            onOpenPdf = onOpenPdf,
                            onSharePdf = onSharePdf,
                            getCadetStats = getCadetStats,
                            onUpdateCadet = onUpdateCadet,
                            onArchiveCadet = onArchiveCadet
                        )

                        3 -> GeminiChatScreen(
                            staff = staff,
                            cadets = cadets,
                            sessions = chatSessions,
                            currentSessionId = currentSessionId,
                            messages = chatMessages,
                            isLoading = isChatLoading,
                            onSendMessage = onSendMessage,
                            onSelectSession = onSelectChatSession,
                            onNewSession = onNewChatSession,
                            onDeleteSession = onDeleteChatSession,
                            onQuickAction = { targetScreen, _ ->
                                when (targetScreen.lowercase()) {
                                    "attendance" -> selectedTab = 0
                                    "cadets", "cadet_profile" -> selectedTab = 1
                                    "reports" -> selectedTab = 2
                                    "chat", "gemini", "ask_gemini" -> selectedTab = 3
                                }
                            },
                            getCadetStats = getCadetStats,
                            onUpdateCadet = onUpdateCadet,
                            onArchiveCadet = onArchiveCadet,
                            onSaveChatLog = onSaveChatLog,
                            onClearChat = onClearChat
                        )
                    }
                }
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
            onRejectStaff = { target ->
                onRejectStaff?.invoke(target)
            },
            onDismiss = { showManageStaffDialog = false }
        )
    }

    if (showManageSyncAccessDialog && staff?.isAdmin == true) {
        ManageSyncAccessDialog(
            staffMembers = staffMembers,
            currentStaff = staff,
            onToggleSyncAccess = onToggleSyncAccess,
            onDismiss = { showManageSyncAccessDialog = false }
        )
    }

    if (showOrgSettingsDialog && onSaveOrgSettings != null) {
        com.example.ui.components.OrgSettingsDialog(
            initialSettings = orgSettings,
            onDismiss = { showOrgSettingsDialog = false },
            onSave = { updated ->
                onSaveOrgSettings(updated)
            }
        )
    }

    if (showSecurityRulesDialog) {
        SecurityRulesDialog(
            onDismiss = { showSecurityRulesDialog = false }
        )
    }

    if (showMyProfileDialog) {
        MyProfileDialog(
            staff = staff,
            onDismiss = { showMyProfileDialog = false },
            onSave = { displayName, rank, role, onComplete ->
                onUpdateOwnProfile(displayName, rank, role, onComplete)
            },
            onUpdatePassword = onUpdatePassword,
            onSendPasswordReset = onSendPasswordReset
        )
    }
}

package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.AttendanceRecord
import com.example.model.BandEvent
import com.example.model.Cadet
import com.example.model.CadetAttendanceStats
import com.example.model.OrgSettings
import com.example.model.SquadronReport
import com.example.model.StaffMember
import com.example.ui.components.CadetProfileDialog
import com.example.ui.components.EditCadetDialog
import com.example.ui.theme.AbsentContainer
import com.example.ui.theme.AbsentContent
import com.example.ui.theme.LateContainer
import com.example.ui.theme.LateContent
import com.example.ui.theme.PresentContainer
import com.example.ui.theme.PresentContent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    orgSettings: OrgSettings,
    staff: StaffMember?,
    cadets: List<Cadet>,
    events: List<BandEvent>,
    allRecords: List<AttendanceRecord>,
    squadronReports: List<SquadronReport>,
    isSheetsSyncing: Boolean,
    syncMessage: String?,
    onSyncSheets: () -> Unit,
    onDownloadExcel: (sheetId: String?, token: String?) -> Unit,
    onExportCsv: (squadron: String?) -> Unit,
    onStartNewYear: (newYearLabel: String, (Boolean) -> Unit) -> Unit,
    onDeleteCadetPermanently: (cadetId: String) -> Unit = {},
    getCadetStats: (Cadet) -> CadetAttendanceStats,
    onUpdateCadet: (Cadet, (Boolean, String?) -> Unit) -> Unit = { _, cb -> cb(true, null) },
    onArchiveCadet: (cadetId: String, status: String) -> Unit
) {
    val context = LocalContext.current
    var selectedCadetForDetail by remember { mutableStateOf<Cadet?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showNewYearDialog by remember { mutableStateOf(false) }
    var newYearInput by remember { mutableStateOf("2027-2028") }
    var showFlaggedOnly by remember { mutableStateOf(false) }
    var selectedFilterSquadron by remember { mutableStateOf("All") }

    val liveSheetUrl = remember(orgSettings.googleSheetUrl, orgSettings.googleSheetId) {
        if (orgSettings.googleSheetUrl.isNotBlank()) {
            orgSettings.googleSheetUrl
        } else if (orgSettings.googleSheetId.isNotBlank()) {
            "https://docs.google.com/spreadsheets/d/${orgSettings.googleSheetId}/edit"
        } else {
            ""
        }
    }

    val activeCadets = cadets.filter { it.status == Cadet.STATUS_ACTIVE }
    val allStats = activeCadets.map { getCadetStats(it) }

    val overallAvg = if (allStats.isNotEmpty()) {
        allStats.map { it.percentage }.average().toFloat()
    } else 0f

    val flaggedCadets = allStats.filter { it.isBelowThreshold }

    val filteredCadetsList = (if (showFlaggedOnly) flaggedCadets.map { it.cadet } else activeCadets).filter {
        selectedFilterSquadron == "All" || it.squadron == selectedFilterSquadron
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Attendance Reports",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${orgSettings.orgName.ifBlank { "Cadet Band" }} • Target: ${orgSettings.attendanceThreshold}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Overall Attendance Summary Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Overall Attendance",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${overallAvg.toInt()}%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (overallAvg >= orgSettings.attendanceThreshold) PresentContent else AbsentContent
                            )
                        }

                        Surface(
                            color = if (flaggedCadets.isNotEmpty()) AbsentContainer else PresentContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.clickable { showFlaggedOnly = !showFlaggedOnly }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                if (flaggedCadets.isNotEmpty()) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AbsentContent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (flaggedCadets.isNotEmpty()) "${flaggedCadets.size} Below ${orgSettings.attendanceThreshold}%" else "All On Track",
                                    color = if (flaggedCadets.isNotEmpty()) AbsentContent else PresentContent,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { overallAvg / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = if (overallAvg >= orgSettings.attendanceThreshold) PresentContent else AbsentContent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // 2. Squadron Breakdown Cards
        item {
            Text(
                text = "By Squadron",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                squadronReports.forEach { rep ->
                    ElevatedCard(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Squadron ${rep.squadron}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${rep.averageAttendanceRate.toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (rep.averageAttendanceRate >= orgSettings.attendanceThreshold) PresentContent else AbsentContent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${rep.activeCadetsCount} Cadets",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (rep.flaggedCadetsCount > 0) {
                                Text(
                                    text = "${rep.flaggedCadetsCount} Below ${orgSettings.attendanceThreshold}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AbsentContent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Google Sheets & Excel Export Section
        item {
            Text(
                text = "Google Sheets & Export",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sheets_sync_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Google Sheet Roster",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val lastSync = if (orgSettings.googleSheetLastSynced > 0) {
                                    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(orgSettings.googleSheetLastSynced))
                                } else "Never synced"
                                Text(
                                    text = "Last synced: $lastSync",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isSheetsSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (!syncMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = syncMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val hasSyncAccess = staff?.isAdmin == true || staff?.canSyncSheets == true

                    // Buttons: Sync / Open / Download Excel / Export CSV
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onSyncSheets() },
                            enabled = hasSyncAccess && !isSheetsSyncing,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_sync_sheets")
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Sheets", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onDownloadExcel(orgSettings.googleSheetId.ifBlank { null }, null)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_download_excel")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excel (.xlsx)", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        }
                    }

                    if (!hasSyncAccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Google Sheets sync is restricted. Ask an administrator to grant you Sync Access.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { onExportCsv(null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        }

                        if (liveSheetUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(liveSheetUrl))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Sheet", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            }
                        }
                    }

                    // Admin only action: Start New Training Year
                    if (staff?.isAdmin == true) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Training Year: ${orgSettings.trainingYear}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            TextButton(onClick = { showNewYearDialog = true }) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start New Training Year", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4. Per-Cadet Breakdown Table / List
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (showFlaggedOnly) "Below ${orgSettings.attendanceThreshold}%" else "Cadet Attendance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (showFlaggedOnly) {
                    TextButton(onClick = { showFlaggedOnly = false }) {
                        Text("Show All (${activeCadets.size})", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val sqnOpts = listOf("All") + orgSettings.squadronNumbers
                sqnOpts.forEach { sqn ->
                    FilterChip(
                        selected = selectedFilterSquadron == sqn,
                        onClick = { selectedFilterSquadron = sqn },
                        label = { Text(if (sqn == "All") "All Squadrons" else "Sqn $sqn", fontSize = 12.sp) }
                    )
                }
            }
        }

        if (filteredCadetsList.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (showFlaggedOnly) "No cadets are currently below the target!" else "No cadets found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredCadetsList, key = { it.id }) { cadet ->
                val stats = getCadetStats(cadet)
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCadetForDetail = cadet }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${cadet.rank} ${cadet.lastName}, ${cadet.firstName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sqn ${cadet.squadron} • ${cadet.instrument.ifBlank { "Band" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${stats.percentage.toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (stats.isBelowThreshold) AbsentContent else PresentContent
                            )
                            Text(
                                text = "${stats.attendedCount} attended, ${stats.absentCount + stats.absentNotifiedCount} missed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Detail & Edit Dialogs
    if (selectedCadetForDetail != null) {
        val cadet = selectedCadetForDetail!!
        CadetProfileDialog(
            cadet = cadet,
            stats = getCadetStats(cadet),
            isAdmin = staff?.isAdmin == true,
            onDismiss = { selectedCadetForDetail = null },
            onEdit = { showEditDialog = true },
            onArchive = { newStatus ->
                onArchiveCadet(cadet.id, newStatus)
                selectedCadetForDetail = null
            },
            onDeletePermanently = {
                onDeleteCadetPermanently(cadet.id)
                selectedCadetForDetail = null
            }
        )
    }

    if (showEditDialog && selectedCadetForDetail != null) {
        EditCadetDialog(
            initialCadet = selectedCadetForDetail,
            squadrons = orgSettings.squadronNumbers,
            onDismiss = { showEditDialog = false },
            onSave = { updated, onComplete ->
                onUpdateCadet(updated) { success, error ->
                    onComplete(success, error)
                    if (success) {
                        showEditDialog = false
                        selectedCadetForDetail = updated
                    }
                }
            }
        )
    }

    // Start New Training Year Dialog
    if (showNewYearDialog) {
        AlertDialog(
            onDismissRequest = { showNewYearDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start New Training Year", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This archives the current Google Sheet, keeps active cadets, and sets up a clean attendance sheet for next year.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newYearInput,
                        onValueChange = { newYearInput = it },
                        label = { Text("New Training Year (e.g. 2026-2027)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStartNewYear(newYearInput.trim()) { success ->
                            if (success) {
                                Toast.makeText(context, "New training year started!", Toast.LENGTH_SHORT).show()
                                showNewYearDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Start Year", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewYearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
    isPdfGenerating: Boolean = false,
    lastPdfUpdated: Long = 0L,
    onGenerateOrUpdatePdf: () -> Unit = {},
    onOpenPdf: () -> Unit = {},
    onSharePdf: () -> Unit = {},
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
                            AnimatedCountText(
                                targetValue = overallAvg.toInt(),
                                suffix = "%",
                                style = MaterialTheme.typography.headlineLarge,
                                color = if (overallAvg >= orgSettings.attendanceThreshold) PresentContent else AbsentContent,
                                fontWeight = FontWeight.Bold
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
                    val animatedProgress by animateFloatAsState(
                        targetValue = overallAvg / 100f,
                        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                        label = "overall_progress"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
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
                            AnimatedCountText(
                                targetValue = rep.averageAttendanceRate.toInt(),
                                suffix = "%",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (rep.averageAttendanceRate >= orgSettings.attendanceThreshold) PresentContent else AbsentContent,
                                fontWeight = FontWeight.Bold
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

        // 3. Animated Attendance Trend Chart
        item {
            AnimatedAttendanceTrendChart(
                events = events,
                cadets = cadets,
                allRecords = allRecords,
                targetThreshold = orgSettings.attendanceThreshold
            )
        }

        // 4. Official Attendance PDF Report (Exact Table Format)
        item {
            Text(
                text = "Attendance Reports & Export",
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
                    .testTag("pdf_report_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = "PDF Document",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Official Attendance PDF",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val lastPdfStr = if (lastPdfUpdated > 0) {
                                    "Updated: " + SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(lastPdfUpdated))
                                } else {
                                    "Exact layout (Training Nights & Band Practices)"
                                }
                                Text(
                                    text = lastPdfStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isPdfGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Generates and updates the canonical PDF in-place. Formatted with official Training Nights and Band Practices tables, attendance symbols (✔, ✖, ⚠, ⏰), and cadet details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onGenerateOrUpdatePdf() },
                            enabled = !isPdfGenerating,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("btn_update_pdf")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (lastPdfUpdated > 0) "Update PDF" else "Create PDF", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onOpenPdf() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_view_pdf")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onSharePdf() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_share_pdf")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 5. Google Sheets & Excel Export Section
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

@Composable
fun AnimatedCountText(
    targetValue: Int,
    suffix: String = "%",
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Bold,
    modifier: Modifier = Modifier
) {
    val animVal = remember { Animatable(0f) }
    LaunchedEffect(targetValue) {
        animVal.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }
    Text(
        text = "${animVal.value.toInt()}$suffix",
        style = style,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}

data class EventTrendData(
    val eventId: String,
    val eventName: String,
    val dateStr: String,
    val overallPercentage: Float,
    val squadronRates: Map<String, Float>
)

@Composable
fun AnimatedAttendanceTrendChart(
    events: List<BandEvent>,
    cadets: List<Cadet>,
    allRecords: List<AttendanceRecord>,
    targetThreshold: Int,
    modifier: Modifier = Modifier
) {
    val activeCadets = remember(cadets) { cadets.filter { it.status == Cadet.STATUS_ACTIVE } }
    val availableSquadrons = remember(activeCadets) {
        activeCadets.map { it.squadron.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }
    var selectedFilter by remember { mutableStateOf("Overall") }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    // Prepare chronological trend data from events and allRecords
    val trendData = remember(events, activeCadets, allRecords) {
        val sortedEvents = events.filter { !it.isCancelled }.sortedBy { it.date }
        sortedEvents.mapNotNull { event ->
            val eventRecords = allRecords.filter { it.eventId == event.id }
            val activeRecordCadetIds = activeCadets.map { it.id }.toSet()
            val relevantRecords = eventRecords.filter { it.cadetId in activeRecordCadetIds }
            
            if (relevantRecords.isEmpty()) {
                null
            } else {
                val presentCount = relevantRecords.count {
                    it.status == AttendanceRecord.STATUS_PRESENT || it.status == AttendanceRecord.STATUS_EXCUSED || it.status == AttendanceRecord.STATUS_LATE
                }
                val overallRate = (presentCount.toFloat() / relevantRecords.size.toFloat()) * 100f

                val sqnRates = mutableMapOf<String, Float>()
                availableSquadrons.forEach { sqn ->
                    val sqnCadetIds = activeCadets.filter { it.squadron.equals(sqn, ignoreCase = true) }.map { it.id }.toSet()
                    val sqnRecords = relevantRecords.filter { it.cadetId in sqnCadetIds }
                    if (sqnRecords.isNotEmpty()) {
                        val sqnPresent = sqnRecords.count {
                            it.status == AttendanceRecord.STATUS_PRESENT || it.status == AttendanceRecord.STATUS_EXCUSED || it.status == AttendanceRecord.STATUS_LATE
                        }
                        sqnRates[sqn] = (sqnPresent.toFloat() / sqnRecords.size.toFloat()) * 100f
                    }
                }

                // Short date formatting
                val shortDate = try {
                    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(event.date)
                    if (parsed != null) SimpleDateFormat("MMM d", Locale.US).format(parsed) else event.date
                } catch (e: Exception) {
                    event.date
                }

                EventTrendData(
                    eventId = event.id,
                    eventName = event.title,
                    dateStr = shortDate,
                    overallPercentage = overallRate,
                    squadronRates = sqnRates
                )
            }
        }.takeLast(8) // Display most recent 8 sessions for clean mobile canvas fit
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(trendData, selectedFilter) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val thresholdColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = surfaceColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .background(primaryColor.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Attendance Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            text = "Session-by-session performance",
                            style = MaterialTheme.typography.bodySmall,
                            color = labelColor
                        )
                    }
                }

                Surface(
                    color = primaryColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Target: $targetThreshold%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips (Overall vs specific squadrons)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = selectedFilter == "Overall",
                    onClick = {
                        selectedFilter = "Overall"
                        selectedPointIndex = null
                    },
                    label = { Text("Overall", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primaryColor,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                availableSquadrons.forEach { sqn ->
                    FilterChip(
                        selected = selectedFilter == sqn,
                        onClick = {
                            selectedFilter = sqn
                            selectedPointIndex = null
                        },
                        label = { Text("Sqn $sqn", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryColor,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (trendData.size < 2) {
                // Empty or single session state
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (trendData.isEmpty()) "No recorded event sessions yet" else "1 session recorded (${trendData.first().dateStr})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Record 2 or more attendance sessions to see live trend curves.",
                            style = MaterialTheme.typography.bodySmall,
                            color = labelColor
                        )
                    }
                }
            } else {
                // Extract points for current selected filter
                val activePoints = trendData.map { item ->
                    val rate = if (selectedFilter == "Overall") {
                        item.overallPercentage
                    } else {
                        item.squadronRates[selectedFilter] ?: item.overallPercentage
                    }
                    Pair(item.dateStr, rate)
                }

                // Interactive info banner if point selected
                if (selectedPointIndex != null && selectedPointIndex!! in activePoints.indices) {
                    val idx = selectedPointIndex!!
                    val pt = activePoints[idx]
                    val evName = trendData[idx].eventName
                    Surface(
                        color = primaryColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$evName (${pt.first})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryColor
                            )
                            Text(
                                text = "${pt.second.toInt()}% Attendance",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (pt.second >= targetThreshold) PresentContent else AbsentContent
                            )
                        }
                    }
                }

                // Canvas Line Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp, bottom = 24.dp, start = 8.dp, end = 8.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val count = activePoints.size
                        if (count < 2) return@Canvas

                        val stepX = width / (count - 1).toFloat()

                        // Grid lines for 25%, 50%, 75%, 100%
                        val yLevels = listOf(0f, 25f, 50f, 75f, 100f)
                        yLevels.forEach { lvl ->
                            val y = height - (lvl / 100f) * height
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Target Threshold reference dashed line
                        val thresholdY = height - (targetThreshold / 100f) * height
                        drawLine(
                            color = thresholdColor,
                            start = Offset(0f, thresholdY),
                            end = Offset(width, thresholdY),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Compute screen coordinates for data points with animation progress
                        val coords = activePoints.mapIndexed { index, pair ->
                            val x = index * stepX
                            val animatedVal = pair.second * animProgress.value
                            val y = height - (animatedVal / 100f) * height
                            Offset(x, y)
                        }

                        // Fill area under curve
                        val fillPath = Path().apply {
                            moveTo(coords.first().x, height)
                            coords.forEachIndexed { i, pt ->
                                if (i == 0) {
                                    lineTo(pt.x, pt.y)
                                } else {
                                    val prev = coords[i - 1]
                                    val cx = (prev.x + pt.x) / 2f
                                    cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                                }
                            }
                            lineTo(coords.last().x, height)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.25f * animProgress.value),
                                    primaryColor.copy(alpha = 0.02f)
                                ),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // Draw smooth line
                        val linePath = Path().apply {
                            coords.forEachIndexed { i, pt ->
                                if (i == 0) {
                                    moveTo(pt.x, pt.y)
                                } else {
                                    val prev = coords[i - 1]
                                    val cx = (prev.x + pt.x) / 2f
                                    cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                                }
                            }
                        }

                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        // Draw point markers and text labels
                        coords.forEachIndexed { i, pt ->
                            val isSelected = selectedPointIndex == i
                            val ptRate = activePoints[i].second

                            // Outer glowing ring
                            drawCircle(
                                color = if (isSelected) secondaryColor else primaryColor.copy(alpha = 0.25f),
                                radius = if (isSelected) 8.dp.toPx() else 5.dp.toPx(),
                                center = pt
                            )
                            // Inner filled dot
                            drawCircle(
                                color = if (isSelected) secondaryColor else if (ptRate >= targetThreshold) primaryColor else thresholdColor,
                                radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(),
                                center = pt
                            )

                            // Draw X-axis label (date)
                            val textLayoutResult = textMeasurer.measure(
                                text = activePoints[i].first,
                                style = TextStyle(
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) primaryColor else labelColor
                                )
                            )
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(
                                    x = (pt.x - textLayoutResult.size.width / 2f).coerceIn(0f, width - textLayoutResult.size.width),
                                    y = height + 4.dp.toPx()
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}


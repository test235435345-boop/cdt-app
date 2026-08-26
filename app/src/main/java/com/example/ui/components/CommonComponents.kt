package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SecurityRules
import com.example.model.AttendanceRecord
import com.example.model.BandEvent
import com.example.model.Cadet
import com.example.model.CadetAttendanceStats
import com.example.model.OrgSettings
import com.example.model.StaffMember
import com.example.ui.theme.AbsentContainer
import com.example.ui.theme.AbsentContent
import com.example.ui.theme.AbsentNotifiedContainer
import com.example.ui.theme.AbsentNotifiedContent
import com.example.ui.theme.ExcusedContainer
import com.example.ui.theme.ExcusedContent
import com.example.ui.theme.GoldAccentContainer
import com.example.ui.theme.GoldOnAccentContainer
import com.example.ui.theme.LateContainer
import com.example.ui.theme.LateContent
import com.example.ui.theme.PresentContainer
import com.example.ui.theme.PresentContent

@Composable
fun CadetBadgeLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_cadet_badge),
        contentDescription = "CadetTrack Logo",
        modifier = modifier.size(size)
    )
}

@Composable
fun AttendanceStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, textCol) = when (status) {
        AttendanceRecord.STATUS_PRESENT -> PresentContainer to PresentContent
        AttendanceRecord.STATUS_ABSENT -> AbsentContainer to AbsentContent
        AttendanceRecord.STATUS_ABSENT_NOTIFIED -> AbsentNotifiedContainer to AbsentNotifiedContent
        AttendanceRecord.STATUS_LATE -> LateContainer to LateContent
        AttendanceRecord.STATUS_EXCUSED -> ExcusedContainer to ExcusedContent
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = status,
            color = textCol,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun QuickAttendanceButton(
    label: String,
    isSelected: Boolean,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )
    val actualBg by animateColorAsState(
        targetValue = if (isSelected) containerColor else containerColor.copy(alpha = 0.35f),
        label = "bg"
    )
    val actualText by animateColorAsState(
        targetValue = if (isSelected) contentColor else contentColor.copy(alpha = 0.65f),
        label = "text"
    )

    Surface(
        color = actualBg,
        shape = RoundedCornerShape(14.dp),
        border = if (isSelected) BorderStroke(1.5.dp, contentColor) else null,
        modifier = modifier
            .scale(scale)
            .shadow(if (isSelected) 3.dp else 0.dp, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                color = actualText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun CadetAttendanceCard(
    cadet: Cadet,
    currentRecord: AttendanceRecord?,
    onMarkStatus: (status: String, note: String) -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = currentRecord?.status ?: AttendanceRecord.STATUS_UNMARKED

    ElevatedCard(
        shape = RoundedCornerShape(20.dp), // 20dp as per spec
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("cadet_card_${cadet.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenProfile)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Text(
                            text = cadet.rank.take(3),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "${cadet.lastName}, ${cadet.firstName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Sqn ${cadet.squadron}" + if (cadet.flight.isNotBlank()) " • Flt ${cadet.flight}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (cadet.instrument.isNotBlank()) {
                                Text(
                                    text = " • ${cadet.instrument}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                AttendanceStatusBadge(status = status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Present / Absent / Late / Excused
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickAttendanceButton(
                    label = "Present",
                    isSelected = status == AttendanceRecord.STATUS_PRESENT,
                    containerColor = PresentContainer,
                    contentColor = PresentContent,
                    onClick = { onMarkStatus(AttendanceRecord.STATUS_PRESENT, "") },
                    modifier = Modifier.weight(1f)
                )

                QuickAttendanceButton(
                    label = "Absent",
                    isSelected = status == AttendanceRecord.STATUS_ABSENT || status == AttendanceRecord.STATUS_ABSENT_NOTIFIED,
                    containerColor = if (status == AttendanceRecord.STATUS_ABSENT_NOTIFIED) AbsentNotifiedContainer else AbsentContainer,
                    contentColor = if (status == AttendanceRecord.STATUS_ABSENT_NOTIFIED) AbsentNotifiedContent else AbsentContent,
                    onClick = {
                        if (status == AttendanceRecord.STATUS_ABSENT) {
                            // Toggle to notified
                            onMarkStatus(AttendanceRecord.STATUS_ABSENT_NOTIFIED, "Notified")
                        } else {
                            onMarkStatus(AttendanceRecord.STATUS_ABSENT, "")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                QuickAttendanceButton(
                    label = "Late",
                    isSelected = status == AttendanceRecord.STATUS_LATE,
                    containerColor = LateContainer,
                    contentColor = LateContent,
                    onClick = { onMarkStatus(AttendanceRecord.STATUS_LATE, "") },
                    modifier = Modifier.weight(1f)
                )

                QuickAttendanceButton(
                    label = "Excused",
                    isSelected = status == AttendanceRecord.STATUS_EXCUSED,
                    containerColor = ExcusedContainer,
                    contentColor = ExcusedContent,
                    onClick = { onMarkStatus(AttendanceRecord.STATUS_EXCUSED, "") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Inline Notify toggle if absent
            AnimatedVisibility(visible = status == AttendanceRecord.STATUS_ABSENT || status == AttendanceRecord.STATUS_ABSENT_NOTIFIED) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(AbsentNotifiedContainer.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Parent notified",
                            tint = AbsentNotifiedContent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Parent notified ahead",
                            style = MaterialTheme.typography.bodySmall,
                            color = AbsentNotifiedContent,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Switch(
                        checked = status == AttendanceRecord.STATUS_ABSENT_NOTIFIED,
                        onCheckedChange = { isNotified ->
                            if (isNotified) {
                                onMarkStatus(AttendanceRecord.STATUS_ABSENT_NOTIFIED, "Parent notified ahead")
                            } else {
                                onMarkStatus(AttendanceRecord.STATUS_ABSENT, "")
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AbsentNotifiedContent,
                            checkedTrackColor = AbsentNotifiedContainer
                        ),
                        modifier = Modifier.scale(0.75f)
                    )
                }
            }
        }
    }
}

@Composable
fun CadetProfileDialog(
    cadet: Cadet,
    stats: CadetAttendanceStats,
    isAdmin: Boolean = false,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onArchive: (newStatus: String) -> Unit,
    onDeletePermanently: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showPermanentDeleteConfirm by remember { mutableStateOf(false) }

    if (showPermanentDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showPermanentDeleteConfirm = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete Cadet Permanently?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete ${cadet.rank} ${cadet.lastName}, ${cadet.firstName}? This will erase all cadet profile details and permanently wipe all their attendance history from the database.\n\nThis action CANNOT be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermanentDeleteConfirm = false
                        onDeletePermanently?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermanentDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "${cadet.rank} ${cadet.lastName}, ${cadet.firstName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Squadron ${cadet.squadron} • ${cadet.flight.ifBlank { "No Flight" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Cadet", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Running Attendance Metric Box
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (stats.isBelowThreshold) AbsentContainer else PresentContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Attendance Rate",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (stats.isBelowThreshold) AbsentContent else PresentContent
                                )
                                Text(
                                    text = "${stats.percentage.toInt()}%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (stats.isBelowThreshold) AbsentContent else PresentContent
                                )
                                if (stats.isBelowThreshold) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = "Below target",
                                            tint = AbsentContent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Below target",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AbsentContent,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Attended: ${stats.attendedCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Absences: ${stats.absentCount + stats.absentNotifiedCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Excused: ${stats.excusedCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                item {
                    // Band details
                    Text(
                        text = "Band Details",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Instrument: ${cadet.instrument.ifBlank { "Not assigned" }}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Appointment: ${cadet.appointment.ifBlank { "None" }}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Status: ${cadet.status}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }

                item {
                    // Cadet & Guardian Contact info
                    HorizontalDivider()
                    Text(
                        text = "Parent / Guardian Contact",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Guardian: ${cadet.parentName.ifBlank { "Not provided" }}", style = MaterialTheme.typography.bodyMedium)
                    if (cadet.parentPhone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Phone: ${cadet.parentPhone}", style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cadet.parentPhone}"))
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Default.Call, contentDescription = "Call parent", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    if (cadet.parentEmail.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Email: ${cadet.parentEmail}", style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${cadet.parentEmail}"))
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Default.Email, contentDescription = "Email parent", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    if (cadet.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Notes: ${cadet.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                item {
                    // Full attendance log
                    HorizontalDivider()
                    Text(
                        text = "Attendance History (${stats.records.size} events)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (stats.records.isEmpty()) {
                    item {
                        Text(
                            text = "No attendance recorded yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(stats.records) { (event, rec) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Column {
                                Text(
                                    text = "${event.date} • ${event.type}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                if (rec.markedBy.isNotBlank()) {
                                    Text(
                                        text = "Marked by: ${rec.markedBy}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            AttendanceStatusBadge(status = rec.status)
                        }
                    }
                }

                item {
                    // Archive action (never hard-delete)
                    HorizontalDivider()
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row {
                            if (cadet.status == Cadet.STATUS_ACTIVE) {
                                OutlinedButton(
                                    onClick = { onArchive(Cadet.STATUS_RELEASED) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AbsentContent)
                                ) {
                                    Text("Release")
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = { onArchive(Cadet.STATUS_TRANSFERRED) }
                                ) {
                                    Text("Transfer")
                                }
                            } else {
                                Button(
                                    onClick = { onArchive(Cadet.STATUS_ACTIVE) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Reactivate")
                                }
                            }
                        }
                    }
                }

                if (isAdmin && onDeletePermanently != null) {
                    item {
                        HorizontalDivider()
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permanent Deletion",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Admin only. Erases cadet & all attendance history.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { showPermanentDeleteConfirm = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete Forever")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

val rankOptions = listOf("Cdt", "LAC", "Cpl", "FCpl", "Sgt", "FSgt", "WO2", "WO1")
val appointmentOptions = listOf(
    "None",
    "Drum Major",
    "Section Leader",
    "Assistant Section Leader",
    "Quartermaster",
    "Band NCO",
    "Flight Commander",
    "Flight Sergeant"
)
val instrumentOptions = listOf(
    "None",
    "Flute",
    "Clarinet",
    "Alto Saxophone",
    "Tenor Saxophone",
    "Trumpet",
    "Trombone",
    "Euphonium",
    "Baritone",
    "Snare Drum",
    "Bass Drum",
    "Tenor Drum",
    "Cymbals",
    "Glockenspiel",
    "Bell Lyre",
    "Bugle",
    "Other"
)
val flightOptions = listOf(
    "Band",
    "Flight 1",
    "Flight 2",
    "Flight 3",
    "Flight A",
    "Flight B",
    "Recruit Flight",
    "HQ / Flag Party"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCadetDialog(
    initialCadet: Cadet? = null,
    squadrons: List<String>,
    onDismiss: () -> Unit,
    onSave: (Cadet, onComplete: (Boolean, String?) -> Unit) -> Unit
) {
    var lastName by remember { mutableStateOf(initialCadet?.lastName ?: "") }
    var firstName by remember { mutableStateOf(initialCadet?.firstName ?: "") }
    var rank by remember { mutableStateOf(initialCadet?.rank?.ifBlank { "Cdt" } ?: "Cdt") }
    var isRankDropdownExpanded by remember { mutableStateOf(false) }
    
    val availableSquadrons = remember(squadrons) {
        if (squadrons.isNotEmpty()) squadrons else listOf("1", "2")
    }
    var squadron by remember { mutableStateOf(initialCadet?.squadron?.ifBlank { availableSquadrons.first() } ?: availableSquadrons.first()) }
    var isSqnDropdownExpanded by remember { mutableStateOf(false) }

    var flight by remember { mutableStateOf(initialCadet?.flight ?: "Band") }
    var isFlightDropdownExpanded by remember { mutableStateOf(false) }

    var instrument by remember { mutableStateOf(initialCadet?.instrument?.ifBlank { "None" } ?: "None") }
    var isInstrumentDropdownExpanded by remember { mutableStateOf(false) }

    var appointment by remember { mutableStateOf(initialCadet?.appointment?.ifBlank { "None" } ?: "None") }
    var isApptDropdownExpanded by remember { mutableStateOf(false) }

    var phone by remember { mutableStateOf(initialCadet?.phone ?: "") }
    var email by remember { mutableStateOf(initialCadet?.email ?: "") }
    var parentName by remember { mutableStateOf(initialCadet?.parentName ?: "") }
    var parentPhone by remember { mutableStateOf(initialCadet?.parentPhone ?: "") }
    var parentEmail by remember { mutableStateOf(initialCadet?.parentEmail ?: "") }
    var notes by remember { mutableStateOf(initialCadet?.notes ?: "") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (initialCadet == null) "Add New Cadet" else "Edit Cadet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Inline error display
                if (!errorMessage.isNullOrBlank()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Name Order Convention: Last Name BEFORE First Name
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = {
                                lastName = it
                                errorMessage = null
                            },
                            label = { Text("Last Name *") },
                            singleLine = true,
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cadet_last_name_input")
                        )
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = {
                                firstName = it
                                errorMessage = null
                            },
                            label = { Text("First Name *") },
                            singleLine = true,
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cadet_first_name_input")
                        )
                    }
                }

                // Rank & Squadron
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = isRankDropdownExpanded && !isSaving,
                            onExpandedChange = { if (!isSaving) isRankDropdownExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = rank,
                                onValueChange = {},
                                readOnly = true,
                                enabled = !isSaving,
                                label = { Text("Rank") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRankDropdownExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isRankDropdownExpanded && !isSaving,
                                onDismissRequest = { isRankDropdownExpanded = false }
                            ) {
                                rankOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            rank = option
                                            isRankDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = isSqnDropdownExpanded && !isSaving,
                            onExpandedChange = { if (!isSaving) isSqnDropdownExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "Sqn $squadron",
                                onValueChange = {},
                                readOnly = true,
                                enabled = !isSaving,
                                label = { Text("Squadron") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSqnDropdownExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isSqnDropdownExpanded && !isSaving,
                                onDismissRequest = { isSqnDropdownExpanded = false }
                            ) {
                                availableSquadrons.forEach { sqnOpt ->
                                    DropdownMenuItem(
                                        text = { Text("Squadron $sqnOpt") },
                                        onClick = {
                                            squadron = sqnOpt
                                            isSqnDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Appointment & Flight Dropdowns
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = isApptDropdownExpanded && !isSaving,
                            onExpandedChange = { if (!isSaving) isApptDropdownExpanded = it },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = appointment.ifBlank { "None" },
                                onValueChange = {},
                                readOnly = true,
                                enabled = !isSaving,
                                label = { Text("Appointment") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isApptDropdownExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isApptDropdownExpanded && !isSaving,
                                onDismissRequest = { isApptDropdownExpanded = false }
                            ) {
                                appointmentOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            appointment = if (opt == "None") "" else opt
                                            isApptDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = isFlightDropdownExpanded && !isSaving,
                            onExpandedChange = { if (!isSaving) isFlightDropdownExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = flight.ifBlank { "Band" },
                                onValueChange = {},
                                readOnly = true,
                                enabled = !isSaving,
                                label = { Text("Flight") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFlightDropdownExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isFlightDropdownExpanded && !isSaving,
                                onDismissRequest = { isFlightDropdownExpanded = false }
                            ) {
                                flightOptions.forEach { fltOpt ->
                                    DropdownMenuItem(
                                        text = { Text(fltOpt) },
                                        onClick = {
                                            flight = fltOpt
                                            isFlightDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Instrument Dropdown (Flat, single-instrument list)
                item {
                    ExposedDropdownMenuBox(
                        expanded = isInstrumentDropdownExpanded && !isSaving,
                        onExpandedChange = { if (!isSaving) isInstrumentDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = instrument.ifBlank { "None" },
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isSaving,
                            label = { Text("Instrument") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isInstrumentDropdownExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isInstrumentDropdownExpanded && !isSaving,
                            onDismissRequest = { isInstrumentDropdownExpanded = false }
                        ) {
                            instrumentOptions.forEach { instOpt ->
                                DropdownMenuItem(
                                    text = { Text(instOpt) },
                                    onClick = {
                                        instrument = if (instOpt == "None") "" else instOpt
                                        isInstrumentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Parent / Guardian Section
                item {
                    Text(
                        text = "Parent / Guardian Contact",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = parentName,
                        onValueChange = { parentName = it },
                        label = { Text("Parent Name (optional)") },
                        singleLine = true,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = parentPhone,
                            onValueChange = { parentPhone = it },
                            label = { Text("Parent Phone") },
                            singleLine = true,
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = parentEmail,
                            onValueChange = { parentEmail = it },
                            label = { Text("Parent Email") },
                            singleLine = true,
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (medical, instrument serial, etc.)") },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedLast = lastName.trim()
                    val trimmedFirst = firstName.trim()
                    if (trimmedLast.isBlank() && trimmedFirst.isBlank()) {
                        errorMessage = "Please enter Last Name and First Name."
                        return@Button
                    }
                    isSaving = true
                    errorMessage = null
                    val toSave = (initialCadet ?: Cadet()).copy(
                        lastName = trimmedLast.ifBlank { trimmedFirst },
                        firstName = if (trimmedLast.isBlank()) "" else trimmedFirst,
                        rank = rank.trim().ifBlank { "Cdt" },
                        squadron = squadron.trim().ifBlank { availableSquadrons.first() },
                        flight = flight.trim(),
                        instrument = if (instrument == "None") "" else instrument.trim(),
                        appointment = if (appointment == "None") "" else appointment.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        parentName = parentName.trim(),
                        parentPhone = parentPhone.trim(),
                        parentEmail = parentEmail.trim(),
                        notes = notes.trim(),
                        status = initialCadet?.status ?: Cadet.STATUS_ACTIVE
                    )
                    onSave(toSave) { success, err ->
                        isSaving = false
                        if (!success) {
                            errorMessage = err ?: "Failed to save cadet to database. Please check your network connection and permissions."
                        }
                    }
                },
                enabled = !isSaving && (firstName.isNotBlank() || lastName.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_cadet_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saving...")
                } else {
                    Text("Save Cadet")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateEventDialog(
    initialEvent: BandEvent? = null,
    squadrons: List<String>,
    onDismiss: () -> Unit,
    onSave: (BandEvent) -> Unit
) {
    var title by remember { mutableStateOf(initialEvent?.title ?: "Weekly Training Night") }
    var date by remember { mutableStateOf(initialEvent?.date ?: "") }
    var startTime by remember { mutableStateOf(initialEvent?.startTime ?: "18:30") }
    var endTime by remember { mutableStateOf(initialEvent?.endTime ?: "21:00") }
    var type by remember { mutableStateOf(initialEvent?.type ?: BandEvent.TYPE_TRAINING_NIGHT) }
    var squadron by remember { mutableStateOf(initialEvent?.squadron ?: "All") }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }

    val typeOptions = listOf(
        BandEvent.TYPE_TRAINING_NIGHT,
        BandEvent.TYPE_BAND_PRACTICE,
        BandEvent.TYPE_PARADE,
        BandEvent.TYPE_SPECIAL
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (initialEvent == null) "New Event" else "Edit Event",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD) *") },
                        placeholder = { Text("2026-09-15") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    typeOptions.forEach { opt ->
                        FilterChip(
                            selected = type == opt,
                            onClick = {
                                type = opt
                                if (title.isBlank() || title == "Weekly Training Night" || title == "Cadet Band Practice") {
                                    title = if (opt == BandEvent.TYPE_TRAINING_NIGHT) "Weekly Training Night" else "Cadet Band Practice"
                                }
                            },
                            label = { Text(opt.take(8), fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && date.isNotBlank()) {
                        val toSave = (initialEvent ?: BandEvent()).copy(
                            title = title.trim(),
                            date = date.trim(),
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
                            type = type,
                            squadron = squadron,
                            location = location.trim()
                        )
                        onSave(toSave)
                    }
                },
                enabled = title.isNotBlank() && date.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditEventDialog(
    event: BandEvent,
    squadrons: List<String>,
    onDismiss: () -> Unit,
    onSave: (BandEvent) -> Unit,
    onCancelEvent: ((reason: String) -> Unit)? = null,
    onUncancelEvent: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(event.title) }
    var date by remember { mutableStateOf(event.date) }
    var startTime by remember { mutableStateOf(event.startTime) }
    var endTime by remember { mutableStateOf(event.endTime) }
    var type by remember { mutableStateOf(event.type) }
    var squadron by remember { mutableStateOf(event.squadron) }
    var location by remember { mutableStateOf(event.location) }
    var notes by remember { mutableStateOf(event.notes) }

    var showCancelReasonPrompt by remember { mutableStateOf(false) }
    var cancelReasonText by remember { mutableStateOf(event.cancellationReason.ifBlank { "Inclement weather / Facility closed" }) }

    val typeOptions = listOf(
        BandEvent.TYPE_TRAINING_NIGHT,
        BandEvent.TYPE_BAND_PRACTICE,
        BandEvent.TYPE_PARADE,
        BandEvent.TYPE_SPECIAL
    )

    if (showCancelReasonPrompt) {
        AlertDialog(
            onDismissRequest = { showCancelReasonPrompt = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Event", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Specify a cancellation reason (will be visible to all staff and excluded from attendance statistics):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = cancelReasonText,
                        onValueChange = { cancelReasonText = it },
                        label = { Text("Reason for Cancellation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelReasonPrompt = false
                        onCancelEvent?.invoke(cancelReasonText.trim().ifBlank { "Cancelled by staff" })
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Cancellation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelReasonPrompt = false }) { Text("Back") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Edit Event",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (event.isCancelled) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "CANCELLED",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Date (YYYY-MM-DD) *") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = squadron,
                            onValueChange = { squadron = it },
                            label = { Text("Squadron (or All)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Start Time") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("End Time") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        typeOptions.forEach { opt ->
                            FilterChip(
                                selected = type == opt,
                                onClick = { type = opt },
                                label = { Text(opt.take(8), fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Special Instructions") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    if (event.isCancelled) {
                        Column {
                            Text(
                                text = "Reason: ${event.cancellationReason.ifBlank { "None given" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = {
                                    onUncancelEvent?.invoke()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Re-activate / Uncancel Event")
                            }
                        }
                    } else if (onCancelEvent != null) {
                        OutlinedButton(
                            onClick = { showCancelReasonPrompt = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel This Event")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && date.isNotBlank()) {
                        val toSave = event.copy(
                            title = title.trim(),
                            date = date.trim(),
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
                            type = type,
                            squadron = squadron.trim(),
                            location = location.trim(),
                            notes = notes.trim()
                        )
                        onSave(toSave)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && date.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CancelEventDialog(
    event: BandEvent,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel Event", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Are you sure you want to cancel '${event.title}' on ${event.date}? Cancelled events will be excluded from attendance percentages.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Cancellation Reason (optional)") },
                    placeholder = { Text("e.g. Weather, school closure, holiday") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason.trim().ifBlank { "Event cancelled by staff" }) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cancel Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep Event") }
        }
    )
}

@Composable
fun SecurityRulesDialog(
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Firestore Rules", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Rules for your Firebase Console Rules tab:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(10.dp)) {
                        item {
                            Text(
                                text = SecurityRules.RULES_TEXT,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboard.setText(AnnotatedString(SecurityRules.RULES_TEXT))
                    copied = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (copied) "Copied to Clipboard!" else "Copy Rules")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ManageStaffDialog(
    staffMembers: List<StaffMember>,
    currentStaff: StaffMember?,
    onUpdateStaff: (StaffMember, isAuthorized: Boolean, isAdmin: Boolean, List<String>) -> Unit,
    onRejectStaff: ((StaffMember) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val pendingList = remember(staffMembers) { staffMembers.filter { !it.isAuthorized } }
    val approvedList = remember(staffMembers) { staffMembers.filter { it.isAuthorized } }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Staff & Approvals", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (pendingList.isNotEmpty()) {
                    item {
                        Surface(
                            color = LateContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = LateContent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pending Account Requests (${pendingList.size})",
                                    fontWeight = FontWeight.Bold,
                                    color = LateContent,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }

                    items(pendingList) { staff ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, LateContent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = staff.displayName.ifBlank { "New Staff Member" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${staff.role} • ${staff.email}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            onUpdateStaff(staff, true, false, listOf("1", "2"))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PresentContainer, contentColor = PresentContent),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Approve", style = MaterialTheme.typography.labelSmall)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onUpdateStaff(staff, true, true, listOf("1", "2"))
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+ Admin", style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (onRejectStaff != null) {
                                        OutlinedButton(
                                            onClick = { onRejectStaff(staff) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.weight(0.8f)
                                        ) {
                                            Text("Deny", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Approved Staff Members",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (approvedList.isEmpty() && pendingList.isEmpty()) {
                    item {
                        Text(
                            text = "No staff members registered yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(approvedList) { staff ->
                        StaffMemberItem(
                            staff = staff,
                            isSelf = staff.uid == currentStaff?.uid,
                            onToggleAuth = { auth ->
                                onUpdateStaff(staff, auth, staff.isAdmin, staff.authorizedSquadrons)
                            },
                            onToggleAdmin = { admin ->
                                onUpdateStaff(staff, staff.isAuthorized, admin, staff.authorizedSquadrons)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = MaterialTheme.colorScheme.primary) }
        }
    )
}

@Composable
fun StaffMemberItem(
    staff: StaffMember,
    isSelf: Boolean,
    onToggleAuth: (Boolean) -> Unit,
    onToggleAdmin: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = staff.displayName + if (isSelf) " (You)" else "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${staff.role} • ${staff.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (staff.isAuthorized) PresentContainer else AbsentContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (staff.isAuthorized) "Approved" else "Pending",
                        color = if (staff.isAuthorized) PresentContent else AbsentContent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = staff.isAuthorized,
                        onCheckedChange = { onToggleAuth(it) },
                        enabled = !isSelf // Don't allow de-authorizing oneself
                    )
                    Text("Approved", style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = staff.isAdmin,
                        onCheckedChange = { onToggleAdmin(it) },
                        enabled = !isSelf
                    )
                    Text("Admin", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ManageSyncAccessDialog(
    staffMembers: List<StaffMember>,
    currentStaff: StaffMember?,
    onToggleSyncAccess: (StaffMember, Boolean, (Boolean, String?) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var actionStatusMessage by remember { mutableStateOf<String?>(null) }
    var actionPendingStaffUid by remember { mutableStateOf<String?>(null) }

    val sortedStaff = remember(staffMembers) {
        staffMembers.sortedWith(
            compareByDescending<StaffMember> { it.isAdmin }
                .thenByDescending { it.canSyncSheets }
                .thenBy { it.displayName }
        )
    }

    AlertDialog(
        onDismissRequest = {
            if (actionPendingStaffUid == null) onDismiss()
        },
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Manage Sync Access",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grant or revoke Google Sheets Sync Access. Turning access ON automatically shares the canonical spreadsheet with the staff member as an Editor via Google Drive.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (!actionStatusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = actionStatusMessage ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (sortedStaff.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No staff members registered.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedStaff, key = { it.uid.ifBlank { it.email } }) { staff ->
                            val isSelf = staff.uid == currentStaff?.uid
                            val isAdmin = staff.isAdmin
                            val isPending = actionPendingStaffUid == staff.uid

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = staff.displayName.ifBlank { "Staff" } + if (isSelf) " (You)" else "",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isAdmin) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Admin",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${staff.rank.ifBlank { "Staff" }} • ${staff.email}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (isPending) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (isAdmin) {
                                        Surface(
                                            color = PresentContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Full Access",
                                                color = PresentContent,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        Switch(
                                            checked = staff.canSyncSheets,
                                            onCheckedChange = { newState ->
                                                actionPendingStaffUid = staff.uid
                                                actionStatusMessage = "Updating Drive & sync permissions for ${staff.displayName}..."
                                                onToggleSyncAccess(staff, newState) { success, msg ->
                                                    actionPendingStaffUid = null
                                                    actionStatusMessage = msg ?: if (success) "Updated successfully" else "Update failed"
                                                }
                                            },
                                            modifier = Modifier.testTag("toggle_sync_${staff.uid}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = actionPendingStaffUid == null
            ) {
                Text("Done", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileDialog(
    staff: StaffMember?,
    onDismiss: () -> Unit,
    onSave: (displayName: String, rank: String, role: String, (Boolean, String?) -> Unit) -> Unit,
    onUpdatePassword: ((newPass: String, (Boolean, String?) -> Unit) -> Unit)? = null,
    onSendPasswordReset: ((email: String, (Boolean, String) -> Unit) -> Unit)? = null
) {
    var displayName by remember { mutableStateOf(staff?.displayName ?: "") }
    var rank by remember { mutableStateOf(staff?.rank?.ifBlank { "Cdt" } ?: "Cdt") }
    var isRankDropdownExpanded by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf(staff?.role ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Password change fields
    var newPassword by remember { mutableStateOf("") }
    var isUpdatingPassword by remember { mutableStateOf(false) }
    var passwordMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSaving && !isUpdatingPassword) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Account & Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Email & Account Status Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Email: ${staff?.email ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Access Level: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                color = if (staff?.isAuthorized == true) PresentContainer else AbsentContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (staff?.isAdmin == true) "Administrator" else if (staff?.isAuthorized == true) "Approved Staff" else "Pending Approval",
                                    color = if (staff?.isAuthorized == true) PresentContent else AbsentContent,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = AbsentContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = AbsentContent,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (!successMessage.isNullOrBlank()) {
                    Surface(
                        color = PresentContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = successMessage ?: "",
                            color = PresentContent,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Text(
                    text = "Profile Details",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_field")
                )

                ExposedDropdownMenuBox(
                    expanded = isRankDropdownExpanded,
                    onExpandedChange = { isRankDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = rank,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rank") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRankDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                            .testTag("profile_rank_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = isRankDropdownExpanded,
                        onDismissRequest = { isRankDropdownExpanded = false }
                    ) {
                        rankOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    rank = option
                                    isRankDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role (e.g. Band Officer, Instructor)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_role_field")
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Password & Security Section
                Text(
                    text = "Account Security & Password",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!passwordMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = passwordMessage ?: "",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Account Password") },
                    placeholder = { Text("e.g. test1234") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            if (newPassword.trim().length >= 6) {
                                isUpdatingPassword = true
                                passwordMessage = null
                                onUpdatePassword?.invoke(newPassword.trim()) { success, err ->
                                    isUpdatingPassword = false
                                    if (success) {
                                        passwordMessage = "Password successfully updated to '$newPassword'!"
                                    } else {
                                        passwordMessage = "Password update failed: ${err ?: "Unknown error"}"
                                    }
                                }
                            } else {
                                passwordMessage = "Password must be at least 6 characters."
                            }
                        },
                        enabled = !isUpdatingPassword && newPassword.trim().isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isUpdatingPassword) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Update Password", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (staff?.email?.isNotBlank() == true && onSendPasswordReset != null) {
                        OutlinedButton(
                            onClick = {
                                onSendPasswordReset.invoke(staff.email) { success, msg ->
                                    passwordMessage = msg
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset Email", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (displayName.trim().isNotBlank()) {
                        isSaving = true
                        errorMessage = null
                        successMessage = null
                        onSave(displayName.trim(), rank.trim(), role.trim()) { success, err ->
                            isSaving = false
                            if (success) {
                                successMessage = "Profile updated successfully!"
                            } else {
                                errorMessage = err ?: "Failed to update profile"
                            }
                        }
                    }
                },
                enabled = !isSaving && displayName.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Profile Changes")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving && !isUpdatingPassword
            ) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgSettingsDialog(
    initialSettings: OrgSettings,
    onDismiss: () -> Unit,
    onSave: (OrgSettings) -> Unit
) {
    var orgName by remember { mutableStateOf(initialSettings.orgName) }
    var homeLocation by remember { mutableStateOf(initialSettings.homeLocation) }
    var squadronList by remember { 
        mutableStateOf(
            if (initialSettings.squadronNumbers.isNotEmpty()) initialSettings.squadronNumbers.toMutableList()
            else mutableListOf("1", "2")
        ) 
    }
    var newSquadronInput by remember { mutableStateOf("") }
    
    var trainingDay by remember { mutableStateOf(initialSettings.defaultTrainingNightDay.ifBlank { "Tuesday" }) }
    var trainingTime by remember { mutableStateOf(initialSettings.defaultTrainingNightTime.ifBlank { "18:30 - 21:00" }) }
    var bandDay by remember { mutableStateOf(initialSettings.defaultBandPracticeDay.ifBlank { "Saturday" }) }
    var bandTime by remember { mutableStateOf(initialSettings.defaultBandPracticeTime.ifBlank { "09:00 - 12:00" }) }
    var thresholdText by remember { mutableStateOf(initialSettings.attendanceThreshold.toString()) }
    var trainingYear by remember { mutableStateOf(initialSettings.trainingYear.ifBlank { "2026-2027" }) }

    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    var expandedTrainingDay by remember { mutableStateOf(false) }
    var expandedBandDay by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "Squadron & Unit Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Configure squadrons, timings, and unit details",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section: Unit Information
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Unit Information",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = orgName,
                            onValueChange = { orgName = it },
                            label = { Text("Squadron / Unit Name") },
                            placeholder = { Text("e.g. 758 Argus Squadron Band") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = homeLocation,
                            onValueChange = { homeLocation = it },
                            label = { Text("Default Training Location") },
                            placeholder = { Text("e.g. Main Training Facility") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Section: Squadron Numbers / Sub-Units Management
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Squadrons / Flights List",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Add or remove squadrons used across attendance and rosters:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Current Squadrons Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            squadronList.forEach { sqn ->
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Squadron $sqn",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove squadron $sqn",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    if (squadronList.size > 1) {
                                                        squadronList = squadronList.filter { it != sqn }.toMutableList()
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        // Add new squadron field & button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newSquadronInput,
                                onValueChange = { newSquadronInput = it },
                                label = { Text("New Squadron") },
                                placeholder = { Text("e.g. 3 or Flight A") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val trimmed = newSquadronInput.trim()
                                    if (trimmed.isNotBlank() && !squadronList.contains(trimmed)) {
                                        squadronList = (squadronList + trimmed).toMutableList()
                                        newSquadronInput = ""
                                    }
                                },
                                enabled = newSquadronInput.trim().isNotBlank(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add")
                            }
                        }

                        // Quick presets
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { squadronList = mutableListOf("1", "2", "3") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Set 1, 2, 3", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { squadronList = mutableListOf("1", "2", "3", "4") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Set 1 to 4", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Section: Schedule & Timings
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Schedule & Timings",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Training Night Day & Time
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = trainingDay,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Training Day") },
                                    trailingIcon = {
                                        IconButton(onClick = { expandedTrainingDay = true }) {
                                            Icon(Icons.Default.ExpandMore, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedTrainingDay = true }
                                )
                                DropdownMenu(
                                    expanded = expandedTrainingDay,
                                    onDismissRequest = { expandedTrainingDay = false }
                                ) {
                                    daysOfWeek.forEach { day ->
                                        DropdownMenuItem(
                                            text = { Text(day) },
                                            onClick = {
                                                trainingDay = day
                                                expandedTrainingDay = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = trainingTime,
                                onValueChange = { trainingTime = it },
                                label = { Text("Training Time") },
                                placeholder = { Text("18:30 - 21:00") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Band Practice Day & Time
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = bandDay,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Band Day") },
                                    trailingIcon = {
                                        IconButton(onClick = { expandedBandDay = true }) {
                                            Icon(Icons.Default.ExpandMore, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedBandDay = true }
                                )
                                DropdownMenu(
                                    expanded = expandedBandDay,
                                    onDismissRequest = { expandedBandDay = false }
                                ) {
                                    daysOfWeek.forEach { day ->
                                        DropdownMenuItem(
                                            text = { Text(day) },
                                            onClick = {
                                                bandDay = day
                                                expandedBandDay = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = bandTime,
                                onValueChange = { bandTime = it },
                                label = { Text("Band Time") },
                                placeholder = { Text("09:00 - 12:00") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Section: Target & Training Year
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Targets & Season",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = thresholdText,
                                onValueChange = { thresholdText = it },
                                label = { Text("Target %") },
                                placeholder = { Text("75") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = trainingYear,
                                onValueChange = { trainingYear = it },
                                label = { Text("Training Year") },
                                placeholder = { Text("2026-2027") },
                                singleLine = true,
                                modifier = Modifier.weight(1.3f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val threshold = thresholdText.toIntOrNull() ?: 75
                    val finalSquadrons = squadronList.ifEmpty { listOf("1", "2") }
                    val updated = initialSettings.copy(
                        orgName = orgName.trim(),
                        homeLocation = homeLocation.trim(),
                        squadronNumbers = finalSquadrons,
                        defaultTrainingNightDay = trainingDay.trim(),
                        defaultTrainingNightTime = trainingTime.trim(),
                        defaultBandPracticeDay = bandDay.trim(),
                        defaultBandPracticeTime = bandTime.trim(),
                        attendanceThreshold = threshold,
                        trainingYear = trainingYear.trim()
                    )
                    onSave(updated)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Squadron Details")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

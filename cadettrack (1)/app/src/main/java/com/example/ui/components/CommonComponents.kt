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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onArchive: (newStatus: String) -> Unit
) {
    val context = LocalContext.current

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
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun EditCadetDialog(
    initialCadet: Cadet? = null,
    squadrons: List<String>,
    onDismiss: () -> Unit,
    onSave: (Cadet) -> Unit
) {
    var lastName by remember { mutableStateOf(initialCadet?.lastName ?: "") }
    var firstName by remember { mutableStateOf(initialCadet?.firstName ?: "") }
    var rank by remember { mutableStateOf(initialCadet?.rank ?: "Cdt") }
    var squadron by remember { mutableStateOf(initialCadet?.squadron ?: squadrons.firstOrNull() ?: "1") }
    var flight by remember { mutableStateOf(initialCadet?.flight ?: "") }
    var instrument by remember { mutableStateOf(initialCadet?.instrument ?: "") }
    var appointment by remember { mutableStateOf(initialCadet?.appointment ?: "") }
    var phone by remember { mutableStateOf(initialCadet?.phone ?: "") }
    var email by remember { mutableStateOf(initialCadet?.email ?: "") }
    var parentName by remember { mutableStateOf(initialCadet?.parentName ?: "") }
    var parentPhone by remember { mutableStateOf(initialCadet?.parentPhone ?: "") }
    var parentEmail by remember { mutableStateOf(initialCadet?.parentEmail ?: "") }
    var notes by remember { mutableStateOf(initialCadet?.notes ?: "") }

    val rankOptions = listOf("Cdt", "LAC", "Cpl", "FCpl", "Sgt", "FSgt", "WO2", "WO1")

    AlertDialog(
        onDismissRequest = onDismiss,
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last Name *") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("First Name *") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rank,
                            onValueChange = { rank = it },
                            label = { Text("Rank (e.g. Cdt, Sgt)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = squadron,
                            onValueChange = { squadron = it },
                            label = { Text("Squadron #") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = flight,
                            onValueChange = { flight = it },
                            label = { Text("Flight (e.g. A, B, Band)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = instrument,
                            onValueChange = { instrument = it },
                            label = { Text("Instrument") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = appointment,
                        onValueChange = { appointment = it },
                        label = { Text("Appointment (e.g. Drum Major, Section Leader)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(
                        text = "Parent / Guardian Info",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = parentName,
                        onValueChange = { parentName = it },
                        label = { Text("Parent / Guardian Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = parentPhone,
                            onValueChange = { parentPhone = it },
                            label = { Text("Parent Phone") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = parentEmail,
                            onValueChange = { parentEmail = it },
                            label = { Text("Parent Email") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (medical, instrument serial, etc.)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (lastName.isNotBlank() && firstName.isNotBlank()) {
                        val toSave = (initialCadet ?: Cadet()).copy(
                            lastName = lastName.trim(),
                            firstName = firstName.trim(),
                            rank = rank.trim().ifBlank { "Cdt" },
                            squadron = squadron.trim(),
                            flight = flight.trim(),
                            instrument = instrument.trim(),
                            appointment = appointment.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            parentName = parentName.trim(),
                            parentPhone = parentPhone.trim(),
                            parentEmail = parentEmail.trim(),
                            notes = notes.trim(),
                            status = initialCadet?.status ?: Cadet.STATUS_ACTIVE
                        )
                        onSave(toSave)
                    }
                },
                enabled = lastName.isNotBlank() && firstName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Cadet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Staff", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (staffMembers.isEmpty()) {
                    item {
                        Text(
                            text = "No staff members registered yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(staffMembers) { staff ->
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

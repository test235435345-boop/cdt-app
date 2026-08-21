package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttendanceRecord
import com.example.model.BandEvent
import com.example.model.Cadet
import com.example.model.CadetAttendanceStats
import com.example.model.OrgSettings
import com.example.ui.components.CadetAttendanceCard
import com.example.ui.components.CadetProfileDialog
import com.example.ui.components.CreateEventDialog
import com.example.ui.components.EditCadetDialog
import com.example.ui.theme.AbsentContainer
import com.example.ui.theme.AbsentContent
import com.example.ui.theme.PresentContainer
import com.example.ui.theme.PresentContent
import com.example.viewmodel.AttendanceUiState
import kotlinx.coroutines.launch

@Composable
fun AttendanceScreen(
    orgSettings: OrgSettings,
    cadets: List<Cadet>,
    events: List<BandEvent>,
    allRecords: List<AttendanceRecord>,
    uiState: AttendanceUiState,
    undoSnapshot: List<AttendanceRecord>?,
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
    onUpdateCadet: (Cadet) -> Unit,
    onArchiveCadet: (cadetId: String, status: String) -> Unit,
    getCadetStats: (Cadet) -> CadetAttendanceStats
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateEventDialog by remember { mutableStateOf(false) }
    var selectedCadetForProfile by remember { mutableStateOf<Cadet?>(null) }
    var showEditCadetDialog by remember { mutableStateOf(false) }

    // Auto-select nearest upcoming event if none selected
    LaunchedEffect(events) {
        if (uiState.selectedEvent == null && events.isNotEmpty()) {
            val nonCancelled = events.filter { !it.isCancelled }
            val nearest = nonCancelled.firstOrNull() ?: events.first()
            onSelectEvent(nearest)
        }
    }

    // Filter active cadets
    val activeCadets = cadets.filter { it.status == Cadet.STATUS_ACTIVE }

    // Records for current event
    val currentEventRecords = if (uiState.selectedEvent != null) {
        allRecords.filter { it.eventId == uiState.selectedEvent.id }
    } else emptyList()

    val recordsMap = currentEventRecords.associateBy { it.cadetId }

    // Filtered cadets for list display
    val displayedCadets = activeCadets.filter { cadet ->
        val matchesSquadron = uiState.filterSquadron == "All" || cadet.squadron == uiState.filterSquadron
        val matchesSearch = uiState.searchQuery.isBlank() ||
                cadet.lastName.contains(uiState.searchQuery, ignoreCase = true) ||
                cadet.firstName.contains(uiState.searchQuery, ignoreCase = true) ||
                cadet.instrument.contains(uiState.searchQuery, ignoreCase = true) ||
                cadet.flight.contains(uiState.searchQuery, ignoreCase = true)

        val record = recordsMap[cadet.id]
        val status = record?.status ?: AttendanceRecord.STATUS_UNMARKED
        val matchesStatus = when (uiState.filterStatus) {
            "All" -> true
            "Unmarked" -> status == AttendanceRecord.STATUS_UNMARKED
            "Present" -> status == AttendanceRecord.STATUS_PRESENT
            "Absent" -> status == AttendanceRecord.STATUS_ABSENT || status == AttendanceRecord.STATUS_ABSENT_NOTIFIED
            "Late" -> status == AttendanceRecord.STATUS_LATE
            "Excused" -> status == AttendanceRecord.STATUS_EXCUSED
            else -> true
        }

        matchesSquadron && matchesSearch && matchesStatus
    }.sortedWith(compareBy({ it.squadron }, { it.lastName }, { it.firstName }))

    // Stats for progress bar
    val totalActiveCount = activeCadets.size
    val markedCount = activeCadets.count { recordsMap.containsKey(it.id) && recordsMap[it.id]?.status != AttendanceRecord.STATUS_UNMARKED }
    val progressPct = if (totalActiveCount > 0) markedCount.toFloat() / totalActiveCount else 0f

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateEventDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_create_event")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Event")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Event Selector Carousel / Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Dates & Events",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                TextButton(onClick = { showCreateEventDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Event", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }

            if (events.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "No events scheduled yet. Tap '+ New Event' to add one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(events) { event ->
                        val isSelected = uiState.selectedEvent?.id == event.id
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                            modifier = Modifier
                                .clickable { onSelectEvent(event) }
                                .testTag("event_item_${event.id}")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = event.date,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                    )
                                    if (event.isCancelled) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "(Cancelled)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AbsentContent
                                        )
                                    }
                                }
                                Text(
                                    text = event.type,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Active Selected Event Details & Progress Header
            if (uiState.selectedEvent != null) {
                val currentEvent = uiState.selectedEvent
                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentEvent.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${currentEvent.date} • ${currentEvent.startTime} - ${currentEvent.endTime} • ${currentEvent.location.ifBlank { "Home Unit" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!currentEvent.isCancelled) {
                                TextButton(
                                    onClick = {
                                        onCancelEvent(currentEvent.id, "Cancelled by staff")
                                    }
                                ) {
                                    Text("Cancel Event", color = AbsentContent, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Bar & Mark All Present Shortcut
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Attendance Progress",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "$markedCount of $totalActiveCount marked",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progressPct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    onMarkAllPresent(activeCadets, currentEventRecords) {
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "All cadets marked Present",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                onUndoMarkAll()
                                            } else {
                                                onClearUndoSnapshot()
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PresentContainer),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_mark_all_present")
                            ) {
                                Text("Mark All Present", color = PresentContent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Search & Filter Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search by name, instrument, or flight...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("attendance_search_input")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Filter Chips (Squadron + Status)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val sqnOptions = listOf("All") + orgSettings.squadronNumbers
                sqnOptions.forEach { sqn ->
                    FilterChip(
                        selected = uiState.filterSquadron == sqn,
                        onClick = { onFilterSquadronChange(sqn) },
                        label = { Text(if (sqn == "All") "All Squadrons" else "Sqn $sqn", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                val statusOptions = listOf("All", "Unmarked", "Present", "Absent", "Late", "Excused")
                statusOptions.forEach { st ->
                    FilterChip(
                        selected = uiState.filterStatus == st,
                        onClick = { onFilterStatusChange(st) },
                        label = { Text(st, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Cadets Attendance List
            if (activeCadets.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "No active cadets yet. Go to the Cadets tab to add some.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (displayedCadets.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "No cadets found with current filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedCadets, key = { it.id }) { cadet ->
                        CadetAttendanceCard(
                            cadet = cadet,
                            currentRecord = recordsMap[cadet.id],
                            onMarkStatus = { status, note ->
                                onMarkStatus(cadet.id, status, note)
                            },
                            onOpenProfile = {
                                selectedCadetForProfile = cadet
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateEventDialog) {
        CreateEventDialog(
            squadrons = orgSettings.squadronNumbers,
            onDismiss = { showCreateEventDialog = false },
            onSave = { newEvent ->
                onCreateEvent(newEvent)
                showCreateEventDialog = false
            }
        )
    }

    if (selectedCadetForProfile != null) {
        val cadet = selectedCadetForProfile!!
        CadetProfileDialog(
            cadet = cadet,
            stats = getCadetStats(cadet),
            onDismiss = { selectedCadetForProfile = null },
            onEdit = {
                showEditCadetDialog = true
            },
            onArchive = { newStatus ->
                onArchiveCadet(cadet.id, newStatus)
                selectedCadetForProfile = null
            }
        )
    }

    if (showEditCadetDialog && selectedCadetForProfile != null) {
        EditCadetDialog(
            initialCadet = selectedCadetForProfile,
            squadrons = orgSettings.squadronNumbers,
            onDismiss = { showEditCadetDialog = false },
            onSave = { updated ->
                onUpdateCadet(updated)
                showEditCadetDialog = false
                selectedCadetForProfile = updated
            }
        )
    }
}

package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.model.Cadet
import com.example.model.CadetAttendanceStats
import com.example.model.OrgSettings
import com.example.ui.components.CadetProfileDialog
import com.example.ui.components.EditCadetDialog
import com.example.ui.theme.AbsentContainer
import com.example.ui.theme.AbsentContent
import com.example.ui.theme.PresentContainer
import com.example.ui.theme.PresentContent

@Composable
fun CadetsScreen(
    orgSettings: OrgSettings,
    cadets: List<Cadet>,
    isAdmin: Boolean = false,
    onAddCadet: (Cadet, (Boolean, String?) -> Unit) -> Unit,
    onUpdateCadet: (Cadet, (Boolean, String?) -> Unit) -> Unit,
    onArchiveCadet: (cadetId: String, status: String) -> Unit,
    onDeleteCadetPermanently: (cadetId: String) -> Unit = {},
    onBulkImportCsv: (csvText: String, (Int, String?) -> Unit) -> Unit,
    getCadetStats: (Cadet) -> CadetAttendanceStats
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Archived
    var searchQuery by remember { mutableStateOf("") }
    var selectedSquadron by remember { mutableStateOf("All") }

    var showAddCadetDialog by remember { mutableStateOf(false) }
    var showCsvImportDialog by remember { mutableStateOf(false) }
    var csvText by remember { mutableStateOf("") }
    var selectedCadetForProfile by remember { mutableStateOf<Cadet?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val activeList = cadets.filter { it.status == Cadet.STATUS_ACTIVE }
    val archivedList = cadets.filter { it.status != Cadet.STATUS_ACTIVE }
    val targetList = if (selectedTab == 0) activeList else archivedList

    val filteredCadets = targetList.filter { cadet ->
        val matchesSqn = selectedSquadron == "All" || cadet.squadron == selectedSquadron
        val matchesSearch = searchQuery.isBlank() ||
                cadet.lastName.contains(searchQuery, ignoreCase = true) ||
                cadet.firstName.contains(searchQuery, ignoreCase = true) ||
                cadet.instrument.contains(searchQuery, ignoreCase = true) ||
                cadet.flight.contains(searchQuery, ignoreCase = true)
        matchesSqn && matchesSearch
    }.sortedWith(compareBy({ it.squadron }, { it.lastName }, { it.firstName }))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Header & Action Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Cadet Roster",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${activeList.size} Active • ${archivedList.size} Archived",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = { showCsvImportDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import CSV", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active / Archived Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Active (${activeList.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Archived (${archivedList.size})", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, rank, instrument, or flight...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cadets_search_input")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Squadron filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val sqnOptions = listOf("All") + orgSettings.squadronNumbers
                sqnOptions.forEach { sqn ->
                    FilterChip(
                        selected = selectedSquadron == sqn,
                        onClick = { selectedSquadron = sqn },
                        label = { Text(if (sqn == "All") "All Squadrons" else "Squadron $sqn", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cadets List
            if (filteredCadets.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = if (cadets.isEmpty()) "No cadets added yet. Tap '+' or 'Import CSV' to add some." else "No cadets match current filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredCadets, key = { it.id }) { cadet ->
                        val stats = getCadetStats(cadet)
                        ElevatedCard(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .clickable { selectedCadetForProfile = cadet }
                                .testTag("cadet_roster_item_${cadet.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                                    ) {
                                        Text(
                                            text = cadet.rank.take(3),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

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

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        color = if (stats.isBelowThreshold) AbsentContainer else PresentContainer,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            if (stats.isBelowThreshold) {
                                                Icon(
                                                    Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = AbsentContent,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                            }
                                            Text(
                                                text = "${stats.percentage.toInt()}%",
                                                color = if (stats.isBelowThreshold) AbsentContent else PresentContent,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${stats.attendedCount}/${stats.totalEvents} attended",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddCadetDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_add_cadet")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Cadet")
        }
    }

    // Add Cadet Dialog
    if (showAddCadetDialog) {
        EditCadetDialog(
            squadrons = orgSettings.squadronNumbers,
            onDismiss = { showAddCadetDialog = false },
            onSave = { newCadet, onComplete ->
                onAddCadet(newCadet) { success, error ->
                    onComplete(success, error)
                    if (success) {
                        showAddCadetDialog = false
                        Toast.makeText(context, "Added cadet ${newCadet.displayName}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Profile Detail Dialog
    if (selectedCadetForProfile != null) {
        val cadet = selectedCadetForProfile!!
        CadetProfileDialog(
            cadet = cadet,
            stats = getCadetStats(cadet),
            isAdmin = isAdmin,
            onDismiss = { selectedCadetForProfile = null },
            onEdit = { showEditDialog = true },
            onArchive = { newStatus ->
                onArchiveCadet(cadet.id, newStatus)
                selectedCadetForProfile = null
                Toast.makeText(context, "Cadet status set to $newStatus", Toast.LENGTH_SHORT).show()
            },
            onDeletePermanently = {
                onDeleteCadetPermanently(cadet.id)
                selectedCadetForProfile = null
                Toast.makeText(context, "Cadet deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Edit Cadet Dialog
    if (showEditDialog && selectedCadetForProfile != null) {
        EditCadetDialog(
            initialCadet = selectedCadetForProfile,
            squadrons = orgSettings.squadronNumbers,
            onDismiss = { showEditDialog = false },
            onSave = { updated, onComplete ->
                onUpdateCadet(updated) { success, error ->
                    onComplete(success, error)
                    if (success) {
                        showEditDialog = false
                        selectedCadetForProfile = updated
                        Toast.makeText(context, "Updated cadet details", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Bulk Import CSV Dialog
    if (showCsvImportDialog) {
        val defaultSqn = orgSettings.squadronNumbers.firstOrNull() ?: "1"
        AlertDialog(
            onDismissRequest = { showCsvImportDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Cadets (CSV / Excel)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste text from Excel or CSV. You can include headers like 'First Name', 'Last Name', 'Rank', 'Squadron', 'Flight', 'Instrument', 'Appointment'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Or paste simple name lists:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = {
                                csvText = """Last Name,First Name,Rank,Squadron,Flight,Instrument,Appointment
Smith,Alex,Sgt,$defaultSqn,Band,Snare Drum,Drum Major
Johnson,Maya,Cpl,$defaultSqn,Band,Trumpet,Section Leader
Brown,Lucas,Cdt,$defaultSqn,A Flight,None,None
Davis,Emma,FCpl,$defaultSqn,Band,Flute,Assistant Section Leader"""
                            }
                        ) {
                            Text("Paste Sample", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = csvText,
                        onValueChange = { csvText = it },
                        placeholder = { Text("Last Name, First Name, Rank, Squadron, Flight, Instrument, Appointment\nSmith, Alex, Sgt, $defaultSqn, Band, Snare Drum, Drum Major\nJohnson, Maya, Cpl, $defaultSqn, Band, Trumpet, Section Leader") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .testTag("csv_import_textarea")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBulkImportCsv(csvText) { count, err ->
                            if (err != null) {
                                Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Successfully imported $count cadets!", Toast.LENGTH_SHORT).show()
                                showCsvImportDialog = false
                                csvText = ""
                            }
                        }
                    },
                    enabled = csvText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_csv_import_button")
                ) {
                    Text("Import Cadets", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvImportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

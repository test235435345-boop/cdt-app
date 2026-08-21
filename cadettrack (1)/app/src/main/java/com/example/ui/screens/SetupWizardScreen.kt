package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.model.OrgSettings
import com.example.ui.components.CadetBadgeLogo
import com.example.ui.components.EditCadetDialog
import com.example.ui.theme.PresentContainer
import com.example.ui.theme.PresentContent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SetupWizardScreen(
    onCompleteSetup: (OrgSettings, List<Cadet>, startDate: String) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) } // 1, 2, 3

    // Step 1: Org Info
    var orgName by remember { mutableStateOf("") }
    var squadronsText by remember { mutableStateOf("101, 202") }
    var homeLocation by remember { mutableStateOf("") }
    var trainingNightDay by remember { mutableStateOf("Tuesday") }
    var trainingNightTime by remember { mutableStateOf("18:30 - 21:00") }
    var bandPracticeDay by remember { mutableStateOf("Thursday") }
    var bandPracticeTime by remember { mutableStateOf("18:30 - 21:00") }
    var thresholdText by remember { mutableStateOf("75") }

    // Step 2: Cadets Roster (starts strictly empty, no fake data)
    val manualCadets = remember { mutableStateOf(listOf<Cadet>()) }
    var showAddCadetDialog by remember { mutableStateOf(false) }
    var showCsvImportDialog by remember { mutableStateOf(false) }
    var csvInputText by remember { mutableStateOf("") }

    // Step 3: Training Year
    val defaultToday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    var trainingYearLabel by remember { mutableStateOf("2026-2027") }
    var startDateStr by remember { mutableStateOf(defaultToday) }

    var isSubmitting by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CadetBadgeLogo(size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Band Setup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Step $currentStep of 3",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { currentStep / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Body based on Step
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentStep) {
                    1 -> {
                        // Step 1: Org Info
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Band Details & Schedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Enter your band details and regular practice times.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = orgName,
                                onValueChange = { orgName = it },
                                label = { Text("Band / Unit Name *") },
                                placeholder = { Text("e.g. 101/202 Combined Band") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wizard_org_name")
                            )

                            OutlinedTextField(
                                value = squadronsText,
                                onValueChange = { squadronsText = it },
                                label = { Text("Squadron Numbers (comma-separated)") },
                                placeholder = { Text("e.g. 101, 202") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = homeLocation,
                                onValueChange = { homeLocation = it },
                                label = { Text("Location") },
                                placeholder = { Text("e.g. Main Hall") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = trainingNightDay,
                                    onValueChange = { trainingNightDay = it },
                                    label = { Text("Training Night Day") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = trainingNightTime,
                                    onValueChange = { trainingNightTime = it },
                                    label = { Text("Training Time") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = bandPracticeDay,
                                    onValueChange = { bandDay -> bandPracticeDay = bandDay },
                                    label = { Text("Band Practice Day") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = bandPracticeTime,
                                    onValueChange = { bandTime -> bandPracticeTime = bandTime },
                                    label = { Text("Practice Time") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = thresholdText,
                                onValueChange = { thresholdText = it },
                                label = { Text("Attendance Target Threshold (%)") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    2 -> {
                        // Step 2: Add Cadets (starts empty, side-by-side Add manually and Bulk import CSV)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Cadet Roster (Optional)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Add cadets now or add them later whenever you want.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { showAddCadetDialog = true },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wizard_add_cadet_btn")
                                 ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Cadet")
                                }

                                OutlinedButton(
                                    onClick = { showCsvImportDialog = true },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wizard_import_csv_btn")
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import CSV", color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                if (manualCadets.value.isEmpty()) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(20.dp)
                                    ) {
                                        Text(
                                            text = "No cadets added yet. You can add them now or continue and add them later.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                                        items(manualCadets.value) { c ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                                            ) {
                                                Text(
                                                    text = "${c.rank} ${c.lastName}, ${c.firstName} (Sqn ${c.squadron})",
                                                    fontWeight = FontWeight.Medium,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                if (c.instrument.isNotBlank()) {
                                                    Text(text = c.instrument, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Step 3: Training Year & Event Generation
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Training Year Dates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Set your start date to automatically generate this year's practice dates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = trainingYearLabel,
                                onValueChange = { trainingYearLabel = it },
                                label = { Text("Training Year Label") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = startDateStr,
                                onValueChange = { startDateStr = it },
                                label = { Text("Start Date (YYYY-MM-DD) *") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Surface(
                                color = PresentContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Schedule Preview",
                                        fontWeight = FontWeight.Bold,
                                        color = PresentContent,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• Training Nights: $trainingNightDay ($trainingNightTime)\n• Band Practices: $bandPracticeDay ($bandPracticeTime)\n• Location: ${homeLocation.ifBlank { "Home Location" }}",
                                        color = PresentContent,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Controls (Back / Next / Finish)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < 3) {
                            if (currentStep == 1 && orgName.isBlank()) {
                                Toast.makeText(context, "Please enter an Organization Name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            currentStep++
                        } else {
                            // Finish Setup
                            isSubmitting = true
                            val sqnList = squadronsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            val settings = OrgSettings(
                                orgName = orgName.trim(),
                                squadronNumbers = sqnList.ifEmpty { listOf("1", "2") },
                                homeLocation = homeLocation.trim(),
                                defaultTrainingNightDay = trainingNightDay,
                                defaultTrainingNightTime = trainingNightTime,
                                defaultBandPracticeDay = bandPracticeDay,
                                defaultBandPracticeTime = bandPracticeTime,
                                attendanceThreshold = thresholdText.toIntOrNull() ?: 75,
                                trainingYear = trainingYearLabel.trim()
                            )
                            onCompleteSetup(settings, manualCadets.value, startDateStr.trim())
                        }
                    },
                    enabled = !isSubmitting && (currentStep != 1 || orgName.isNotBlank()),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("wizard_next_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    } else {
                        Text(if (currentStep == 3) "Finish Setup" else "Next Step", color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (currentStep == 3) Icons.Default.Check else Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Add Cadet Dialog
        if (showAddCadetDialog) {
            val sqnList = squadronsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
            EditCadetDialog(
                squadrons = sqnList.ifEmpty { listOf("1", "2") },
                onDismiss = { showAddCadetDialog = false },
                onSave = { newCadet ->
                    manualCadets.value = manualCadets.value + newCadet
                    showAddCadetDialog = false
                }
            )
        }

        // Bulk CSV Dialog
        if (showCsvImportDialog) {
            AlertDialog(
                onDismissRequest = { showCsvImportDialog = false },
                shape = RoundedCornerShape(20.dp),
                title = { Text("Import Cadets (CSV)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                text = {
                    Column {
                        Text(
                            text = "Format: LastName, FirstName, Rank, Squadron, Flight, Instrument, Appointment, Phone, Email, GuardianName, GuardianPhone, GuardianEmail",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = csvInputText,
                            onValueChange = { csvInputText = it },
                            label = { Text("Paste CSV Content") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsed = mutableListOf<Cadet>()
                            val lines = csvInputText.lines().filter { it.isNotBlank() }
                            val startIdx = if (lines.isNotEmpty() && lines[0].contains("Name", ignoreCase = true)) 1 else 0
                            for (i in startIdx until lines.size) {
                                val cols = lines[i].split(",").map { it.trim().removeSurrounding("\"") }
                                if (cols.size >= 2) {
                                    parsed.add(
                                        Cadet(
                                            lastName = cols.getOrElse(0) { "" },
                                            firstName = cols.getOrElse(1) { "" },
                                            rank = cols.getOrElse(2) { "Cdt" },
                                            squadron = cols.getOrElse(3) { "1" },
                                            flight = cols.getOrElse(4) { "" },
                                            instrument = cols.getOrElse(5) { "" },
                                            appointment = cols.getOrElse(6) { "" },
                                            phone = cols.getOrElse(7) { "" },
                                            email = cols.getOrElse(8) { "" },
                                            parentName = cols.getOrElse(9) { "" },
                                            parentPhone = cols.getOrElse(10) { "" },
                                            parentEmail = cols.getOrElse(11) { "" }
                                        )
                                    )
                                }
                            }
                            manualCadets.value = manualCadets.value + parsed
                            showCsvImportDialog = false
                            csvInputText = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Parse & Add", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCsvImportDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

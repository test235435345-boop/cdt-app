package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.AttendanceRecord
import com.example.model.BandEvent
import com.example.model.Cadet
import com.example.model.OrgSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoogleSheetsSyncService(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder().build()
) {

    /**
     * Builds standard CSV representation of the attendance grid
     * Format: Cadet Name, Rank, Squadron, Flight, [Event 1 Date], [Event 2 Date]..., Attendance %
     */
    suspend fun generateAttendanceCsv(
        squadron: String?,
        cadets: List<Cadet>,
        events: List<BandEvent>,
        records: List<AttendanceRecord>
    ): File = withContext(Dispatchers.IO) {
        val filteredCadets = cadets.filter {
            it.status == Cadet.STATUS_ACTIVE && (squadron == null || squadron == "All" || it.squadron == squadron)
        }.sortedWith(compareBy({ it.squadron }, { it.lastName }, { it.firstName }))

        val sortedEvents = events.filter { !it.isCancelled }.sortedBy { it.date }
        val recordsMap = records.associateBy { "${it.eventId}_${it.cadetId}" }

        val sb = StringBuilder()
        // Header
        sb.append("Cadet ID,Last Name,First Name,Rank,Squadron,Flight,Instrument,Appointment")
        for (event in sortedEvents) {
            val dateLabel = "${event.date} (${event.type.take(4)})"
            sb.append(",\"").append(dateLabel.replace("\"", "\"\"")).append("\"")
        }
        sb.append(",Total Present,Total Events,Attendance %\n")

        // Rows
        for (cadet in filteredCadets) {
            sb.append("\"").append(cadet.id).append("\",")
            sb.append("\"").append(cadet.lastName.replace("\"", "\"\"")).append("\",")
            sb.append("\"").append(cadet.firstName.replace("\"", "\"\"")).append("\",")
            sb.append("\"").append(cadet.rank).append("\",")
            sb.append("\"").append(cadet.squadron).append("\",")
            sb.append("\"").append(cadet.flight).append("\",")
            sb.append("\"").append(cadet.instrument).append("\",")
            sb.append("\"").append(cadet.appointment).append("\"")

            var presentCount = 0
            var markedEvents = 0

            for (event in sortedEvents) {
                val rec = recordsMap["${event.id}_${cadet.id}"]
                val status = rec?.status ?: AttendanceRecord.STATUS_UNMARKED
                sb.append(",\"").append(status).append("\"")

                if (status == AttendanceRecord.STATUS_PRESENT || status == AttendanceRecord.STATUS_LATE) {
                    presentCount++
                    markedEvents++
                } else if (status == AttendanceRecord.STATUS_ABSENT || status == AttendanceRecord.STATUS_ABSENT_NOTIFIED) {
                    markedEvents++
                }
            }

            val pct = if (markedEvents > 0) ((presentCount.toFloat() / markedEvents) * 100).toInt() else 0
            sb.append(",").append(presentCount).append(",").append(markedEvents).append(",").append(pct).append("%\n")
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val filename = "CadetTrack_Attendance_${squadron ?: "AllSquadrons"}_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.csv"
        val file = File(exportDir, filename)
        FileOutputStream(file).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
        file
    }

    /**
     * Downloads/Exports live Google Sheet directly as genuine Microsoft Excel (.xlsx) file
     * using the Google Drive / Sheets export endpoint:
     * GET https://docs.google.com/spreadsheets/d/{spreadsheetId}/export?format=xlsx
     */
    suspend fun downloadExcelFromGoogleSheet(
        spreadsheetId: String,
        authToken: String?
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportUrl = "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=xlsx"
            val requestBuilder = Request.Builder().url(exportUrl)
            if (!authToken.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to export Excel from Google Sheets (HTTP ${response.code})"))
            }

            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, "Cadet_Band_Attendance_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.xlsx")
            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))

            FileOutputStream(file).use { output ->
                body.byteStream().copyTo(output)
            }
            Result.success(file)
        } catch (e: Exception) {
            Log.e("SheetsSync", "downloadExcelFromGoogleSheet error", e)
            Result.failure(e)
        }
    }

    /**
     * Syncs the attendance grid to Google Sheets via Google Sheets API v4
     * Tabs: One tab per squadron
     */
    suspend fun syncToGoogleSheets(
        oauthToken: String,
        existingSheetId: String?,
        settings: OrgSettings,
        cadets: List<Cadet>,
        events: List<BandEvent>,
        records: List<AttendanceRecord>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            var sheetId = existingSheetId
            val title = "Cadet Band Attendance – ${settings.trainingYear}"

            // 1. Create sheet if none exists
            if (sheetId.isNullOrBlank()) {
                val createPayload = JSONObject().apply {
                    put("properties", JSONObject().apply { put("title", title) })
                    val sheetsArray = JSONArray()
                    for (sq in settings.squadronNumbers) {
                        sheetsArray.put(JSONObject().apply {
                            put("properties", JSONObject().apply { put("title", "Squadron $sq") })
                        })
                    }
                    if (settings.squadronNumbers.isEmpty()) {
                        sheetsArray.put(JSONObject().apply {
                            put("properties", JSONObject().apply { put("title", "Band Roster") })
                        })
                    }
                    put("sheets", sheetsArray)
                }

                val createReq = Request.Builder()
                    .url("https://sheets.googleapis.com/v4/spreadsheets")
                    .addHeader("Authorization", "Bearer $oauthToken")
                    .addHeader("Content-Type", "application/json")
                    .post(createPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val resp = client.newCall(createReq).execute()
                val bodyStr = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Google Sheets Create Failed: $bodyStr"))
                }
                val respJson = JSONObject(bodyStr)
                sheetId = respJson.getString("spreadsheetId")
            }

            // 2. Populate data for each squadron
            val squadronsToSync = if (settings.squadronNumbers.isNotEmpty()) settings.squadronNumbers else listOf("All")
            val sortedEvents = events.filter { !it.isCancelled }.sortedBy { it.date }
            val recordsMap = records.associateBy { "${it.eventId}_${it.cadetId}" }

            for (sq in squadronsToSync) {
                val tabName = if (sq == "All") "Band Roster" else "Squadron $sq"
                val sqCadets = cadets.filter {
                    it.status == Cadet.STATUS_ACTIVE && (sq == "All" || it.squadron == sq)
                }.sortedWith(compareBy({ it.lastName }, { it.firstName }))

                val valuesArray = JSONArray()
                // Header row
                val headerRow = JSONArray().apply {
                    put("Cadet Name")
                    put("Rank")
                    put("Flight")
                    put("Instrument")
                    put("Appointment")
                    for (ev in sortedEvents) {
                        put("${ev.date}\n${ev.type}")
                    }
                    put("Attended")
                    put("Total")
                    put("Attendance %")
                }
                valuesArray.put(headerRow)

                // Cadet rows
                for (cadet in sqCadets) {
                    val row = JSONArray().apply {
                        put("${cadet.lastName}, ${cadet.firstName}")
                        put(cadet.rank)
                        put(cadet.flight)
                        put(cadet.instrument)
                        put(cadet.appointment)

                        var attended = 0
                        var totalMarked = 0

                        for (ev in sortedEvents) {
                            val status = recordsMap["${ev.id}_${cadet.id}"]?.status ?: ""
                            put(status)
                            if (status == AttendanceRecord.STATUS_PRESENT || status == AttendanceRecord.STATUS_LATE) {
                                attended++
                                totalMarked++
                            } else if (status == AttendanceRecord.STATUS_ABSENT || status == AttendanceRecord.STATUS_ABSENT_NOTIFIED) {
                                totalMarked++
                            }
                        }

                        val pct = if (totalMarked > 0) "${((attended.toFloat() / totalMarked) * 100).toInt()}%" else "N/A"
                        put(attended.toString())
                        put(totalMarked.toString())
                        put(pct)
                    }
                    valuesArray.put(row)
                }

                // Push values to sheet range
                val updatePayload = JSONObject().apply {
                    put("range", "'$tabName'!A1")
                    put("majorDimension", "ROWS")
                    put("values", valuesArray)
                }

                val updateReq = Request.Builder()
                    .url("https://sheets.googleapis.com/v4/spreadsheets/$sheetId/values/'$tabName'!A1?valueInputOption=USER_ENTERED")
                    .addHeader("Authorization", "Bearer $oauthToken")
                    .addHeader("Content-Type", "application/json")
                    .put(updatePayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(updateReq).execute()
            }

            Result.success(sheetId!!)
        } catch (e: Exception) {
            Log.e("SheetsSync", "syncToGoogleSheets error", e)
            Result.failure(e)
        }
    }

    /**
     * Start New Training Year: Archives existing sheet by renaming in Google Drive,
     * and generates a fresh Sheet template with the active roster only.
     */
    suspend fun startNewTrainingYear(
        oauthToken: String,
        oldSheetId: String?,
        newYearLabel: String,
        settings: OrgSettings,
        activeCadets: List<Cadet>,
        newYearEvents: List<BandEvent>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Archive old sheet if present
            if (!oldSheetId.isNullOrBlank()) {
                val archivePayload = JSONObject().apply {
                    put("name", "ARCHIVED - Cadet Band Attendance – ${settings.trainingYear}")
                }
                val renameReq = Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files/$oldSheetId")
                    .addHeader("Authorization", "Bearer $oauthToken")
                    .addHeader("Content-Type", "application/json")
                    .patch(archivePayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(renameReq).execute()
            }

            // Create new sheet with new training year label
            val updatedSettings = settings.copy(trainingYear = newYearLabel)
            val newSheetResult = syncToGoogleSheets(
                oauthToken = oauthToken,
                existingSheetId = null,
                settings = updatedSettings,
                cadets = activeCadets,
                events = newYearEvents,
                records = emptyList() // Blank new attendance history
            )
            newSheetResult
        } catch (e: Exception) {
            Log.e("SheetsSync", "startNewTrainingYear error", e)
            Result.failure(e)
        }
    }

    fun shareFile(file: File, mimeType: String, title: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share $title").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("SheetsSync", "shareFile error", e)
        }
    }
}

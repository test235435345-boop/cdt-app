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

class GoogleAuthExpiredException(message: String = "Google OAuth token expired or unauthorized (401)") : Exception(message)

class GoogleSheetsSyncService(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder().build()
) {

    /**
     * Builds standard CSV representation of the attendance grid
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
            val dateLabel = formatSessionTag(event)
            sb.append(",\"").append(dateLabel.replace("\"", "\"\"")).append("\"")
        }
        sb.append(",Total Present,Total Sessions,Attendance %\n")

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
                val symbol = getStatusSymbol(rec?.status)
                sb.append(",\"").append(symbol).append("\"")

                val st = rec?.status
                if (st == AttendanceRecord.STATUS_PRESENT || st == AttendanceRecord.STATUS_LATE) {
                    presentCount++
                    markedEvents++
                } else if (st == AttendanceRecord.STATUS_ABSENT || st == AttendanceRecord.STATUS_ABSENT_NOTIFIED) {
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
            if (response.code == 401) {
                return@withContext Result.failure(GoogleAuthExpiredException())
            }
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
     * Syncs attendance grid to Google Sheets matching exact required structure:
     * - One tab per squadron (tab name = squadron number from orgSettings)
     * - Row 1: Legend: ✔ Present   ✖ Absent   ⚠ Absent (Notified)   ⏰ Late   — Excused
     * - Row 2: Headers (bolded, frozen): Last Name, First Name, Rank, Phone, Email, Flight, Appointment, [Sessions...], Total Present, Total Sessions, Attendance %
     * - Columns A-G frozen
     * - Active cadets only (archived/released excluded)
     * - Cell values use legend symbols
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
            var sheetId = existingSheetId?.trim()
            val title = "${settings.orgName.ifBlank { "Cadet Band" }} Attendance – ${settings.trainingYear}"

            // Determine target squadrons
            val squadronsToSync = if (settings.squadronNumbers.isNotEmpty()) {
                settings.squadronNumbers
            } else {
                listOf("1", "2")
            }

            val tabSheetIds = mutableMapOf<String, Int>()

            // 1. Create or inspect existing sheet
            if (sheetId.isNullOrBlank()) {
                val createPayload = JSONObject().apply {
                    put("properties", JSONObject().apply { put("title", title) })
                    val sheetsArray = JSONArray()
                    for (sq in squadronsToSync) {
                        sheetsArray.put(JSONObject().apply {
                            put("properties", JSONObject().apply { put("title", "Squadron $sq") })
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
                if (resp.code == 401) {
                    return@withContext Result.failure(GoogleAuthExpiredException())
                }
                val bodyStr = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Google Sheets Create Failed: $bodyStr"))
                }
                val respJson = JSONObject(bodyStr)
                sheetId = respJson.getString("spreadsheetId")

                val sheetsArr = respJson.optJSONArray("sheets")
                if (sheetsArr != null) {
                    for (i in 0 until sheetsArr.length()) {
                        val sObj = sheetsArr.getJSONObject(i).getJSONObject("properties")
                        val tabTitle = sObj.getString("title")
                        val sId = sObj.getInt("sheetId")
                        tabSheetIds[tabTitle] = sId
                    }
                }
            } else {
                // Fetch existing sheet metadata — NEVER create a new sheet if existingSheetId was provided!
                val getReq = Request.Builder()
                    .url("https://sheets.googleapis.com/v4/spreadsheets/$sheetId")
                    .addHeader("Authorization", "Bearer $oauthToken")
                    .build()

                val getResp = client.newCall(getReq).execute()
                if (getResp.code == 401) {
                    return@withContext Result.failure(GoogleAuthExpiredException())
                }
                if (getResp.code == 403) {
                    return@withContext Result.failure(Exception("Permission denied for canonical sheet ($sheetId). Please ask an admin to grant you Sync Access (Editor permission)."))
                }
                if (getResp.code == 404) {
                    return@withContext Result.failure(Exception("Canonical spreadsheet ($sheetId) was not found in Google Drive. Please verify the spreadsheet exists and is shared with your account."))
                }
                if (!getResp.isSuccessful) {
                    val getErr = getResp.body?.string() ?: ""
                    return@withContext Result.failure(Exception("Failed to access canonical sheet ($sheetId): HTTP ${getResp.code} - $getErr"))
                }

                val getBody = getResp.body?.string() ?: ""
                val getJson = JSONObject(getBody)
                val sheetsArr = getJson.optJSONArray("sheets")
                if (sheetsArr != null) {
                    for (i in 0 until sheetsArr.length()) {
                        val sObj = sheetsArr.getJSONObject(i).getJSONObject("properties")
                        val tabTitle = sObj.getString("title")
                        val sId = sObj.getInt("sheetId")
                        tabSheetIds[tabTitle] = sId
                    }
                }

                // Add missing tabs if any squadron is missing
                val missingTabs = squadronsToSync.map { "Squadron $it" }.filter { !tabSheetIds.containsKey(it) }
                if (missingTabs.isNotEmpty()) {
                    val addRequests = JSONArray()
                    for (tab in missingTabs) {
                        addRequests.put(JSONObject().apply {
                            put("addSheet", JSONObject().apply {
                                put("properties", JSONObject().apply { put("title", tab) })
                            })
                        })
                    }
                    val batchAddReq = Request.Builder()
                        .url("https://sheets.googleapis.com/v4/spreadsheets/$sheetId:batchUpdate")
                        .addHeader("Authorization", "Bearer $oauthToken")
                        .addHeader("Content-Type", "application/json")
                        .post(JSONObject().apply { put("requests", addRequests) }.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    val batchResp = client.newCall(batchAddReq).execute()
                    if (batchResp.isSuccessful) {
                        val batchBody = JSONObject(batchResp.body?.string() ?: "")
                        val replies = batchBody.optJSONArray("replies")
                        if (replies != null) {
                            for (i in 0 until replies.length()) {
                                val rObj = replies.getJSONObject(i).optJSONObject("addSheet")?.optJSONObject("properties")
                                if (rObj != null) {
                                    tabSheetIds[rObj.getString("title")] = rObj.getInt("sheetId")
                                }
                            }
                        }
                    }
                }
            }

            val nonCancelledEvents = events.filter { !it.isCancelled }.sortedBy { it.date }
            val trainingEvents = nonCancelledEvents.filter {
                it.type.equals(BandEvent.TYPE_TRAINING_NIGHT, ignoreCase = true) ||
                        it.title.contains("Training", ignoreCase = true)
            }.ifEmpty { nonCancelledEvents }

            val bandEvents = nonCancelledEvents.filter {
                it.type.equals(BandEvent.TYPE_BAND_PRACTICE, ignoreCase = true) ||
                        it.title.contains("Band", ignoreCase = true) ||
                        it.title.contains("Practice", ignoreCase = true)
            }.ifEmpty { nonCancelledEvents.filter { !trainingEvents.contains(it) } }

            val recordsMap = records.associateBy { "${it.eventId}_${it.cadetId}" }
            val batchFormatRequests = JSONArray()

            for (sq in squadronsToSync) {
                val tabName = "Squadron $sq"
                // Only active cadets (exclude archived / released)
                val sqCadets = cadets.filter {
                    it.status == Cadet.STATUS_ACTIVE && it.squadron == sq
                }.sortedWith(compareBy({ it.lastName.lowercase(Locale.ROOT) }, { it.firstName.lowercase(Locale.ROOT) }))

                val valuesArray = JSONArray()

                // --- SECTION 1: Training Nights ---
                // Row 1: Title and exact Legend
                val legendRow = JSONArray().apply {
                    put("Training Nights")
                    put("LEGEND: ✔ = PRESENT; ✖ = ABSENT; ⚠ = ABSENT BUT INFORMED (HAD LET SOMEONE KNOW); ⏰ = LATE")
                    for (i in 2..6) put("")
                    for (ev in trainingEvents) put("")
                }
                valuesArray.put(legendRow)

                // Row 2: Headers (Name, Rank, Phone, Email, BAND, Flight, Appointment, then Training Dates)
                val headerRow = JSONArray().apply {
                    put("")
                    put("")
                    put("")
                    put("")
                    put("")
                    put("")
                    put("")
                    for (ev in trainingEvents) {
                        put(formatSessionDate(ev))
                    }
                }
                valuesArray.put(headerRow)

                // Cadet Data Rows for Training Nights
                for (cadet in sqCadets) {
                    val row = JSONArray().apply {
                        val fullName = "${cadet.lastName} ${cadet.firstName}".trim()
                        put(fullName)
                        put(cadet.rank)
                        put(cadet.phone)
                        put(cadet.email)
                        put(if (cadet.squadron.isNotBlank()) "BAND ${cadet.squadron}" else "BAND")
                        put(cadet.flight.ifBlank { "0" })
                        put(cadet.appointment.ifBlank { "N/A" })

                        for (ev in trainingEvents) {
                            val rec = recordsMap["${ev.id}_${cadet.id}"]
                            val symbol = getStatusSymbol(rec?.status)
                            put(symbol)
                        }
                    }
                    valuesArray.put(row)
                }

                // --- SECTION 2: Band Practices ---
                if (bandEvents.isNotEmpty()) {
                    // Blank separator row
                    valuesArray.put(JSONArray().apply { for (i in 0..(6 + bandEvents.size)) put("") })

                    // Section Header
                    val bpTitleRow = JSONArray().apply {
                        put("Band Practices")
                        for (i in 1..(6 + bandEvents.size)) put("")
                    }
                    valuesArray.put(bpTitleRow)

                    // Band Practice Dates Header
                    val bpHeaderRow = JSONArray().apply {
                        put("")
                        put("")
                        put("")
                        put("")
                        put("")
                        put("")
                        put("")
                        for (ev in bandEvents) {
                            put(formatSessionDate(ev))
                        }
                    }
                    valuesArray.put(bpHeaderRow)

                    // Cadet Data Rows for Band Practices
                    for (cadet in sqCadets) {
                        val row = JSONArray().apply {
                            val fullName = "${cadet.lastName} ${cadet.firstName}".trim()
                            put(fullName)
                            put(cadet.rank)
                            put(cadet.phone)
                            put(cadet.email)
                            put(if (cadet.squadron.isNotBlank()) "BAND ${cadet.squadron}" else "BAND")
                            put(cadet.flight.ifBlank { "0" })
                            put(cadet.appointment.ifBlank { "N/A" })

                            for (ev in bandEvents) {
                                val rec = recordsMap["${ev.id}_${cadet.id}"]
                                val symbol = getStatusSymbol(rec?.status)
                                put(symbol)
                            }
                        }
                        valuesArray.put(row)
                    }
                }

                // Clear previous cells on tab first to replace old information in-place
                val clearReq = Request.Builder()
                    .url("https://sheets.googleapis.com/v4/spreadsheets/$sheetId/values/'$tabName'!A1:ZZ1000:clear")
                    .addHeader("Authorization", "Bearer $oauthToken")
                    .addHeader("Content-Type", "application/json")
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .build()
                val clearResp = client.newCall(clearReq).execute()
                if (clearResp.code == 401) {
                    return@withContext Result.failure(GoogleAuthExpiredException())
                }
                if (clearResp.code == 403) {
                    return@withContext Result.failure(Exception("Write permission denied for canonical sheet ($sheetId). Please ask an admin to grant you Sync Access."))
                }

                // Push new values
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

                val updateResp = client.newCall(updateReq).execute()
                if (updateResp.code == 401) {
                    return@withContext Result.failure(GoogleAuthExpiredException())
                }
                if (updateResp.code == 403) {
                    return@withContext Result.failure(Exception("Write permission denied for canonical sheet ($sheetId). Please ask an admin to grant you Sync Access."))
                }
                if (!updateResp.isSuccessful) {
                    val updateErr = updateResp.body?.string() ?: ""
                    return@withContext Result.failure(Exception("Failed to update cells on $tabName: HTTP ${updateResp.code} - $updateErr"))
                }

                val maxEvents = maxOf(trainingEvents.size, bandEvents.size, 1)
                val totalCols = 7 + maxEvents

                val tabId = tabSheetIds[tabName]
                if (tabId != null) {
                    // Freeze 2 rows and 7 columns
                    batchFormatRequests.put(JSONObject().apply {
                        put("updateSheetProperties", JSONObject().apply {
                            put("properties", JSONObject().apply {
                                put("sheetId", tabId)
                                put("gridProperties", JSONObject().apply {
                                    put("frozenRowCount", 2)
                                    put("frozenColumnCount", 7)
                                })
                            })
                            put("fields", "gridProperties.frozenRowCount,gridProperties.frozenColumnCount")
                        })
                    })

                    // Format Row 1 (Title & Legend)
                    batchFormatRequests.put(JSONObject().apply {
                        put("repeatCell", JSONObject().apply {
                            put("range", JSONObject().apply {
                                put("sheetId", tabId)
                                put("startRowIndex", 0)
                                put("endRowIndex", 1)
                                put("startColumnIndex", 0)
                                put("endColumnIndex", totalCols)
                            })
                            put("cell", JSONObject().apply {
                                put("userEnteredFormat", JSONObject().apply {
                                    put("backgroundColor", JSONObject().apply {
                                        put("red", 0.95)
                                        put("green", 0.96)
                                        put("blue", 0.98)
                                    })
                                    put("textFormat", JSONObject().apply {
                                        put("bold", true)
                                        put("fontSize", 10)
                                        put("foregroundColor", JSONObject().apply {
                                            put("red", 0.1)
                                            put("green", 0.1)
                                            put("blue", 0.1)
                                        })
                                    })
                                    put("verticalAlignment", "MIDDLE")
                                })
                            })
                            put("fields", "userEnteredFormat(backgroundColor,textFormat,verticalAlignment)")
                        })
                    })

                    // 3. Format Row 2 (Headers - Bold White on Cadet Navy)
                    batchFormatRequests.put(JSONObject().apply {
                        put("repeatCell", JSONObject().apply {
                            put("range", JSONObject().apply {
                                put("sheetId", tabId)
                                put("startRowIndex", 1)
                                put("endRowIndex", 2)
                                put("startColumnIndex", 0)
                                put("endColumnIndex", totalCols)
                            })
                            put("cell", JSONObject().apply {
                                put("userEnteredFormat", JSONObject().apply {
                                    put("backgroundColor", JSONObject().apply {
                                        put("red", 0.10)
                                        put("green", 0.21)
                                        put("blue", 0.36)
                                    })
                                    put("textFormat", JSONObject().apply {
                                        put("bold", true)
                                        put("fontSize", 10)
                                        put("foregroundColor", JSONObject().apply {
                                            put("red", 1.0)
                                            put("green", 1.0)
                                            put("blue", 1.0)
                                        })
                                    })
                                    put("verticalAlignment", "MIDDLE")
                                })
                            })
                            put("fields", "userEnteredFormat(backgroundColor,textFormat,verticalAlignment)")
                        })
                    })

                    // 4. Alternating Row Banding (Zebra striping for readability)
                    for (r in 0 until sqCadets.size) {
                        val rowIndex = 2 + r
                        val isOdd = (r % 2 == 1)
                        batchFormatRequests.put(JSONObject().apply {
                            put("repeatCell", JSONObject().apply {
                                put("range", JSONObject().apply {
                                    put("sheetId", tabId)
                                    put("startRowIndex", rowIndex)
                                    put("endRowIndex", rowIndex + 1)
                                    put("startColumnIndex", 0)
                                    put("endColumnIndex", totalCols)
                                })
                                put("cell", JSONObject().apply {
                                    put("userEnteredFormat", JSONObject().apply {
                                        put("backgroundColor", JSONObject().apply {
                                            if (isOdd) {
                                                put("red", 0.96)
                                                put("green", 0.97)
                                                put("blue", 0.99)
                                            } else {
                                                put("red", 1.0)
                                                put("green", 1.0)
                                                put("blue", 1.0)
                                            }
                                        })
                                        put("verticalAlignment", "MIDDLE")
                                    })
                                })
                                put("fields", "userEnteredFormat(backgroundColor,verticalAlignment)")
                            })
                        })
                    }

                    val totalRows = valuesArray.length()

                    // 5. Center-align attendance symbol cells
                    if (sqCadets.isNotEmpty()) {
                        batchFormatRequests.put(JSONObject().apply {
                            put("repeatCell", JSONObject().apply {
                                put("range", JSONObject().apply {
                                    put("sheetId", tabId)
                                    put("startRowIndex", 2)
                                    put("endRowIndex", totalRows)
                                    put("startColumnIndex", 7)
                                    put("endColumnIndex", totalCols)
                                })
                                put("cell", JSONObject().apply {
                                    put("userEnteredFormat", JSONObject().apply {
                                        put("horizontalAlignment", "CENTER")
                                        put("verticalAlignment", "MIDDLE")
                                    })
                                })
                                put("fields", "userEnteredFormat(horizontalAlignment,verticalAlignment)")
                            })
                        })
                    }

                    // 6. Conditional formatting rules for distinct background & text colors per attendance symbol
                    if (maxEvents > 0 && sqCadets.isNotEmpty()) {
                        val attendanceRange = JSONObject().apply {
                            put("sheetId", tabId)
                            put("startRowIndex", 2)
                            put("endRowIndex", totalRows)
                            put("startColumnIndex", 7)
                            put("endColumnIndex", totalCols)
                        }

                        // Present: ✔ (Soft Green)
                        batchFormatRequests.put(JSONObject().apply {
                            put("addConditionalFormatRule", JSONObject().apply {
                                put("rule", JSONObject().apply {
                                    put("ranges", JSONArray().put(attendanceRange))
                                    put("booleanRule", JSONObject().apply {
                                        put("condition", JSONObject().apply {
                                            put("type", "TEXT_EQ")
                                            put("values", JSONArray().put(JSONObject().apply { put("userEnteredValue", "✔") }))
                                        })
                                        put("format", JSONObject().apply {
                                            put("backgroundColor", JSONObject().apply {
                                                put("red", 0.91)
                                                put("green", 0.96)
                                                put("blue", 0.91)
                                            })
                                            put("textFormat", JSONObject().apply {
                                                put("bold", true)
                                                put("foregroundColor", JSONObject().apply {
                                                    put("red", 0.11)
                                                    put("green", 0.37)
                                                    put("blue", 0.13)
                                                })
                                            })
                                        })
                                    })
                                })
                                put("index", 0)
                            })
                        })

                        // Absent: ✖ (Soft Red)
                        batchFormatRequests.put(JSONObject().apply {
                            put("addConditionalFormatRule", JSONObject().apply {
                                put("rule", JSONObject().apply {
                                    put("ranges", JSONArray().put(attendanceRange))
                                    put("booleanRule", JSONObject().apply {
                                        put("condition", JSONObject().apply {
                                            put("type", "TEXT_EQ")
                                            put("values", JSONArray().put(JSONObject().apply { put("userEnteredValue", "✖") }))
                                        })
                                        put("format", JSONObject().apply {
                                            put("backgroundColor", JSONObject().apply {
                                                put("red", 1.0)
                                                put("green", 0.92)
                                                put("blue", 0.93)
                                            })
                                            put("textFormat", JSONObject().apply {
                                                put("bold", true)
                                                put("foregroundColor", JSONObject().apply {
                                                    put("red", 0.72)
                                                    put("green", 0.11)
                                                    put("blue", 0.11)
                                                })
                                            })
                                        })
                                    })
                                })
                                put("index", 1)
                            })
                        })

                        // Absent-Notified: ⚠ (Soft Amber)
                        batchFormatRequests.put(JSONObject().apply {
                            put("addConditionalFormatRule", JSONObject().apply {
                                put("rule", JSONObject().apply {
                                    put("ranges", JSONArray().put(attendanceRange))
                                    put("booleanRule", JSONObject().apply {
                                        put("condition", JSONObject().apply {
                                            put("type", "TEXT_EQ")
                                            put("values", JSONArray().put(JSONObject().apply { put("userEnteredValue", "⚠") }))
                                        })
                                        put("format", JSONObject().apply {
                                            put("backgroundColor", JSONObject().apply {
                                                put("red", 1.0)
                                                put("green", 0.97)
                                                put("blue", 0.88)
                                            })
                                            put("textFormat", JSONObject().apply {
                                                put("bold", true)
                                                put("foregroundColor", JSONObject().apply {
                                                    put("red", 0.90)
                                                    put("green", 0.32)
                                                    put("blue", 0.0)
                                                })
                                            })
                                        })
                                    })
                                })
                                put("index", 2)
                            })
                        })

                        // Late: ⏰ (Soft Gold/Yellow)
                        batchFormatRequests.put(JSONObject().apply {
                            put("addConditionalFormatRule", JSONObject().apply {
                                put("rule", JSONObject().apply {
                                    put("ranges", JSONArray().put(attendanceRange))
                                    put("booleanRule", JSONObject().apply {
                                        put("condition", JSONObject().apply {
                                            put("type", "TEXT_EQ")
                                            put("values", JSONArray().put(JSONObject().apply { put("userEnteredValue", "⏰") }))
                                        })
                                        put("format", JSONObject().apply {
                                            put("backgroundColor", JSONObject().apply {
                                                put("red", 1.0)
                                                put("green", 0.99)
                                                put("blue", 0.90)
                                            })
                                            put("textFormat", JSONObject().apply {
                                                put("bold", true)
                                                put("foregroundColor", JSONObject().apply {
                                                    put("red", 0.96)
                                                    put("green", 0.50)
                                                    put("blue", 0.09)
                                                })
                                            })
                                        })
                                    })
                                })
                                put("index", 3)
                            })
                        })

                        // Excused: — (Soft Slate Gray)
                        batchFormatRequests.put(JSONObject().apply {
                            put("addConditionalFormatRule", JSONObject().apply {
                                put("rule", JSONObject().apply {
                                    put("ranges", JSONArray().put(attendanceRange))
                                    put("booleanRule", JSONObject().apply {
                                        put("condition", JSONObject().apply {
                                            put("type", "TEXT_EQ")
                                            put("values", JSONArray().put(JSONObject().apply { put("userEnteredValue", "—") }))
                                        })
                                        put("format", JSONObject().apply {
                                            put("backgroundColor", JSONObject().apply {
                                                put("red", 0.92)
                                                put("green", 0.94)
                                                put("blue", 0.95)
                                            })
                                            put("textFormat", JSONObject().apply {
                                                put("bold", false)
                                                put("foregroundColor", JSONObject().apply {
                                                    put("red", 0.27)
                                                    put("green", 0.35)
                                                    put("blue", 0.39)
                                                })
                                            })
                                        })
                                    })
                                })
                                put("index", 4)
                            })
                        })
                    }

                    // 7. Explicit column pixel widths for consistent layout & readability
                    val columnWidths = mapOf(
                        0 to 135, // Last Name
                        1 to 125, // First Name
                        2 to 75,  // Rank
                        3 to 125, // Phone
                        4 to 190, // Email
                        5 to 85,  // Flight
                        6 to 130  // Appointment
                    )

                    for ((colIdx, widthPx) in columnWidths) {
                        batchFormatRequests.put(JSONObject().apply {
                            put("updateDimensionProperties", JSONObject().apply {
                                put("range", JSONObject().apply {
                                    put("sheetId", tabId)
                                    put("dimension", "COLUMNS")
                                    put("startIndex", colIdx)
                                    put("endIndex", colIdx + 1)
                                })
                                put("properties", JSONObject().apply {
                                    put("pixelSize", widthPx)
                                })
                                put("fields", "pixelSize")
                            })
                        })
                    }

                    // Set consistent width for session dates (Cols 7 .. 7 + maxEvents)
                    for (i in 0 until maxEvents) {
                        val sessionColIdx = 7 + i
                        batchFormatRequests.put(JSONObject().apply {
                            put("updateDimensionProperties", JSONObject().apply {
                                put("range", JSONObject().apply {
                                    put("sheetId", tabId)
                                    put("dimension", "COLUMNS")
                                    put("startIndex", sessionColIdx)
                                    put("endIndex", sessionColIdx + 1)
                                })
                                put("properties", JSONObject().apply {
                                    put("pixelSize", 90)
                                })
                                put("fields", "pixelSize")
                            })
                        })
                    }
                }
            }

            // Execute formatting batchUpdate
            if (batchFormatRequests.length() > 0) {
                val formatReq = Request.Builder()
                    .url("https://sheets.googleapis.com/v4/spreadsheets/$sheetId:batchUpdate")
                    .addHeader("Authorization", "Bearer $oauthToken")
                    .addHeader("Content-Type", "application/json")
                    .post(JSONObject().apply { put("requests", batchFormatRequests) }.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                val formatResp = client.newCall(formatReq).execute()
                if (formatResp.code == 401) {
                    return@withContext Result.failure(GoogleAuthExpiredException())
                }
                if (!formatResp.isSuccessful) {
                    Log.w("SheetsSync", "Formatting batch update notice: ${formatResp.code} ${formatResp.body?.string()}")
                }
            }

            Result.success(sheetId!!)
        } catch (e: Exception) {
            Log.e("SheetsSync", "syncToGoogleSheets error", e)
            Result.failure(e)
        }
    }

    /**
     * Shares canonical spreadsheet with staff email as Editor via Google Drive API
     */
    suspend fun shareSpreadsheetWithEmail(
        oauthToken: String,
        spreadsheetId: String,
        email: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (spreadsheetId.isBlank()) {
                return@withContext Result.failure(Exception("No canonical spreadsheet ID found."))
            }
            if (email.isBlank()) {
                return@withContext Result.failure(Exception("Staff member email is blank."))
            }

            val payload = JSONObject().apply {
                put("role", "writer")
                put("type", "user")
                put("emailAddress", email.trim())
            }

            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/${spreadsheetId.trim()}/permissions?sendNotificationEmail=false")
                .addHeader("Authorization", "Bearer $oauthToken")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string() ?: ""

            if (response.code == 401) {
                return@withContext Result.failure(GoogleAuthExpiredException())
            }
            if (!response.isSuccessful) {
                if (response.code == 403 || response.code == 404) {
                    return@withContext Result.failure(
                        Exception("Drive sharing requires the spreadsheet owner account to be signed in (HTTP ${response.code}).")
                    )
                }
                return@withContext Result.failure(Exception("Google Drive API sharing error (${response.code}): $bodyStr"))
            }

            Result.success("Shared as Editor with $email")
        } catch (e: Exception) {
            Log.e("SheetsSync", "shareSpreadsheetWithEmail error", e)
            Result.failure(e)
        }
    }

    /**
     * Revokes staff email's Editor access from canonical spreadsheet via Google Drive API
     */
    suspend fun revokeSpreadsheetAccess(
        oauthToken: String,
        spreadsheetId: String,
        email: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (spreadsheetId.isBlank() || email.isBlank()) {
                return@withContext Result.failure(Exception("Spreadsheet ID or email is blank."))
            }

            // 1. List permissions to locate the permission ID for email
            val listReq = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/${spreadsheetId.trim()}/permissions?fields=permissions(id,emailAddress,role)")
                .addHeader("Authorization", "Bearer $oauthToken")
                .build()

            val listResp = client.newCall(listReq).execute()
            val listBody = listResp.body?.string() ?: ""

            if (listResp.code == 401) {
                return@withContext Result.failure(GoogleAuthExpiredException())
            }
            if (!listResp.isSuccessful) {
                return@withContext Result.failure(Exception("Drive API query permissions error (${listResp.code}): $listBody"))
            }

            val listJson = JSONObject(listBody)
            val permissions = listJson.optJSONArray("permissions")
            var permIdToDelete: String? = null

            if (permissions != null) {
                for (i in 0 until permissions.length()) {
                    val p = permissions.getJSONObject(i)
                    val pEmail = p.optString("emailAddress")
                    if (pEmail.equals(email.trim(), ignoreCase = true)) {
                        permIdToDelete = p.optString("id")
                        break
                    }
                }
            }

            if (!permIdToDelete.isNullOrBlank()) {
                val delReq = Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files/${spreadsheetId.trim()}/permissions/$permIdToDelete")
                    .addHeader("Authorization", "Bearer $oauthToken")
                    .delete()
                    .build()

                val delResp = client.newCall(delReq).execute()
                if (!delResp.isSuccessful && delResp.code != 404) {
                    val delBody = delResp.body?.string() ?: ""
                    return@withContext Result.failure(Exception("Drive API revoke permission error (${delResp.code}): $delBody"))
                }
            }

            Result.success("Revoked Editor permission for $email")
        } catch (e: Exception) {
            Log.e("SheetsSync", "revokeSpreadsheetAccess error", e)
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
                    put("name", "ARCHIVED - ${settings.orgName.ifBlank { "Cadet Band" }} Attendance – ${settings.trainingYear}")
                }
                val renameReq = Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files/$oldSheetId")
                    .addHeader("Authorization", "Bearer $oauthToken")
                    .addHeader("Content-Type", "application/json")
                    .patch(archivePayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                val renameResp = client.newCall(renameReq).execute()
                if (renameResp.code == 401) {
                    return@withContext Result.failure(GoogleAuthExpiredException())
                }
            }

            // Create new sheet with new training year label
            val updatedSettings = settings.copy(trainingYear = newYearLabel)
            val newSheetResult = syncToGoogleSheets(
                oauthToken = oauthToken,
                existingSheetId = null,
                settings = updatedSettings,
                cadets = activeCadets,
                events = newYearEvents,
                records = emptyList() // Clean new attendance history
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

    private fun formatSessionDate(event: BandEvent): String {
        return try {
            val inSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outSdf = SimpleDateFormat("MMMM d", Locale.US)
            val d = inSdf.parse(event.date)
            if (d != null) outSdf.format(d) else event.date
        } catch (e: Exception) {
            event.date
        }
    }

    private fun formatSessionTag(event: BandEvent): String {
        val dateLabel = formatSessionDate(event)
        val typeTag = when (event.type) {
            BandEvent.TYPE_TRAINING_NIGHT -> "TN"
            BandEvent.TYPE_BAND_PRACTICE -> "BP"
            BandEvent.TYPE_PARADE -> "Parade"
            BandEvent.TYPE_SPECIAL -> "Special"
            else -> if (event.type.length > 4) event.type.take(3) else event.type
        }
        return "$dateLabel ($typeTag)"
    }

    private fun getStatusSymbol(status: String?): String {
        return when (status) {
            AttendanceRecord.STATUS_PRESENT -> "✔"
            AttendanceRecord.STATUS_ABSENT -> "✖"
            AttendanceRecord.STATUS_ABSENT_NOTIFIED -> "⚠"
            AttendanceRecord.STATUS_LATE -> "⏰"
            AttendanceRecord.STATUS_EXCUSED -> "N/A"
            else -> ""
        }
    }
}

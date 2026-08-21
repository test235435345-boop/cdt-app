package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.model.AttendanceRecord
import com.example.model.BandEvent
import com.example.model.Cadet
import com.example.model.ChatMessage
import com.example.model.OrgSettings
import com.example.model.StaffMember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    suspend fun generateGroundedResponse(
        userPrompt: String,
        history: List<ChatMessage>,
        settings: OrgSettings,
        staff: StaffMember,
        cadets: List<Cadet>,
        events: List<BandEvent>,
        records: List<AttendanceRecord>
    ): ChatMessage = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ChatMessage(
                sender = "gemini",
                text = "Gemini API Key is not configured. Please ensure GEMINI_API_KEY is set in AI Studio Secrets."
            )
        }

        // Build live grounded system context
        val contextBuilder = StringBuilder()
        contextBuilder.append("You are a helpful assistant for CadetTrack, a band attendance app.\n")
        contextBuilder.append("Band/Unit Name: ${settings.orgName.ifBlank { "Cadet Band" }}\n")
        contextBuilder.append("Squadrons: ${settings.squadronNumbers.joinToString(", ")}\n")
        contextBuilder.append("Attendance Target: ${settings.attendanceThreshold}%\n")
        contextBuilder.append("Current Staff Member: ${staff.displayName} (${staff.role})\n\n")

        contextBuilder.append("--- CADET ROSTER (${cadets.size} total) ---\n")
        for (c in cadets) {
            contextBuilder.append("ID: ${c.id} | Name: ${c.lastName}, ${c.firstName} | Rank: ${c.rank} | Sqn: ${c.squadron} | Flight: ${c.flight} | Instrument: ${c.instrument} | Status: ${c.status} | Guardian: ${c.parentName} (${c.parentPhone}, ${c.parentEmail})\n")
        }

        contextBuilder.append("\n--- EVENTS SCHEDULE (${events.size} total) ---\n")
        for (e in events.takeLast(30)) {
            contextBuilder.append("ID: ${e.id} | Title: ${e.title} | Date: ${e.date} | Type: ${e.type} | Cancelled: ${e.isCancelled}\n")
        }

        contextBuilder.append("\n--- ATTENDANCE SUMMARY ---\n")
        val recordsMap = records.groupBy { it.cadetId }
        for (c in cadets.filter { it.status == Cadet.STATUS_ACTIVE }) {
            val cRecs = recordsMap[c.id] ?: emptyList()
            val total = cRecs.size
            val present = cRecs.count { it.status == AttendanceRecord.STATUS_PRESENT || it.status == AttendanceRecord.STATUS_LATE }
            val absent = cRecs.count { it.status == AttendanceRecord.STATUS_ABSENT || it.status == AttendanceRecord.STATUS_ABSENT_NOTIFIED }
            val pct = if (total > 0) ((present.toFloat() / total) * 100).toInt() else 100
            contextBuilder.append("${c.rank} ${c.lastName}, ${c.firstName} (Sqn ${c.squadron}): $present/$total attended ($pct%). Absences: $absent\n")
        }

        contextBuilder.append("\nRULES:\n")
        contextBuilder.append("1. Answer questions concisely, friendly, and factually based on the data provided.\n")
        contextBuilder.append("2. When drafting a message to a parent/guardian, keep it friendly and clear. Use real guardian contact info from the cadet record if available. Sign with '${staff.displayName}, ${staff.role}'.\n")
        contextBuilder.append("3. Format comparisons cleanly, using simple tables where appropriate.\n")
        contextBuilder.append("4. Never invent fake cadets or fake contact details.\n")

        try {
            val contentsArray = JSONArray()

            // Include limited previous conversation turns for context
            val recentHistory = history.takeLast(6)
            for (msg in recentHistory) {
                val role = if (msg.sender == "user") "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                })
            }

            // Current prompt
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
            })

            val payload = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", contextBuilder.toString())))
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("topP", 0.95)
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiService", "API error: $bodyString")
                return@withContext ChatMessage(
                    sender = "gemini",
                    text = "Could not generate answer (HTTP ${response.code}). Please check network or API quota."
                )
            }

            val respJson = JSONObject(bodyString)
            val candidates = respJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: "No response generated."

            // Check if response is a guardian draft
            val isDraft = userPrompt.contains("draft", ignoreCase = true) ||
                    userPrompt.contains("guardian", ignoreCase = true) ||
                    userPrompt.contains("parent", ignoreCase = true) ||
                    userPrompt.contains("message", ignoreCase = true)

            // Extract table if markdown table exists
            val (cleanText, headers, rows) = parseMarkdownTable(rawText)

            ChatMessage(
                sender = "gemini",
                text = cleanText,
                isGuardianDraft = isDraft && rawText.length > 50,
                tableHeaders = headers,
                tableRows = rows
            )
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception calling Gemini", e)
            ChatMessage(
                sender = "gemini",
                text = "Error reaching Gemini service: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun parseMarkdownTable(text: String): Triple<String, List<String>, List<List<String>>> {
        val lines = text.lines()
        val tableLines = mutableListOf<String>()
        val nonTableLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                tableLines.add(trimmed)
            } else {
                nonTableLines.add(line)
            }
        }

        if (tableLines.size >= 3) {
            val headerCells = tableLines[0].split("|").map { it.trim() }.filter { it.isNotEmpty() }
            val rows = mutableListOf<List<String>>()
            for (i in 2 until tableLines.size) {
                val rowCells = tableLines[i].split("|").map { it.trim() }.filter { it.isNotEmpty() }
                if (rowCells.isNotEmpty()) {
                    rows.add(rowCells)
                }
            }
            val cleanText = nonTableLines.joinToString("\n").trim()
            return Triple(cleanText, headerCells, rows)
        }

        return Triple(text, emptyList(), emptyList())
    }
}

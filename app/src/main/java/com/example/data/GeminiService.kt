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

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateGroundedResponse(
        userPrompt: String,
        history: List<ChatMessage>,
        settings: OrgSettings,
        staff: StaffMember,
        cadets: List<Cadet>,
        events: List<BandEvent>,
        records: List<AttendanceRecord>,
        responseFormat: String = "smart"
    ): ChatMessage = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "UNSET" || apiKey == "DEFAULT_KEY") {
            val sampleBlocks = ChatBlockParser.parseBlocks(
                "Gemini API Key is not configured. Please ensure `GEMINI_API_KEY` is set in AI Studio Secrets."
            )
            return@withContext ChatMessage(
                sender = "gemini",
                text = "Gemini API Key is not configured. Please ensure GEMINI_API_KEY is set in AI Studio Secrets.",
                blocks = sampleBlocks
            )
        }

        // Build live grounded system context
        val contextBuilder = StringBuilder()
        contextBuilder.append("You are an intelligent, friendly AI assistant for CadetTrack, an attendance management app for cadet bands.\n")
        contextBuilder.append("Band/Unit Name: ${settings.orgName.ifBlank { "Cadet Band" }}\n")
        contextBuilder.append("Squadrons: ${settings.squadronNumbers.joinToString(", ")}\n")
        contextBuilder.append("Attendance Target Threshold: ${settings.attendanceThreshold}%\n")
        contextBuilder.append("Current Staff Member Logged In: ${staff.displayName} (${staff.role})\n\n")

        contextBuilder.append("--- CADET ROSTER (${cadets.size} cadets) ---\n")
        for (c in cadets) {
            contextBuilder.append("ID: ${c.id} | Name: ${c.lastName}, ${c.firstName} | Rank: ${c.rank} | Sqn: ${c.squadron} | Flight: ${c.flight} | Instrument: ${c.instrument} | Status: ${c.status} | Guardian: ${c.parentName} (${c.parentPhone}, ${c.parentEmail})\n")
        }

        contextBuilder.append("\n--- RECENT EVENTS (${events.takeLast(30).size} events) ---\n")
        for (e in events.takeLast(30)) {
            contextBuilder.append("ID: ${e.id} | Title: ${e.title} | Date: ${e.date} | Type: ${e.type} | Cancelled: ${e.isCancelled}\n")
        }

        contextBuilder.append("\n--- ATTENDANCE SUMMARY PER CADET ---\n")
        val recordsMap = records.groupBy { it.cadetId }
        for (c in cadets.filter { it.status == Cadet.STATUS_ACTIVE }) {
            val cRecs = recordsMap[c.id] ?: emptyList()
            val total = cRecs.size
            val present = cRecs.count { it.status == AttendanceRecord.STATUS_PRESENT || it.status == AttendanceRecord.STATUS_LATE }
            val absent = cRecs.count { it.status == AttendanceRecord.STATUS_ABSENT || it.status == AttendanceRecord.STATUS_ABSENT_NOTIFIED }
            val pct = if (total > 0) ((present.toFloat() / total) * 100).toInt() else 100
            val recentStatuses = cRecs.takeLast(5).joinToString(", ") { "${it.status} (${it.markedBy})" }
            contextBuilder.append("${c.rank} ${c.lastName}, ${c.firstName} (ID: ${c.id}, Sqn ${c.squadron}): $present/$total attended ($pct%). Absences: $absent. Recent: [$recentStatuses]\n")
        }

        contextBuilder.append("\n--- OUTPUT FORMAT INSTRUCTIONS ---\n")
        contextBuilder.append("You MUST ALWAYS respond with a JSON array of block objects `[ { \"type\": \"...\", ... } ]`.\n")
        contextBuilder.append("Do not output raw conversational text outside the JSON array.\n")
        contextBuilder.append("Available block types to choose from:\n")
        contextBuilder.append("1. `paragraph`: { \"type\": \"paragraph\", \"text\": \"Text supporting markdown like **bold**, *italic*, `code`\" }\n")
        contextBuilder.append("2. `heading`: { \"type\": \"heading\", \"text\": \"Heading Title\", \"level\": 1|2|3 }\n")
        contextBuilder.append("3. `bulleted_list`: { \"type\": \"bulleted_list\", \"items\": [\"item 1\", \"item 2\"] }\n")
        contextBuilder.append("4. `numbered_list`: { \"type\": \"numbered_list\", \"items\": [\"item 1\", \"item 2\"] }\n")
        contextBuilder.append("5. `table`: { \"type\": \"table\", \"headers\": [\"Header 1\", \"Header 2\"], \"rows\": [[\"Row1 Col1\", \"Row1 Col2\"]] }\n")
        contextBuilder.append("6. `attendance_chips`: { \"type\": \"attendance_chips\", \"title\": \"Recent Attendance\", \"cadetName\": \"John Doe\", \"cadetId\": \"123\", \"chips\": [{\"status\": \"Present\", \"label\": \"Sep 2\", \"date\": \"2026-09-02\"}, {\"status\": \"Absent\", \"label\": \"Sep 9\", \"date\": \"2026-09-09\"}] } (Valid statuses: 'Present', 'Absent', 'Absent-Notified', 'Late', 'Excused')\n")
        contextBuilder.append("7. `stat_card`: { \"type\": \"stat_card\", \"value\": \"84%\", \"label\": \"845 Squadron Attendance\", \"subtext\": \"Above target\", \"isPositive\": true|false|null }\n")
        contextBuilder.append("8. `trend_chart`: { \"type\": \"trend_chart\", \"title\": \"Attendance Trend\", \"subtitle\": \"Last 5 Sessions\", \"threshold\": 75, \"dataPoints\": [{\"label\": \"Aug 1\", \"value\": 80}, {\"label\": \"Aug 8\", \"value\": 90}] }\n")
        contextBuilder.append("9. `cadet_chip`: { \"type\": \"cadet_chip\", \"cadetId\": \"...\", \"cadetName\": \"Last, First\", \"rank\": \"Sgt\", \"squadron\": \"845\", \"info\": \"92% Attendance\" }\n")
        contextBuilder.append("10. `comparison_card`: { \"type\": \"comparison_card\", \"title\": \"Squadron Attendance Comparison\", \"metricLabel\": \"Attendance Rate\", \"leftItem\": { \"title\": \"Sqn 845\", \"value\": \"88%\", \"subtext\": \"18 Cadets\", \"isHighlighted\": true }, \"rightItem\": { \"title\": \"Sqn 746\", \"value\": \"76%\", \"subtext\": \"14 Cadets\", \"isHighlighted\": false }, \"diffText\": \"+12% higher in Sqn 845\", \"winnerSide\": \"left\" }\n")
        contextBuilder.append("11. `leaderboard`: { \"type\": \"leaderboard\", \"title\": \"Top Attendance Cadets\", \"subtitle\": \"Ranked by 2026 participation\", \"entries\": [ { \"rank\": 1, \"title\": \"Sgt Smith, John\", \"subtitle\": \"Sqn 845 • 12/12 Events\", \"score\": \"100%\", \"badge\": \"Perfect\", \"isFlagged\": false, \"cadetId\": \"...\" }, { \"rank\": 2, \"title\": \"Cpl Doe, Jane\", \"subtitle\": \"Sqn 746 • 11/12 Events\", \"score\": \"92%\", \"badge\": \"High Attendance\", \"isFlagged\": false, \"cadetId\": \"...\" } ] }\n")
        contextBuilder.append("12. `alert_banner`: { \"type\": \"alert_banner\", \"title\": \"Attendance Alert\", \"message\": \"3 cadets have fallen below the 75% training threshold\", \"severity\": \"warning|danger|success|info\", \"actionText\": \"View Flagged Cadets\" }\n")
        contextBuilder.append("13. `quick_action`: { \"type\": \"quick_action\", \"title\": \"Suggested Actions\", \"actions\": [ { \"label\": \"Take Tonight's Attendance\", \"targetScreen\": \"attendance\", \"targetParam\": \"\", \"iconName\": \"check\" }, { \"label\": \"View Roster\", \"targetScreen\": \"cadets\", \"targetParam\": \"\", \"iconName\": \"person\" }, { \"label\": \"View Reports & Sync\", \"targetScreen\": \"reports\", \"targetParam\": \"\", \"iconName\": \"chart\" } ] } (Valid targetScreens: 'attendance', 'cadets', 'reports', 'cadet_profile')\n")
        contextBuilder.append("14. `quote`: { \"type\": \"quote\", \"text\": \"...\", \"source\": \"...\" }\n")
        contextBuilder.append("15. `code_block`: { \"type\": \"code_block\", \"language\": \"csv\", \"code\": \"...\" }\n")
        contextBuilder.append("16. `divider`: { \"type\": \"divider\" }\n\n")

        when (responseFormat) {
            "summary" -> {
                contextBuilder.append("USER FORMAT PREFERENCE: SHORT SUMMARY.\n")
                contextBuilder.append("Provide a direct, concise 1-2 sentence paragraph response answering the inquiry without unnecessary filler.\n\n")
            }
            "bullets" -> {
                contextBuilder.append("USER FORMAT PREFERENCE: BULLET-POINT LIST.\n")
                contextBuilder.append("Format the response using a brief introductory paragraph followed by a clean `bulleted_list` (or `numbered_list`) of key observations.\n\n")
            }
            "table" -> {
                contextBuilder.append("USER FORMAT PREFERENCE: DETAILED TABLE.\n")
                contextBuilder.append("Structure the response primarily as a structured `table` block with clear column headers and data rows, accompanied by a brief summary note.\n\n")
            }
            else -> {
                contextBuilder.append("USER FORMAT PREFERENCE: SMART / RICH BLOCKS.\n")
                contextBuilder.append("Provide a well-balanced visual response combining paragraphs, stat cards, tables, chips, and charts where appropriate.\n\n")
            }
        }

        contextBuilder.append("RULES & BEST PRACTICES:\n")
        contextBuilder.append("- For general queries: start with a friendly paragraph or heading, followed by tables, stat cards, or lists.\n")
        contextBuilder.append("- When asked about trends or comparisons across squadrons/dates: use `trend_chart` or `stat_card` alongside a `table`.\n")
        contextBuilder.append("- When asked who has low attendance or missed events: list them with `cadet_chip` and include `attendance_chips` for their recent attendance.\n")
        contextBuilder.append("- Never hallucinate fake cadets or contact info not in the roster.\n")

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
                    put("temperature", 0.2)
                    put("topP", 0.95)
                })
            }

            val modelsToTry = listOf("gemini-3.5-flash", "gemini-flash-latest")
            var bodyString = ""
            var responseCode = 0
            var successful = false

            for (model in modelsToTry) {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                responseCode = response.code
                val respBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    bodyString = respBody
                    successful = true
                    break
                } else {
                    Log.w("GeminiService", "Model $model returned HTTP $responseCode: $respBody")
                    bodyString = respBody
                }
            }

            if (!successful) {
                Log.e("GeminiService", "All Gemini models failed: HTTP $responseCode: $bodyString")
                val fallbackMsg = "Could not generate answer (HTTP $responseCode). Please verify your Gemini API key quota and network connectivity."
                return@withContext ChatMessage(
                    sender = "gemini",
                    text = fallbackMsg,
                    blocks = ChatBlockParser.parseBlocks(fallbackMsg)
                )
            }

            val respJson = JSONObject(bodyString)
            val candidates = respJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: "No response generated."

            // Parse response into rich structured ChatBlocks
            val blocks = ChatBlockParser.parseBlocks(rawText)
            val plainText = ChatBlockParser.extractPlainText(blocks).ifBlank { rawText }

            // Extract table info for backward compatibility
            val firstTable = blocks.filterIsInstance<com.example.model.ChatBlock.TableBlock>().firstOrNull()

            ChatMessage(
                sender = "gemini",
                text = plainText,
                blocks = blocks,
                tableHeaders = firstTable?.headers ?: emptyList(),
                tableRows = firstTable?.rows ?: emptyList()
            )
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception calling Gemini", e)
            val errorMsg = "Error reaching Gemini service: ${e.localizedMessage ?: "Unknown error"}"
            ChatMessage(
                sender = "gemini",
                text = errorMsg,
                blocks = ChatBlockParser.parseBlocks(errorMsg)
            )
        }
    }
}

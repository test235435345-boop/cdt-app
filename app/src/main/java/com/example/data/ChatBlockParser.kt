package com.example.data

import com.example.model.ChatBlock
import org.json.JSONArray
import org.json.JSONObject

object ChatBlockParser {

    fun parseBlocks(rawText: String): List<ChatBlock> {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) return emptyList()

        // 1. Try to extract and parse JSON array if present
        val jsonArrayString = extractJsonArray(trimmed)
        if (jsonArrayString != null) {
            try {
                val jsonArray = JSONArray(jsonArrayString)
                val blocks = mutableListOf<ChatBlock>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    val block = parseSingleJsonBlock(obj)
                    if (block != null) {
                        blocks.add(block)
                    }
                }
                if (blocks.isNotEmpty()) {
                    return blocks
                }
            } catch (_: Exception) {
                // Fallback to markdown parsing
            }
        }

        // 2. Try JSON object with "blocks" array
        val jsonObjectString = extractJsonObject(trimmed)
        if (jsonObjectString != null) {
            try {
                val jsonObj = JSONObject(jsonObjectString)
                val blocksArr = jsonObj.optJSONArray("blocks")
                if (blocksArr != null) {
                    val blocks = mutableListOf<ChatBlock>()
                    for (i in 0 until blocksArr.length()) {
                        val obj = blocksArr.optJSONObject(i) ?: continue
                        val block = parseSingleJsonBlock(obj)
                        if (block != null) {
                            blocks.add(block)
                        }
                    }
                    if (blocks.isNotEmpty()) {
                        return blocks
                    }
                }
            } catch (_: Exception) {
                // Fallback to markdown parsing
            }
        }

        // 3. Fallback: Parse markdown structure into rich ChatBlocks
        return parseMarkdownToBlocks(trimmed)
    }

    private fun parseSingleJsonBlock(obj: JSONObject): ChatBlock? {
        val type = obj.optString("type", "").lowercase().trim()
        return when (type) {
            "heading", "header", "h1", "h2", "h3" -> {
                val text = obj.optString("text", obj.optString("content", ""))
                val level = obj.optInt("level", if (type == "h1") 1 else if (type == "h2") 2 else if (type == "h3") 3 else 1)
                if (text.isNotBlank()) ChatBlock.HeadingBlock(text, level) else null
            }
            "paragraph", "text" -> {
                val text = obj.optString("text", obj.optString("content", ""))
                if (text.isNotBlank()) ChatBlock.ParagraphBlock(text) else null
            }
            "bulleted_list", "bullet_list", "bullets", "list" -> {
                val itemsArr = obj.optJSONArray("items") ?: obj.optJSONArray("list")
                val items = mutableListOf<String>()
                if (itemsArr != null) {
                    for (j in 0 until itemsArr.length()) {
                        val str = itemsArr.optString(j)
                        if (str.isNotBlank()) items.add(str)
                    }
                }
                if (items.isNotEmpty()) ChatBlock.BulletedListBlock(items) else null
            }
            "numbered_list", "ordered_list" -> {
                val itemsArr = obj.optJSONArray("items") ?: obj.optJSONArray("list")
                val items = mutableListOf<String>()
                if (itemsArr != null) {
                    for (j in 0 until itemsArr.length()) {
                        val str = itemsArr.optString(j)
                        if (str.isNotBlank()) items.add(str)
                    }
                }
                if (items.isNotEmpty()) ChatBlock.NumberedListBlock(items) else null
            }
            "table" -> {
                val headersArr = obj.optJSONArray("headers") ?: JSONArray()
                val headers = mutableListOf<String>()
                for (j in 0 until headersArr.length()) {
                    headers.add(headersArr.optString(j))
                }
                val rowsArr = obj.optJSONArray("rows") ?: JSONArray()
                val rows = mutableListOf<List<String>>()
                for (j in 0 until rowsArr.length()) {
                    val rowArr = rowsArr.optJSONArray(j)
                    if (rowArr != null) {
                        val row = mutableListOf<String>()
                        for (k in 0 until rowArr.length()) {
                            row.add(rowArr.optString(k))
                        }
                        rows.add(row)
                    }
                }
                if (headers.isNotEmpty() || rows.isNotEmpty()) ChatBlock.TableBlock(headers, rows) else null
            }
            "attendance_chips", "attendance_history", "status_chips" -> {
                val title = obj.optString("title", "")
                val cadetName = obj.optString("cadetName", obj.optString("name", ""))
                val cadetId = obj.optString("cadetId", "")
                val chipsArr = obj.optJSONArray("chips") ?: obj.optJSONArray("history") ?: JSONArray()
                val chips = mutableListOf<ChatBlock.AttendanceChipItem>()
                for (j in 0 until chipsArr.length()) {
                    val chipObj = chipsArr.optJSONObject(j)
                    if (chipObj != null) {
                        chips.add(
                            ChatBlock.AttendanceChipItem(
                                status = chipObj.optString("status", "Present"),
                                label = chipObj.optString("label", chipObj.optString("event", "")),
                                date = chipObj.optString("date", "")
                            )
                        )
                    } else {
                        val statusStr = chipsArr.optString(j)
                        if (statusStr.isNotBlank()) {
                            chips.add(ChatBlock.AttendanceChipItem(status = statusStr))
                        }
                    }
                }
                ChatBlock.AttendanceChipsBlock(title, cadetName, cadetId, chips)
            }
            "stat_card", "stat", "metric" -> {
                val value = obj.optString("value", "")
                val label = obj.optString("label", obj.optString("title", ""))
                val subtext = obj.optString("subtext", obj.optString("description", ""))
                val isPositive = if (obj.has("isPositive")) obj.optBoolean("isPositive") else null
                if (value.isNotBlank() || label.isNotBlank()) {
                    ChatBlock.StatCardBlock(value, label, subtext, isPositive)
                } else null
            }
            "trend_chart", "trend", "chart" -> {
                val title = obj.optString("title", "Attendance Trend")
                val subtitle = obj.optString("subtitle", "")
                val threshold = if (obj.has("threshold")) obj.optDouble("threshold").toFloat() else null
                val dataPointsArr = obj.optJSONArray("dataPoints") ?: obj.optJSONArray("data") ?: JSONArray()
                val dataPoints = mutableListOf<ChatBlock.TrendDataPoint>()
                for (j in 0 until dataPointsArr.length()) {
                    val dpObj = dataPointsArr.optJSONObject(j)
                    if (dpObj != null) {
                        dataPoints.add(
                            ChatBlock.TrendDataPoint(
                                label = dpObj.optString("label", "Point ${j + 1}"),
                                value = dpObj.optDouble("value", 0.0).toFloat()
                            )
                        )
                    }
                }
                ChatBlock.TrendChartBlock(title, subtitle, dataPoints, threshold)
            }
            "cadet_chip", "cadet" -> {
                val cadetId = obj.optString("cadetId", "")
                val cadetName = obj.optString("cadetName", obj.optString("name", ""))
                val rank = obj.optString("rank", "")
                val squadron = obj.optString("squadron", obj.optString("sqn", ""))
                val info = obj.optString("info", obj.optString("details", ""))
                if (cadetName.isNotBlank()) {
                    ChatBlock.CadetChipBlock(cadetId, cadetName, rank, squadron, info)
                } else null
            }
            "quote", "blockquote" -> {
                val text = obj.optString("text", obj.optString("content", ""))
                val source = obj.optString("source", obj.optString("cite", ""))
                if (text.isNotBlank()) ChatBlock.QuoteBlock(text, source) else null
            }
            "code_block", "code" -> {
                val language = obj.optString("language", obj.optString("lang", ""))
                val code = obj.optString("code", obj.optString("text", ""))
                if (code.isNotBlank()) ChatBlock.CodeBlock(language, code) else null
            }
            "comparison_card", "comparison", "compare" -> {
                val title = obj.optString("title", "")
                val metricLabel = obj.optString("metricLabel", obj.optString("metric", ""))
                val leftObj = obj.optJSONObject("leftItem") ?: obj.optJSONObject("left") ?: JSONObject()
                val rightObj = obj.optJSONObject("rightItem") ?: obj.optJSONObject("right") ?: JSONObject()

                val leftItem = ChatBlock.ComparisonItem(
                    title = leftObj.optString("title", leftObj.optString("name", "Option A")),
                    value = leftObj.optString("value", "0"),
                    subtext = leftObj.optString("subtext", leftObj.optString("subtitle", "")),
                    isHighlighted = leftObj.optBoolean("isHighlighted", false)
                )
                val rightItem = ChatBlock.ComparisonItem(
                    title = rightObj.optString("title", rightObj.optString("name", "Option B")),
                    value = rightObj.optString("value", "0"),
                    subtext = rightObj.optString("subtext", rightObj.optString("subtitle", "")),
                    isHighlighted = rightObj.optBoolean("isHighlighted", false)
                )
                val diffText = obj.optString("diffText", obj.optString("difference", obj.optString("diff", "")))
                val winnerSide = obj.optString("winnerSide", obj.optString("winner", "")).lowercase()

                ChatBlock.ComparisonCardBlock(
                    title = title,
                    metricLabel = metricLabel,
                    leftItem = leftItem,
                    rightItem = rightItem,
                    diffText = diffText,
                    winnerSide = winnerSide
                )
            }
            "leaderboard", "rankings", "ranking", "top_list" -> {
                val title = obj.optString("title", "Attendance Leaderboard")
                val subtitle = obj.optString("subtitle", "")
                val entriesArr = obj.optJSONArray("entries") ?: obj.optJSONArray("items") ?: JSONArray()
                val entries = mutableListOf<ChatBlock.LeaderboardEntry>()

                for (j in 0 until entriesArr.length()) {
                    val eObj = entriesArr.optJSONObject(j) ?: continue
                    entries.add(
                        ChatBlock.LeaderboardEntry(
                            rank = eObj.optInt("rank", j + 1),
                            title = eObj.optString("title", eObj.optString("name", "Cadet")),
                            subtitle = eObj.optString("subtitle", eObj.optString("squadron", "")),
                            score = eObj.optString("score", eObj.optString("value", "")),
                            badge = eObj.optString("badge", eObj.optString("status", "")),
                            isFlagged = eObj.optBoolean("isFlagged", false),
                            cadetId = eObj.optString("cadetId", "")
                        )
                    )
                }
                if (entries.isNotEmpty() || title.isNotBlank()) {
                    ChatBlock.LeaderboardBlock(title, subtitle, entries)
                } else null
            }
            "alert_banner", "alert", "callout", "banner", "warning_banner" -> {
                val title = obj.optString("title", "")
                val message = obj.optString("message", obj.optString("text", obj.optString("content", "")))
                val severity = obj.optString("severity", obj.optString("type_level", "warning")).lowercase()
                val actionText = obj.optString("actionText", obj.optString("action", ""))

                if (message.isNotBlank() || title.isNotBlank()) {
                    ChatBlock.AlertBannerBlock(title, message, severity, actionText)
                } else null
            }
            "quick_action", "quick_actions", "actions", "action_buttons" -> {
                val title = obj.optString("title", "")
                val actionsArr = obj.optJSONArray("actions") ?: obj.optJSONArray("buttons") ?: JSONArray()
                val actions = mutableListOf<ChatBlock.QuickActionItem>()

                for (j in 0 until actionsArr.length()) {
                    val aObj = actionsArr.optJSONObject(j)
                    if (aObj != null) {
                        actions.add(
                            ChatBlock.QuickActionItem(
                                label = aObj.optString("label", aObj.optString("title", "Action")),
                                targetScreen = aObj.optString("targetScreen", aObj.optString("screen", aObj.optString("target", "attendance"))),
                                targetParam = aObj.optString("targetParam", aObj.optString("param", aObj.optString("id", ""))),
                                iconName = aObj.optString("iconName", aObj.optString("icon", ""))
                            )
                        )
                    }
                }
                if (actions.isNotEmpty()) {
                    ChatBlock.QuickActionBlock(title, actions)
                } else null
            }
            "divider", "hr" -> {
                ChatBlock.DividerBlock
            }
            else -> {
                val text = obj.optString("text", obj.optString("content", obj.optString("value", "")))
                if (text.isNotBlank()) ChatBlock.ParagraphBlock(text) else null
            }
        }
    }

    private fun extractJsonArray(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed
        }
        // Check for ```json [ ... ] ``` or ``` [ ... ] ```
        val codeFenceMatch = Regex("```(?:json)?\\s*(\\[[\\s\\S]*?\\])\\s*```", RegexOption.IGNORE_CASE).find(trimmed)
        if (codeFenceMatch != null) {
            return codeFenceMatch.groupValues[1].trim()
        }
        val firstBracket = trimmed.indexOf('[')
        val lastBracket = trimmed.lastIndexOf(']')
        if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
            return trimmed.substring(firstBracket, lastBracket + 1)
        }
        return null
    }

    private fun extractJsonObject(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed
        }
        val codeFenceMatch = Regex("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```", RegexOption.IGNORE_CASE).find(trimmed)
        if (codeFenceMatch != null) {
            return codeFenceMatch.groupValues[1].trim()
        }
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }
        return null
    }

    fun parseMarkdownToBlocks(markdown: String): List<ChatBlock> {
        val blocks = mutableListOf<ChatBlock>()
        val lines = markdown.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // Blank line
            if (trimmed.isBlank()) {
                i++
                continue
            }

            // Code block ```lang ... ```
            if (trimmed.startsWith("```")) {
                val lang = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                if (i < lines.size) i++ // skip closing ```
                blocks.add(ChatBlock.CodeBlock(lang, codeLines.joinToString("\n")))
                continue
            }

            // Markdown Table | col1 | col2 |
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                val tableLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    tableLines.add(lines[i].trim())
                    i++
                }
                if (tableLines.isNotEmpty()) {
                    val headers = tableLines[0].split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    val rows = mutableListOf<List<String>>()
                    val startIndex = if (tableLines.size > 1 && tableLines[1].contains("-")) 2 else 1
                    for (r in startIndex until tableLines.size) {
                        val rowCells = tableLines[r].split("|").map { it.trim() }.filter { it.isNotEmpty() }
                        if (rowCells.isNotEmpty()) {
                            rows.add(rowCells)
                        }
                    }
                    blocks.add(ChatBlock.TableBlock(headers, rows))
                }
                continue
            }

            // Headings
            if (trimmed.startsWith("#")) {
                var level = 0
                while (level < trimmed.length && trimmed[level] == '#') {
                    level++
                }
                val headingText = trimmed.substring(level).trim()
                blocks.add(ChatBlock.HeadingBlock(headingText, level.coerceIn(1, 3)))
                i++
                continue
            }

            // Horizontal Rule
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                blocks.add(ChatBlock.DividerBlock)
                i++
                continue
            }

            // Blockquote > quote
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trim())
                    i++
                }
                blocks.add(ChatBlock.QuoteBlock(quoteLines.joinToString(" ")))
                continue
            }

            // Bullet list (- item, * item, + item)
            if (isBulletLine(trimmed)) {
                val listItems = mutableListOf<String>()
                while (i < lines.size && isBulletLine(lines[i].trim())) {
                    listItems.add(cleanBulletPrefix(lines[i].trim()))
                    i++
                }
                blocks.add(ChatBlock.BulletedListBlock(listItems))
                continue
            }

            // Numbered list (1. item)
            if (isNumberedLine(trimmed)) {
                val listItems = mutableListOf<String>()
                while (i < lines.size && isNumberedLine(lines[i].trim())) {
                    listItems.add(cleanNumberedPrefix(lines[i].trim()))
                    i++
                }
                blocks.add(ChatBlock.NumberedListBlock(listItems))
                continue
            }

            // Regular paragraph text
            val paragraphLines = mutableListOf<String>()
            while (i < lines.size) {
                val nextTrimmed = lines[i].trim()
                if (nextTrimmed.isBlank() ||
                    nextTrimmed.startsWith("#") ||
                    nextTrimmed.startsWith("```") ||
                    (nextTrimmed.startsWith("|") && nextTrimmed.endsWith("|")) ||
                    nextTrimmed.startsWith(">") ||
                    isBulletLine(nextTrimmed) ||
                    isNumberedLine(nextTrimmed) ||
                    nextTrimmed == "---"
                ) {
                    break
                }
                paragraphLines.add(lines[i])
                i++
            }
            if (paragraphLines.isNotEmpty()) {
                blocks.add(ChatBlock.ParagraphBlock(paragraphLines.joinToString("\n")))
            }
        }

        return blocks
    }

    private fun isBulletLine(line: String): Boolean {
        return (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") || line.startsWith("• "))
    }

    private fun cleanBulletPrefix(line: String): String {
        return line.removePrefix("- ").removePrefix("* ").removePrefix("+ ").removePrefix("• ").trim()
    }

    private fun isNumberedLine(line: String): Boolean {
        return Regex("^\\d+[.)]\\s+").containsMatchIn(line)
    }

    private fun cleanNumberedPrefix(line: String): String {
        return line.replaceFirst(Regex("^\\d+[.)]\\s+"), "").trim()
    }

    fun extractPlainText(blocks: List<ChatBlock>): String {
        val sb = StringBuilder()
        for (block in blocks) {
            when (block) {
                is ChatBlock.HeadingBlock -> sb.append("${block.text}\n\n")
                is ChatBlock.ParagraphBlock -> sb.append("${block.text}\n\n")
                is ChatBlock.BulletedListBlock -> {
                    block.items.forEach { sb.append("• $it\n") }
                    sb.append("\n")
                }
                is ChatBlock.NumberedListBlock -> {
                    block.items.forEachIndexed { idx, item -> sb.append("${idx + 1}. $item\n") }
                    sb.append("\n")
                }
                is ChatBlock.TableBlock -> {
                    if (block.headers.isNotEmpty()) {
                        sb.append(block.headers.joinToString(" | ")).append("\n")
                    }
                    block.rows.forEach { row ->
                        sb.append(row.joinToString(" | ")).append("\n")
                    }
                    sb.append("\n")
                }
                is ChatBlock.AttendanceChipsBlock -> {
                    if (block.title.isNotBlank()) sb.append("${block.title}: ")
                    sb.append(block.chips.joinToString(", ") { "${it.status} (${it.label.ifBlank { it.date }})" }).append("\n\n")
                }
                is ChatBlock.StatCardBlock -> {
                    sb.append("${block.label}: ${block.value} (${block.subtext})\n\n")
                }
                is ChatBlock.TrendChartBlock -> {
                    sb.append("${block.title}\n")
                    block.dataPoints.forEach { sb.append("${it.label}: ${it.value}\n") }
                    sb.append("\n")
                }
                is ChatBlock.CadetChipBlock -> {
                    sb.append("${block.rank} ${block.cadetName} (Sqn ${block.squadron})\n\n")
                }
                is ChatBlock.QuoteBlock -> sb.append("> ${block.text}\n\n")
                is ChatBlock.CodeBlock -> sb.append("${block.code}\n\n")
                is ChatBlock.ComparisonCardBlock -> {
                    sb.append("Comparison: ${block.leftItem.title} (${block.leftItem.value}) vs ${block.rightItem.title} (${block.rightItem.value})\n\n")
                }
                is ChatBlock.LeaderboardBlock -> {
                    sb.append("${block.title}:\n")
                    block.entries.forEach { sb.append("${it.rank}. ${it.title} - ${it.score} (${it.subtitle})\n") }
                    sb.append("\n")
                }
                is ChatBlock.AlertBannerBlock -> {
                    sb.append("[${block.severity.uppercase()}] ${block.title}: ${block.message}\n\n")
                }
                is ChatBlock.QuickActionBlock -> {
                    sb.append("Actions: ${block.actions.joinToString(" | ") { it.label }}\n\n")
                }
                is ChatBlock.DividerBlock -> sb.append("---\n\n")
            }
        }
        return sb.toString().trim()
    }
}

package com.example.model

sealed class ChatBlock {
    data class HeadingBlock(
        val text: String,
        val level: Int = 1
    ) : ChatBlock()

    data class ParagraphBlock(
        val text: String
    ) : ChatBlock()

    data class BulletedListBlock(
        val items: List<String>
    ) : ChatBlock()

    data class NumberedListBlock(
        val items: List<String>
    ) : ChatBlock()

    data class TableBlock(
        val headers: List<String>,
        val rows: List<List<String>>
    ) : ChatBlock()

    data class AttendanceChipItem(
        val status: String,
        val label: String = "",
        val date: String = ""
    )

    data class AttendanceChipsBlock(
        val title: String = "",
        val cadetName: String = "",
        val cadetId: String = "",
        val chips: List<AttendanceChipItem> = emptyList()
    ) : ChatBlock()

    data class StatCardBlock(
        val value: String,
        val label: String,
        val subtext: String = "",
        val isPositive: Boolean? = null
    ) : ChatBlock()

    data class TrendDataPoint(
        val label: String,
        val value: Float
    )

    data class TrendChartBlock(
        val title: String,
        val subtitle: String = "",
        val dataPoints: List<TrendDataPoint> = emptyList(),
        val threshold: Float? = null
    ) : ChatBlock()

    data class CadetChipBlock(
        val cadetId: String = "",
        val cadetName: String,
        val rank: String = "",
        val squadron: String = "",
        val info: String = ""
    ) : ChatBlock()

    data class QuoteBlock(
        val text: String,
        val source: String = ""
    ) : ChatBlock()

    data class CodeBlock(
        val language: String = "",
        val code: String = ""
    ) : ChatBlock()

    data object DividerBlock : ChatBlock()
}

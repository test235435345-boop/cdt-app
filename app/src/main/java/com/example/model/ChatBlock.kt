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

    data class ComparisonItem(
        val title: String,
        val value: String,
        val subtext: String = "",
        val isHighlighted: Boolean = false
    )

    data class ComparisonCardBlock(
        val title: String = "",
        val metricLabel: String = "",
        val leftItem: ComparisonItem,
        val rightItem: ComparisonItem,
        val diffText: String = "",
        val winnerSide: String = "" // "left", "right", or ""
    ) : ChatBlock()

    data class LeaderboardEntry(
        val rank: Int,
        val title: String,
        val subtitle: String = "",
        val score: String,
        val badge: String = "",
        val isFlagged: Boolean = false,
        val cadetId: String = ""
    )

    data class LeaderboardBlock(
        val title: String = "Leaderboard",
        val subtitle: String = "",
        val entries: List<LeaderboardEntry> = emptyList()
    ) : ChatBlock()

    data class AlertBannerBlock(
        val title: String = "",
        val message: String,
        val severity: String = "warning", // "warning", "danger", "success", "info"
        val actionText: String = ""
    ) : ChatBlock()

    data class QuickActionItem(
        val label: String,
        val targetScreen: String, // "attendance", "cadets", "reports", "cadet_profile"
        val targetParam: String = "",
        val iconName: String = ""
    )

    data class QuickActionBlock(
        val title: String = "",
        val actions: List<QuickActionItem> = emptyList()
    ) : ChatBlock()

    data object DividerBlock : ChatBlock()
}

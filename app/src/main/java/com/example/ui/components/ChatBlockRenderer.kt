package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttendanceRecord
import com.example.model.Cadet
import com.example.model.ChatBlock
import com.example.ui.theme.AbsentContainer
import com.example.ui.theme.AbsentContent
import com.example.ui.theme.AbsentNotifiedContainer
import com.example.ui.theme.AbsentNotifiedContent
import com.example.ui.theme.ExcusedContainer
import com.example.ui.theme.ExcusedContent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LateContainer
import com.example.ui.theme.LateContent
import com.example.ui.theme.PlumPrimary
import com.example.ui.theme.PresentContainer
import com.example.ui.theme.PresentContent

@Composable
fun ChatBlockView(
    block: ChatBlock,
    cadets: List<Cadet>,
    onSelectCadet: ((Cadet) -> Unit)? = null,
    onQuickAction: ((targetScreen: String, targetParam: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when (block) {
            is ChatBlock.HeadingBlock -> RenderHeading(block)
            is ChatBlock.ParagraphBlock -> RenderParagraph(block, cadets, onSelectCadet)
            is ChatBlock.BulletedListBlock -> RenderBulletedList(block)
            is ChatBlock.NumberedListBlock -> RenderNumberedList(block)
            is ChatBlock.TableBlock -> RenderTable(block)
            is ChatBlock.AttendanceChipsBlock -> RenderAttendanceChips(block, cadets, onSelectCadet)
            is ChatBlock.StatCardBlock -> RenderStatCard(block)
            is ChatBlock.TrendChartBlock -> RenderTrendChart(block)
            is ChatBlock.CadetChipBlock -> RenderCadetChip(block, cadets, onSelectCadet)
            is ChatBlock.ComparisonCardBlock -> RenderComparisonCard(block)
            is ChatBlock.LeaderboardBlock -> RenderLeaderboard(block, cadets, onSelectCadet)
            is ChatBlock.AlertBannerBlock -> RenderAlertBanner(block, onQuickAction)
            is ChatBlock.QuickActionBlock -> RenderQuickAction(block, onQuickAction)
            is ChatBlock.QuoteBlock -> RenderQuote(block)
            is ChatBlock.CodeBlock -> RenderCodeBlock(block)
            is ChatBlock.DividerBlock -> RenderDivider()
        }
    }
}

@Composable
private fun RenderHeading(block: ChatBlock.HeadingBlock) {
    val style = when (block.level) {
        1 -> MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 19.sp
        )
        2 -> MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 16.sp
        )
        else -> MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp
        )
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        MarkdownText(
            text = block.text,
            style = style,
            color = if (block.level <= 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RenderParagraph(
    block: ChatBlock.ParagraphBlock,
    cadets: List<Cadet>,
    onSelectCadet: ((Cadet) -> Unit)?
) {
    MarkdownText(
        text = block.text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun RenderBulletedList(block: ChatBlock.BulletedListBlock) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        block.items.forEach { itemText ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp, end = 10.dp, start = 4.dp)
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                MarkdownText(
                    text = itemText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RenderNumberedList(block: ChatBlock.NumberedListBlock) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        block.items.forEachIndexed { index, itemText ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(top = 2.dp, end = 8.dp)
                        .size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp
                        )
                    }
                }
                MarkdownText(
                    text = itemText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RenderTable(block: ChatBlock.TableBlock) {
    val scrollState = rememberScrollState()

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(8.dp)
        ) {
            // Header Row
            if (block.headers.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    block.headers.forEach { header ->
                        Box(
                            modifier = Modifier
                                .widthIn(min = 90.dp, max = 220.dp)
                                .padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = header,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Data Rows
            block.rows.forEachIndexed { rowIndex, rowCells ->
                val rowBg = if (rowIndex % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(rowBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    rowCells.forEach { cellText ->
                        Box(
                            modifier = Modifier
                                .widthIn(min = 90.dp, max = 220.dp)
                                .padding(horizontal = 6.dp)
                        ) {
                            MarkdownText(
                                text = cellText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderAttendanceChips(
    block: ChatBlock.AttendanceChipsBlock,
    cadets: List<Cadet>,
    onSelectCadet: ((Cadet) -> Unit)?
) {
    val matchingCadet = cadets.firstOrNull {
        (block.cadetId.isNotBlank() && it.id == block.cadetId) ||
                (block.cadetName.isNotBlank() && (it.displayName.contains(block.cadetName, ignoreCase = true) || it.lastName.contains(block.cadetName, ignoreCase = true)))
    }

    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header if cadet or title given
            if (block.title.isNotBlank() || block.cadetName.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (matchingCadet != null && onSelectCadet != null) {
                                    Modifier.clickable { onSelectCadet(matchingCadet) }
                                } else Modifier
                            )
                            .padding(2.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (block.cadetName.isNotBlank()) "${block.cadetName} Attendance History" else block.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (matchingCadet != null && onSelectCadet != null) {
                        Text(
                            text = "View Profile ›",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onSelectCadet(matchingCadet) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Chips Row / Horizontal Scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                block.chips.forEach { chipItem ->
                    val statusText = chipItem.status
                    val (bg, fg) = getStatusColors(statusText)

                    Surface(
                        color = bg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = fg
                            )
                            if (chipItem.label.isNotBlank() || chipItem.date.isNotBlank()) {
                                Text(
                                    text = chipItem.label.ifBlank { chipItem.date },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = fg.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderStatCard(block: ChatBlock.StatCardBlock) {
    val valueColor = when (block.isPositive) {
        true -> PresentContent
        false -> AbsentContent
        null -> MaterialTheme.colorScheme.primary
    }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = block.value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                if (block.subtext.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = block.subtext,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (block.isPositive != null) {
                Surface(
                    color = if (block.isPositive) PresentContainer else AbsentContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            if (block.isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (block.isPositive) PresentContent else AbsentContent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (block.isPositive) "On Track" else "Flagged",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (block.isPositive) PresentContent else AbsentContent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderTrendChart(block: ChatBlock.TrendChartBlock) {
    if (block.dataPoints.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val goldColor = GoldAccent
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                ) {
                    Icon(
                        Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = block.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (block.threshold != null) {
                    Surface(
                        color = PresentContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = "Target: ${block.threshold.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PresentContent,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (block.subtitle.isNotBlank()) {
                Text(
                    text = block.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                val w = size.width
                val h = size.height
                val count = block.dataPoints.size
                val maxVal = 100f
                val barWidth = (w / (count * 2f)).coerceIn(16f, 44f)
                val spacing = w / count

                // Draw threshold line if present
                block.threshold?.let { target ->
                    val y = h - (target / maxVal * (h - 25f))
                    drawLine(
                        color = PresentContent.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // Draw bars and labels
                block.dataPoints.forEachIndexed { i, dp ->
                    val x = spacing * i + (spacing / 2f)
                    val barHeight = (dp.value / maxVal * (h - 30f)).coerceAtLeast(6f)
                    val topY = h - 20f - barHeight

                    // Bar color
                    val barColor = if (dp.value >= (block.threshold ?: 70f)) primaryColor else AbsentContent

                    // Draw Bar
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x - barWidth / 2f, topY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )

                    // Value text above bar
                    val valText = "${dp.value.toInt()}%"
                    val valLayout = textMeasurer.measure(
                        text = valText,
                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = barColor)
                    )
                    drawText(
                        textLayoutResult = valLayout,
                        topLeft = Offset(x - (valLayout.size.width / 2f), (topY - valLayout.size.height - 2f).coerceAtLeast(0f))
                    )

                    // Label text below bar
                    val labelLayout = textMeasurer.measure(
                        text = dp.label,
                        style = TextStyle(fontSize = 9.sp, color = onSurfaceVariant)
                    )
                    drawText(
                        textLayoutResult = labelLayout,
                        topLeft = Offset(x - (labelLayout.size.width / 2f), h - 14f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderCadetChip(
    block: ChatBlock.CadetChipBlock,
    cadets: List<Cadet>,
    onSelectCadet: ((Cadet) -> Unit)?
) {
    val matchingCadet = cadets.firstOrNull {
        (block.cadetId.isNotBlank() && it.id == block.cadetId) ||
                (block.cadetName.isNotBlank() && (it.displayName.contains(block.cadetName, ignoreCase = true) || it.lastName.contains(block.cadetName, ignoreCase = true)))
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .then(
                if (matchingCadet != null && onSelectCadet != null) {
                    Modifier.clickable { onSelectCadet(matchingCadet) }
                } else Modifier
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                val title = if (block.rank.isNotBlank()) "${block.rank} ${block.cadetName}" else block.cadetName
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                val sub = listOfNotNull(
                    if (block.squadron.isNotBlank()) "Sqn ${block.squadron}" else null,
                    block.info.ifBlank { null }
                ).joinToString(" • ")

                if (sub.isNotBlank()) {
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            if (matchingCadet != null && onSelectCadet != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View ›",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RenderQuote(block: ChatBlock.QuoteBlock) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(0.dp, 10.dp, 10.dp, 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.padding(end = 8.dp)) {
                MarkdownText(
                    text = block.text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (block.source.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "— ${block.source}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderCodeBlock(block: ChatBlock.CodeBlock) {
    val clipboard = LocalClipboardManager.current

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = block.language.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { clipboard.setText(AnnotatedString(block.code)) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = block.code,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RenderDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

@Composable
private fun RenderComparisonCard(block: ChatBlock.ComparisonCardBlock) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (block.title.isNotBlank()) {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (block.metricLabel.isNotBlank()) {
                Text(
                    text = block.metricLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left Option
                val isLeftWinner = block.winnerSide == "left" || block.leftItem.isHighlighted
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLeftWinner) PresentContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (isLeftWinner) Stroke(1.5f).let { androidx.compose.foundation.BorderStroke(1.5.dp, PresentContent.copy(alpha = 0.6f)) } else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = block.leftItem.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = block.leftItem.value,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isLeftWinner) PresentContent else MaterialTheme.colorScheme.primary
                        )
                        if (block.leftItem.subtext.isNotBlank()) {
                            Text(
                                text = block.leftItem.subtext,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                // VS Badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                ) {
                    Text(
                        text = "VS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Right Option
                val isRightWinner = block.winnerSide == "right" || block.rightItem.isHighlighted
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isRightWinner) PresentContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (isRightWinner) androidx.compose.foundation.BorderStroke(1.5.dp, PresentContent.copy(alpha = 0.6f)) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = block.rightItem.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = block.rightItem.value,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isRightWinner) PresentContent else MaterialTheme.colorScheme.primary
                        )
                        if (block.rightItem.subtext.isNotBlank()) {
                            Text(
                                text = block.rightItem.subtext,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (block.diffText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = PresentContent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = block.diffText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderLeaderboard(
    block: ChatBlock.LeaderboardBlock,
    cadets: List<Cadet>,
    onSelectCadet: ((Cadet) -> Unit)?
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = block.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (block.subtitle.isNotBlank()) {
                            Text(
                                text = block.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            block.entries.forEachIndexed { index, entry ->
                val matchingCadet = cadets.firstOrNull { it.id == entry.cadetId }
                    ?: cadets.firstOrNull { cadet ->
                        val cleanTitle = entry.title.lowercase()
                        cleanTitle.contains(cadet.lastName.lowercase()) ||
                                (cadet.firstName.isNotBlank() && cleanTitle.contains(cadet.firstName.lowercase()))
                    }

                val rankBadgeColor = when (entry.rank) {
                    1 -> Color(0xFFFFD700) // Gold
                    2 -> Color(0xFFC0C0C0) // Silver
                    3 -> Color(0xFFCD7F32) // Bronze
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val rankTextColor = when (entry.rank) {
                    1, 2, 3 -> Color(0xFF1E1E1E)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (entry.isFlagged) AbsentContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable(enabled = matchingCadet != null && onSelectCadet != null) {
                            matchingCadet?.let { onSelectCadet?.invoke(it) }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(rankBadgeColor, CircleShape)
                            ) {
                                Text(
                                    text = "${entry.rank}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = rankTextColor
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (entry.subtitle.isNotBlank()) {
                                    Text(
                                        text = entry.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = entry.score,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (entry.isFlagged) AbsentContent else PresentContent
                            )
                            if (entry.badge.isNotBlank()) {
                                Surface(
                                    color = if (entry.isFlagged) AbsentContainer else PresentContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = entry.badge,
                                        color = if (entry.isFlagged) AbsentContent else PresentContent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderAlertBanner(
    block: ChatBlock.AlertBannerBlock,
    onQuickAction: ((targetScreen: String, targetParam: String) -> Unit)?
) {
    val (containerColor, contentColor, icon) = when (block.severity.lowercase()) {
        "danger", "error" -> Triple(AbsentContainer, AbsentContent, Icons.Default.Error)
        "success" -> Triple(PresentContainer, PresentContent, Icons.Default.CheckCircle)
        "info" -> Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.primary,
            Icons.Default.Info
        )
        else -> Triple(LateContainer, LateContent, Icons.Default.Warning) // default warning
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (block.title.isNotBlank()) {
                        Text(
                            text = block.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                        text = block.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor,
                        lineHeight = 18.sp
                    )
                }
            }

            if (block.actionText.isNotBlank() && onQuickAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = {
                            val target = if (block.actionText.contains("cadet", ignoreCase = true)) "cadets"
                            else if (block.actionText.contains("report", ignoreCase = true) || block.actionText.contains("sync", ignoreCase = true)) "reports"
                            else "attendance"
                            onQuickAction(target, "")
                        }
                    ) {
                        Text(
                            text = block.actionText,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderQuickAction(
    block: ChatBlock.QuickActionBlock,
    onQuickAction: ((targetScreen: String, targetParam: String) -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        if (block.title.isNotBlank()) {
            Text(
                text = block.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            block.actions.forEach { action ->
                val icon = when (action.iconName.lowercase()) {
                    "check", "attendance", "mark" -> Icons.Default.CheckCircle
                    "person", "cadet", "user" -> Icons.Default.Person
                    "chart", "report", "stats" -> Icons.Default.Assessment
                    "calendar", "event" -> Icons.Default.Event
                    else -> when (action.targetScreen.lowercase()) {
                        "attendance" -> Icons.Default.CheckCircle
                        "cadets", "cadet_profile" -> Icons.Default.Person
                        "reports" -> Icons.Default.Assessment
                        else -> Icons.Default.AutoAwesome
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .clickable(enabled = onQuickAction != null) {
                            onQuickAction?.invoke(action.targetScreen, action.targetParam)
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun getStatusColors(status: String): Pair<Color, Color> {
    return when (status.trim()) {
        AttendanceRecord.STATUS_PRESENT, "P", "Present" -> PresentContainer to PresentContent
        AttendanceRecord.STATUS_ABSENT, "A", "Absent" -> AbsentContainer to AbsentContent
        AttendanceRecord.STATUS_ABSENT_NOTIFIED, "N", "Absent-Notified", "Notified" -> AbsentNotifiedContainer to AbsentNotifiedContent
        AttendanceRecord.STATUS_LATE, "L", "Late" -> LateContainer to LateContent
        AttendanceRecord.STATUS_EXCUSED, "E", "Excused" -> ExcusedContainer to ExcusedContent
        else -> PresentContainer to PresentContent
    }
}

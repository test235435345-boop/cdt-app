package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant
    val codeTextColor = MaterialTheme.colorScheme.primary

    val annotatedString = remember(text, primaryColor, codeBgColor, codeTextColor) {
        parseMarkdownToAnnotatedString(
            markdown = text,
            linkColor = primaryColor,
            codeBgColor = codeBgColor,
            codeTextColor = codeTextColor
        )
    }

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = style.merge(
            TextStyle(
                color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                textAlign = textAlign ?: TextAlign.Start,
                lineHeight = lineHeight
            )
        ),
        maxLines = maxLines,
        overflow = overflow,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                }
        }
    )
}

fun parseMarkdownToAnnotatedString(
    markdown: String,
    linkColor: Color,
    codeBgColor: Color,
    codeTextColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var idx = 0
        val len = markdown.length

        while (idx < len) {
            // Check for Markdown Link [title](url)
            if (markdown[idx] == '[' && markdown.indexOf(']', idx) != -1) {
                val closeBracket = markdown.indexOf(']', idx)
                if (closeBracket + 1 < len && markdown[closeBracket + 1] == '(') {
                    val closeParen = markdown.indexOf(')', closeBracket + 1)
                    if (closeParen != -1) {
                        val linkTitle = markdown.substring(idx + 1, closeBracket)
                        val linkUrl = markdown.substring(closeBracket + 2, closeParen)
                        val startPos = length
                        append(linkTitle)
                        addStyle(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            ),
                            startPos,
                            length
                        )
                        addStringAnnotation(tag = "URL", annotation = linkUrl, start = startPos, end = length)
                        idx = closeParen + 1
                        continue
                    }
                }
            }

            // Check for inline code `code`
            if (markdown[idx] == '`' && !markdown.startsWith("```", idx)) {
                val endCode = markdown.indexOf('`', idx + 1)
                if (endCode != -1 && !markdown.startsWith("```", endCode)) {
                    val codeContent = markdown.substring(idx + 1, endCode)
                    val startPos = length
                    append(" $codeContent ")
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBgColor,
                            color = codeTextColor,
                            fontWeight = FontWeight.Medium
                        ),
                        startPos,
                        length
                    )
                    idx = endCode + 1
                    continue
                }
            }

            // Check for Bold **bold** or __bold__
            if ((markdown.startsWith("**", idx) || markdown.startsWith("__", idx)) && len > idx + 2) {
                val marker = markdown.substring(idx, idx + 2)
                val endBold = markdown.indexOf(marker, idx + 2)
                if (endBold != -1) {
                    val boldContent = markdown.substring(idx + 2, endBold)
                    val startPos = length
                    append(boldContent)
                    addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        startPos,
                        length
                    )
                    idx = endBold + 2
                    continue
                }
            }

            // Check for Italic *italic* or _italic_
            if ((markdown[idx] == '*' || markdown[idx] == '_') && len > idx + 1) {
                val marker = markdown[idx]
                // Make sure it's not double marker
                if (idx + 1 < len && markdown[idx + 1] != marker) {
                    val endItalic = markdown.indexOf(marker, idx + 1)
                    if (endItalic != -1 && (endItalic + 1 >= len || markdown[endItalic + 1] != marker)) {
                        val italicContent = markdown.substring(idx + 1, endItalic)
                        val startPos = length
                        append(italicContent)
                        addStyle(
                            SpanStyle(fontStyle = FontStyle.Italic),
                            startPos,
                            length
                        )
                        idx = endItalic + 1
                        continue
                    }
                }
            }

            // Skip any stray markdown fence characters if left unparsed
            if (markdown.startsWith("```", idx)) {
                idx += 3
                continue
            }

            // Normal character
            append(markdown[idx])
            idx++
        }
    }
}

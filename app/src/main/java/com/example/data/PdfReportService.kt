package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.AttendanceRecord
import com.example.model.BandEvent
import com.example.model.Cadet
import com.example.model.OrgSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportService(private val context: Context) {

    companion object {
        const val CANONICAL_PDF_FILENAME = "Cadet_Attendance_Report.pdf"
    }

    /**
     * Generates or updates the canonical attendance PDF in-place with the latest information.
     * Follows the exact format:
     * - Section 1: "Training Nights" with LEGEND: ✔ = PRESENT; ✖ = ABSENT; ⚠ = ABSENT BUT INFORMED (HAD LET SOMEONE KNOW); ⏰ = LATE
     *   Columns: Name, Rank, Phone, Email, BAND, Flight, Appointment, followed by each Training Night date (MMMM d)
     * - Section 2: "Band Practices"
     *   Columns: Name, Rank, Phone, Email, BAND, Flight, Appointment, followed by each Band Practice date (MMMM d)
     */
    suspend fun generateOrUpdatePdf(
        settings: OrgSettings,
        cadets: List<Cadet>,
        events: List<BandEvent>,
        records: List<AttendanceRecord>
    ): Result<File> = withContext(Dispatchers.IO) {
        var pdfDocument: PdfDocument? = null
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val targetPdfFile = File(exportDir, CANONICAL_PDF_FILENAME)

            // Filter active cadets sorted by last name, then first name
            val activeCadets = cadets.filter { it.status == Cadet.STATUS_ACTIVE }
                .sortedWith(compareBy({ it.lastName.lowercase(Locale.ROOT) }, { it.firstName.lowercase(Locale.ROOT) }))

            // Separate events into Training Nights and Band Practices (or all active events)
            val nonCancelledEvents = events.filter { !it.isCancelled }.sortedBy { it.date }
            val trainingEvents = nonCancelledEvents.filter {
                it.type.equals(BandEvent.TYPE_TRAINING_NIGHT, ignoreCase = true) ||
                        it.title.contains("Training", ignoreCase = true)
            }.ifEmpty {
                // If types are not tagged, take the first half or all non-cancelled
                nonCancelledEvents
            }

            val bandEvents = nonCancelledEvents.filter {
                it.type.equals(BandEvent.TYPE_BAND_PRACTICE, ignoreCase = true) ||
                        it.title.contains("Band", ignoreCase = true) ||
                        it.title.contains("Practice", ignoreCase = true)
            }.ifEmpty {
                nonCancelledEvents.filter { !trainingEvents.contains(it) }
            }

            val recordsMap = records.associateBy { "${it.eventId}_${it.cadetId}" }

            // Geometry calculations
            val maxDates = maxOf(trainingEvents.size, bandEvents.size, 1)
            val nameColWidth = 110f
            val rankColWidth = 85f
            val phoneColWidth = 75f
            val emailColWidth = 145f
            val squadronColWidth = 45f
            val flightColWidth = 35f
            val appointmentColWidth = 65f
            val infoColsTotal = nameColWidth + rankColWidth + phoneColWidth + emailColWidth + squadronColWidth + flightColWidth + appointmentColWidth

            val dateColWidth = 48f
            val marginX = 24f
            val marginY = 24f

            val totalTableWidth = infoColsTotal + (maxDates * dateColWidth)
            val pageWidth = maxOf(1200, (totalTableWidth + (marginX * 2)).toInt())

            val rowHeight = 16f
            val headerRowHeight = 22f
            val sectionGap = 28f

            val table1Rows = 1 + activeCadets.size
            val table1Height = headerRowHeight + (activeCadets.size * rowHeight)

            val table2Rows = if (bandEvents.isNotEmpty()) 1 + activeCadets.size else 0
            val table2Height = if (bandEvents.isNotEmpty()) headerRowHeight + (activeCadets.size * rowHeight) else 0f

            val totalContentHeight = marginY + 28f + table1Height + sectionGap + 24f + table2Height + marginY + 40f
            val pageHeight = maxOf(850, totalContentHeight.toInt())

            pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Paints
            val bgPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

            val gridPaint = Paint().apply {
                color = Color.rgb(180, 180, 180)
                strokeWidth = 0.5f
                style = Paint.Style.STROKE
            }

            val borderPaint = Paint().apply {
                color = Color.rgb(80, 80, 80)
                strokeWidth = 1.0f
                style = Paint.Style.STROKE
            }

            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val legendPaint = Paint().apply {
                color = Color.rgb(20, 20, 20)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val headerTextPaint = Paint().apply {
                color = Color.BLACK
                textSize = 7f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val cellTextPaint = Paint().apply {
                color = Color.rgb(30, 30, 30)
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val symbolPaint = Paint().apply {
                color = Color.BLACK
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(245, 245, 245)
                style = Paint.Style.FILL
            }

            val zebraBgPaint = Paint().apply {
                color = Color.rgb(250, 250, 250)
                style = Paint.Style.FILL
            }

            var currentY = marginY

            val inputDateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val displayDateSdf = SimpleDateFormat("MMMM d", Locale.US)

            fun formatHeaderDate(rawDate: String): String {
                return try {
                    val parsed = inputDateSdf.parse(rawDate)
                    if (parsed != null) displayDateSdf.format(parsed) else rawDate
                } catch (e: Exception) {
                    rawDate
                }
            }

            // Function to render a section (Table 1: Training Nights, Table 2: Band Practices)
            fun drawSectionTable(
                sectionTitle: String,
                showLegend: Boolean,
                eventList: List<BandEvent>
            ) {
                // Draw Title and Legend
                canvas.drawText(sectionTitle, marginX, currentY + 12f, titlePaint)

                if (showLegend) {
                    val legendStr = "LEGEND: ✔ = PRESENT; ✖ = ABSENT; ⚠ = ABSENT BUT INFORMED (HAD LET SOMEONE KNOW); ⏰ = LATE"
                    canvas.drawText(legendStr, marginX + 115f, currentY + 11f, legendPaint)
                }

                currentY += 20f

                val startX = marginX
                val tableW = infoColsTotal + (eventList.size * dateColWidth)

                // Header Background
                canvas.drawRect(startX, currentY, startX + tableW, currentY + headerRowHeight, headerBgPaint)

                // Draw Header Cells
                var colX = startX

                fun drawHeaderCell(title: String, width: Float) {
                    canvas.drawRect(colX, currentY, colX + width, currentY + headerRowHeight, gridPaint)
                    canvas.drawText(title, colX + 4f, currentY + (headerRowHeight / 2) + 2.5f, headerTextPaint)
                    colX += width
                }

                drawHeaderCell("", nameColWidth)
                drawHeaderCell("", rankColWidth)
                drawHeaderCell("", phoneColWidth)
                drawHeaderCell("", emailColWidth)
                drawHeaderCell("", squadronColWidth)
                drawHeaderCell("", flightColWidth)
                drawHeaderCell("", appointmentColWidth)

                // Event Date Headers
                for (ev in eventList) {
                    val dateLabel = formatHeaderDate(ev.date)
                    canvas.drawRect(colX, currentY, colX + dateColWidth, currentY + headerRowHeight, gridPaint)

                    // Draw date text centered or clipped
                    val textW = headerTextPaint.measureText(dateLabel)
                    val textX = if (textW < dateColWidth) {
                        colX + (dateColWidth - textW) / 2f
                    } else {
                        colX + 2f
                    }
                    canvas.drawText(dateLabel, textX, currentY + (headerRowHeight / 2) + 2.5f, headerTextPaint)
                    colX += dateColWidth
                }

                // Header Outline
                canvas.drawRect(startX, currentY, startX + tableW, currentY + headerRowHeight, borderPaint)
                currentY += headerRowHeight

                // Draw Cadet Data Rows
                for ((idx, cadet) in activeCadets.withIndex()) {
                    val rowTop = currentY
                    val rowBottom = currentY + rowHeight

                    // Zebra stripe
                    if (idx % 2 == 1) {
                        canvas.drawRect(startX, rowTop, startX + tableW, rowBottom, zebraBgPaint)
                    }

                    var rowX = startX

                    fun drawDataCell(text: String, width: Float, alignCenter: Boolean = false) {
                        canvas.drawRect(rowX, rowTop, rowX + width, rowBottom, gridPaint)
                        if (alignCenter) {
                            symbolPaint.color = Color.rgb(40, 40, 40)
                            canvas.drawText(text, rowX + (width / 2f), rowTop + 11.5f, symbolPaint)
                        } else {
                            // Clip text if longer than width
                            var display = text
                            while (display.length > 3 && cellTextPaint.measureText(display) > (width - 6f)) {
                                display = display.substring(0, display.length - 2) + "…"
                            }
                            canvas.drawText(display, rowX + 4f, rowTop + 11f, cellTextPaint)
                        }
                        rowX += width
                    }

                    // Columns: Name, Rank, Phone, Email, Squadron, Flight, Appointment
                    val fullName = "${cadet.lastName} ${cadet.firstName}".trim()
                    drawDataCell(fullName, nameColWidth)
                    drawDataCell(cadet.rank, rankColWidth)
                    drawDataCell(cadet.phone, phoneColWidth)
                    drawDataCell(cadet.email, emailColWidth)
                    val sqLabel = if (cadet.squadron.isNotBlank()) "BAND ${cadet.squadron}" else "BAND"
                    drawDataCell(sqLabel, squadronColWidth, alignCenter = true)
                    drawDataCell(cadet.flight.ifBlank { "0" }, flightColWidth, alignCenter = true)
                    drawDataCell(cadet.appointment.ifBlank { "N/A" }, appointmentColWidth, alignCenter = true)

                    // Event Attendance Cells
                    for (ev in eventList) {
                        val rec = recordsMap["${ev.id}_${cadet.id}"]
                        val statusSymbol = when (rec?.status) {
                            AttendanceRecord.STATUS_PRESENT -> "✔"
                            AttendanceRecord.STATUS_ABSENT -> "✖"
                            AttendanceRecord.STATUS_ABSENT_NOTIFIED -> "⚠"
                            AttendanceRecord.STATUS_LATE -> "⏰"
                            AttendanceRecord.STATUS_EXCUSED -> "N/A"
                            else -> ""
                        }

                        // Colors for symbols matching clean legible theme
                        val sColor = when (statusSymbol) {
                            "✔" -> Color.rgb(24, 134, 75)
                            "✖" -> Color.rgb(198, 40, 40)
                            "⚠" -> Color.rgb(217, 119, 6)
                            "⏰" -> Color.rgb(2, 132, 199)
                            else -> Color.rgb(100, 100, 100)
                        }

                        canvas.drawRect(rowX, rowTop, rowX + dateColWidth, rowBottom, gridPaint)
                        if (statusSymbol.isNotBlank()) {
                            symbolPaint.color = sColor
                            canvas.drawText(statusSymbol, rowX + (dateColWidth / 2f), rowTop + 11.5f, symbolPaint)
                        }
                        rowX += dateColWidth
                    }

                    // Row Outer border
                    canvas.drawRect(startX, rowTop, startX + tableW, rowBottom, borderPaint)
                    currentY += rowHeight
                }
            }

            // 1. Render Table 1: Training Nights
            drawSectionTable(
                sectionTitle = "Training Nights",
                showLegend = true,
                eventList = trainingEvents
            )

            // 2. Render Table 2: Band Practices (if present)
            if (bandEvents.isNotEmpty()) {
                currentY += sectionGap
                drawSectionTable(
                    sectionTitle = "Band Practices",
                    showLegend = false,
                    eventList = bandEvents
                )
            }

            pdfDocument.finishPage(page)

            // Overwrite THIS exact canonical PDF file in place
            if (targetPdfFile.exists()) {
                targetPdfFile.delete()
            }
            FileOutputStream(targetPdfFile).use { out ->
                pdfDocument.writeTo(out)
            }

            Result.success(targetPdfFile)
        } catch (e: Exception) {
            Log.e("PdfReportService", "generateOrUpdatePdf failed", e)
            Result.failure(e)
        } finally {
            try {
                pdfDocument?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Gets the FileProvider content Uri for the canonical PDF
     */
    fun getCanonicalPdfUri(): Uri? {
        val exportDir = File(context.cacheDir, "exports")
        val file = File(exportDir, CANONICAL_PDF_FILENAME)
        return if (file.exists()) {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } else null
    }

    /**
     * Checks whether the canonical PDF file exists and returns its last modified time
     */
    fun getCanonicalPdfLastModified(): Long {
        val exportDir = File(context.cacheDir, "exports")
        val file = File(exportDir, CANONICAL_PDF_FILENAME)
        return if (file.exists()) file.lastModified() else 0L
    }

    /**
     * Creates an Intent to view/open the canonical PDF directly
     */
    fun createViewPdfIntent(): Intent? {
        val uri = getCanonicalPdfUri() ?: return null
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Creates an Intent to share the canonical PDF
     */
    fun createSharePdfIntent(): Intent? {
        val uri = getCanonicalPdfUri() ?: return null
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

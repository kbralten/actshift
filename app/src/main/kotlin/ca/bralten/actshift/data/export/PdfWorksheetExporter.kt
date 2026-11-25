package ca.bralten.actshift.data.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import ca.bralten.actshift.domain.InsightsCalculator
import ca.bralten.actshift.domain.model.ActivityWithLog
import ca.bralten.actshift.domain.model.AverageDeltaStats
import ca.bralten.actshift.domain.model.CciScaleLevel
import ca.bralten.actshift.domain.model.InsightsSummary
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Generates a therapist-friendly PDF styled after the CCI Behavioural Activation Worksheet.
 */
object PdfWorksheetExporter {

  private const val PAGE_WIDTH = 612
  private const val PAGE_HEIGHT = 792
  private const val MARGIN = 36f
  private const val CONTENT_WIDTH = PAGE_WIDTH - 2f * MARGIN
  private const val PAGE_BOTTOM_Y = 742f

  private const val COLOR_INK = 0xFF1A1C19.toInt()
  private const val COLOR_MUTED = 0xFF4F544D.toInt()
  private const val COLOR_BORDER = 0xFFC5C8BE.toInt()
  private const val COLOR_SURFACE_TINT = 0xFFF4F6F0.toInt()
  private const val COLOR_ACCENT = 0xFF476857.toInt()
  private const val COLOR_ACCENT_TINT = 0xFFE8F3EC.toInt()

  private val exportDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
  private val activityDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.US)

  fun writeToStream(
    items: List<ActivityWithLog>,
    insights: InsightsSummary,
    outputStream: OutputStream,
    zoneId: ZoneId = ZoneId.systemDefault(),
    generatedAtMs: Long = System.currentTimeMillis(),
  ) {
    val completeItems =
      items
        .filter { it.log?.computeDeltaSummary() != null }
        .sortedBy { InsightsCalculator.effectiveTimestampMs(it) }

    val document = PdfDocument()
    try {
      val paints = PdfPaints()
      var pageNumber = 1
      var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
      var canvas = page.canvas
      var cursorY = drawFirstPageHeader(canvas, paints, insights, completeItems.size, generatedAtMs, zoneId)

      if (completeItems.isNotEmpty()) {
        val summaryHeight = measureSummaryBoxHeight(paints, insights)
        if (cursorY + summaryHeight > PAGE_BOTTOM_Y) {
          drawFooter(canvas, paints, pageNumber)
          document.finishPage(page)
          pageNumber += 1
          page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
          canvas = page.canvas
          cursorY = drawContinuationHeader(canvas, paints, insights)
        }
        cursorY = drawSummaryBox(canvas, paints, insights, cursorY) + 14f
      }

      if (completeItems.isEmpty()) {
        val emptyLayout = buildLayout("No completed activities in this range yet.", paints.bodyMuted, CONTENT_WIDTH.toInt())
        emptyLayout.drawAt(canvas, MARGIN, cursorY)
        cursorY += emptyLayout.height + 16f
      } else {
        for (item in completeItems) {
          val measured = measureActivityBlock(item, paints, zoneId)
          if (cursorY + measured.totalHeight > PAGE_BOTTOM_Y) {
            drawFooter(canvas, paints, pageNumber)
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            cursorY = drawContinuationHeader(canvas, paints, insights)
          }
          drawActivityBlock(canvas, paints, item, measured, cursorY)
          cursorY += measured.totalHeight + 10f
        }
      }

      val reflectionBoxHeight = 84f
      if (cursorY + reflectionBoxHeight > PAGE_BOTTOM_Y) {
        drawFooter(canvas, paints, pageNumber)
        document.finishPage(page)
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        canvas = page.canvas
        cursorY = drawContinuationHeader(canvas, paints, insights)
      }
      drawSessionReflectionBox(canvas, paints, cursorY)
      drawFooter(canvas, paints, pageNumber)
      document.finishPage(page)

      document.writeTo(outputStream)
      outputStream.flush()
    } finally {
      document.close()
    }
  }

  private fun drawFirstPageHeader(
    canvas: Canvas,
    paints: PdfPaints,
    insights: InsightsSummary,
    count: Int,
    generatedAtMs: Long,
    zoneId: ZoneId,
  ): Float {
    var y = MARGIN
    canvas.drawText("Behavioural Activation Worksheet — Fun & Achievement", MARGIN, y + 15f, paints.title)
    y += 22f

    val exportedDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(generatedAtMs), zoneId).format(exportDateFormatter)
    val countLabel = if (count == 1) "1 completed activity" else "$count completed activities"
    val subtitle = "ActShift Log · ${insights.timeRange.label} · $countLabel · Exported $exportedDate"
    canvas.drawText(subtitle, MARGIN, y + 10f, paints.subtitle)
    y += 20f

    // CCI 0-8 rating scale strip
    val stripHeight = 36f
    val stripRect = RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + stripHeight)
    canvas.drawRoundRect(stripRect, 6f, 6f, paints.surfaceFill)
    canvas.drawRoundRect(stripRect, 6f, 6f, paints.borderStroke)

    val colWidth = CONTENT_WIDTH / 9f
    CciScaleLevel.entries.forEachIndexed { index, level ->
      val colLeft = MARGIN + index * colWidth
      if (index > 0) {
        canvas.drawLine(colLeft, y, colLeft, y + stripHeight, paints.borderStroke)
      }
      val centerX = colLeft + colWidth / 2f
      canvas.drawText(level.score.toString(), centerX, y + 14f, paints.scaleNumberCentered)
      val label =
        when (level) {
          CciScaleLevel.ABSOLUTELY_NONE -> "None"
          else -> level.label
        }
      canvas.drawText(label, centerX, y + 27f, paints.scaleLabelCentered)
    }

    return y + stripHeight + 14f
  }

  private fun drawContinuationHeader(
    canvas: Canvas,
    paints: PdfPaints,
    insights: InsightsSummary,
  ): Float {
    val y = MARGIN
    canvas.drawText(
      "Behavioural Activation Worksheet (${insights.timeRange.label}) — continued",
      MARGIN,
      y + 11f,
      paints.subtitle,
    )
    canvas.drawLine(MARGIN, y + 18f, MARGIN + CONTENT_WIDTH, y + 18f, paints.borderStroke)
    return y + 28f
  }

  private fun measureSummaryBoxHeight(
    paints: PdfPaints,
    insights: InsightsSummary,
  ): Float {
    val footnote = bothModeFootnote(insights.deltaBreakdown.countBothInFunAndAchievement)
    val footnoteLayout = buildLayout(footnote, paints.smallMuted, (CONTENT_WIDTH - 24f).toInt())
    // Header + balance line + 4 table rows + footnote + padding
    return 12f + 16f + 16f + 16f + (4 * 16f) + 8f + footnoteLayout.height + 12f
  }

  private fun drawSummaryBox(
    canvas: Canvas,
    paints: PdfPaints,
    insights: InsightsSummary,
    startY: Float,
  ): Float {
    val boxHeight = measureSummaryBoxHeight(paints, insights)
    val rect = RectF(MARGIN, startY, MARGIN + CONTENT_WIDTH, startY + boxHeight)
    canvas.drawRoundRect(rect, 8f, 8f, paints.accentFill)
    canvas.drawRoundRect(rect, 8f, 8f, paints.borderStroke)

    var y = startY + 14f
    val left = MARGIN + 12f
    canvas.drawText("Summary & Average Mood Shifts", left, y + 10f, paints.sectionHeader)
    y += 16f

    canvas.drawText(formatBalanceSummaryLine(insights), left, y + 10f, paints.bodyRegular)
    y += 18f

    // Compact table header
    val colGroup = left
    val colCount = left + 165f
    val colDep = left + 235f
    val colPle = left + 335f
    val colAch = left + 435f

    canvas.drawText("Category", colGroup, y + 10f, paints.tableHeader)
    canvas.drawText("Count", colCount, y + 10f, paints.tableHeader)
    canvas.drawText("Avg Dep", colDep, y + 10f, paints.tableHeader)
    canvas.drawText("Avg Pleasure", colPle, y + 10f, paints.tableHeader)
    canvas.drawText("Avg Achiev.", colAch, y + 10f, paints.tableHeader)
    y += 14f
    canvas.drawLine(left, y, MARGIN + CONTENT_WIDTH - 12f, y, paints.borderStroke)
    y += 3f

    val breakdown = insights.deltaBreakdown
    val rows = listOf(
      "Overall" to breakdown.overall,
      "Pleasure / Fun" to breakdown.pleasure,
      "Achievement" to breakdown.achievement,
      "Both / Mixed" to breakdown.both,
    )
    for ((label, stats) in rows) {
      drawStatsRow(canvas, paints, label, stats, colGroup, colCount, colDep, colPle, colAch, y)
      y += 16f
    }

    y += 5f
    val footnoteLayout = buildLayout(
      bothModeFootnote(breakdown.countBothInFunAndAchievement),
      paints.smallMuted,
      (CONTENT_WIDTH - 24f).toInt(),
    )
    footnoteLayout.drawAt(canvas, left, y)

    return startY + boxHeight
  }

  private fun drawStatsRow(
    canvas: Canvas,
    paints: PdfPaints,
    label: String,
    stats: AverageDeltaStats,
    colGroup: Float,
    colCount: Float,
    colDep: Float,
    colPle: Float,
    colAch: Float,
    y: Float,
  ) {
    val baseline = y + 11f
    canvas.drawText(label, colGroup, baseline, paints.bodyRegular)
    canvas.drawText(stats.count.toString(), colCount, baseline, paints.bodyRegular)
    canvas.drawText(stats.avgDepressionDelta?.let(InsightsCalculator::formatSignedAverage) ?: "—", colDep, baseline, paints.bodyRegular)
    canvas.drawText(stats.avgPleasureDelta?.let(InsightsCalculator::formatSignedAverage) ?: "—", colPle, baseline, paints.bodyRegular)
    canvas.drawText(stats.avgAchievementDelta?.let(InsightsCalculator::formatSignedAverage) ?: "—", colAch, baseline, paints.bodyRegular)
  }

  private fun formatBalanceSummaryLine(insights: InsightsSummary): String {
    val balance = insights.balance
    return if (balance.countBothInFunAndAchievement) {
      val pcts = InsightsCalculator.roundedPercentages(listOf(balance.funFraction, balance.achievementFraction))
      val bothSuffix =
        if (balance.bothCount > 0) {
          " (${balance.bothCount} Both/Mixed counted in both)"
        } else {
          ""
        }
      "Fun vs. Achievement Balance: ${pcts[0]}% Fun · ${pcts[1]}% Achievement$bothSuffix"
    } else {
      val pcts =
        InsightsCalculator.roundedPercentages(
          listOf(balance.funFraction, balance.achievementFraction, balance.bothFraction),
        )
      "Fun vs. Achievement Balance: ${pcts[0]}% Fun (${balance.pleasureCount}) · ${pcts[1]}% Achievement (${balance.achievementCount}) · ${pcts[2]}% Both (${balance.bothCount})"
    }
  }

  private fun bothModeFootnote(countBothInFunAndAchievement: Boolean): String =
    if (countBothInFunAndAchievement) {
      "Note: Both / Mixed activities are included in Pleasure/Fun, Achievement, and Both averages."
    } else {
      "Note: Both / Mixed activities are counted separately in the Both average only."
    }

  private data class MeasuredActivityBlock(
    val dateLine: String,
    val metaLine: String,
    val titleLayout: StaticLayout,
    val noteLayout: StaticLayout?,
    val topRowHeight: Float,
    val totalHeight: Float,
  )

  private fun measureActivityBlock(
    item: ActivityWithLog,
    paints: PdfPaints,
    zoneId: ZoneId,
  ): MeasuredActivityBlock {
    val dateLine =
      ZonedDateTime
        .ofInstant(Instant.ofEpochMilli(InsightsCalculator.effectiveTimestampMs(item)), zoneId)
        .format(activityDateFormatter)
    val metaLine = "${item.activity.category.label} · ${item.activity.durationMinutes} min"
    val titleLayout = buildLayout(item.activity.title, paints.cardTitle, 232)
    val leftHeight = 14f + 14f + titleLayout.height + 16f
    val rightHeight = 68f
    val topRowHeight = maxOf(leftHeight, rightHeight)

    val note = item.log?.reflectionNote?.trim()?.takeIf { it.isNotEmpty() }
    val noteLayout =
      note?.let {
        buildLayout("What did you notice: $it", paints.bodyMuted, (CONTENT_WIDTH - 24f).toInt())
      }
    val noteSectionHeight = if (noteLayout != null) 12f + noteLayout.height else 0f
    val totalHeight = 12f + topRowHeight + noteSectionHeight + 12f

    return MeasuredActivityBlock(
      dateLine = dateLine,
      metaLine = metaLine,
      titleLayout = titleLayout,
      noteLayout = noteLayout,
      topRowHeight = topRowHeight,
      totalHeight = totalHeight,
    )
  }

  private fun drawActivityBlock(
    canvas: Canvas,
    paints: PdfPaints,
    item: ActivityWithLog,
    measured: MeasuredActivityBlock,
    startY: Float,
  ) {
    val log = item.log ?: return
    val delta = log.computeDeltaSummary() ?: return
    val postDep = log.postDepression ?: return
    val postPle = log.postPleasure ?: return
    val postAch = log.postAchievement ?: return

    val rect = RectF(MARGIN, startY, MARGIN + CONTENT_WIDTH, startY + measured.totalHeight)
    canvas.drawRoundRect(rect, 8f, 8f, paints.whiteFill)
    canvas.drawRoundRect(rect, 8f, 8f, paints.borderStroke)

    val leftX = MARGIN + 12f
    var y = startY + 12f
    canvas.drawText("Activity & Date:", leftX, y + 9f, paints.smallMuted)
    y += 13f
    canvas.drawText(measured.dateLine, leftX, y + 10f, paints.bodyRegular)
    y += 14f
    measured.titleLayout.drawAt(canvas, leftX, y)
    y += measured.titleLayout.height + 4f
    canvas.drawText(measured.metaLine, leftX, y + 9f, paints.smallMuted)

    // Right column: Before / After / Shift matrix
    val rightBaseX = MARGIN + 262f
    val colLabel = rightBaseX
    val colDep = rightBaseX + 78f
    val colPle = rightBaseX + 146f
    val colAch = rightBaseX + 216f
    var matrixY = startY + 14f

    canvas.drawText("Depression", colDep, matrixY + 9f, paints.tableHeader)
    canvas.drawText("Pleasure", colPle, matrixY + 9f, paints.tableHeader)
    canvas.drawText("Achiev.", colAch, matrixY + 9f, paints.tableHeader)
    matrixY += 15f

    canvas.drawText("Before:", colLabel, matrixY + 10f, paints.smallMuted)
    canvas.drawText(log.preDepression.toString(), colDep, matrixY + 10f, paints.bodyRegular)
    canvas.drawText(log.prePleasure.toString(), colPle, matrixY + 10f, paints.bodyRegular)
    canvas.drawText(log.preAchievement.toString(), colAch, matrixY + 10f, paints.bodyRegular)
    matrixY += 16f

    canvas.drawText("After:", colLabel, matrixY + 10f, paints.smallMuted)
    canvas.drawText(postDep.toString(), colDep, matrixY + 10f, paints.bodyRegular)
    canvas.drawText(postPle.toString(), colPle, matrixY + 10f, paints.bodyRegular)
    canvas.drawText(postAch.toString(), colAch, matrixY + 10f, paints.bodyRegular)
    matrixY += 16f

    canvas.drawText("Shift:", colLabel, matrixY + 10f, paints.smallMuted)
    canvas.drawText(formatSignedInt(delta.depressionDelta), colDep, matrixY + 10f, paints.sectionHeader)
    canvas.drawText(formatSignedInt(delta.pleasureDelta), colPle, matrixY + 10f, paints.sectionHeader)
    canvas.drawText(formatSignedInt(delta.achievementDelta), colAch, matrixY + 10f, paints.sectionHeader)

    val noteLayout = measured.noteLayout
    if (noteLayout != null) {
      val dividerY = startY + 12f + measured.topRowHeight + 4f
      canvas.drawLine(leftX, dividerY, MARGIN + CONTENT_WIDTH - 12f, dividerY, paints.borderStroke)
      noteLayout.drawAt(canvas, leftX, dividerY + 6f)
    }
  }

  private fun drawSessionReflectionBox(
    canvas: Canvas,
    paints: PdfPaints,
    startY: Float,
  ) {
    val y = startY + 8f
    canvas.drawText("What did you notice about yourself?", MARGIN, y + 11f, paints.sectionHeader)
    for (lineIdx in 1..3) {
      val lineY = y + 16f + lineIdx * 18f
      canvas.drawLine(MARGIN, lineY, MARGIN + CONTENT_WIDTH, lineY, paints.borderStroke)
    }
  }

  private fun drawFooter(
    canvas: Canvas,
    paints: PdfPaints,
    pageNumber: Int,
  ) {
    val y = PAGE_HEIGHT - 22f
    canvas.drawText("ActShift · CCI Behavioural Activation Framework (0–8 scale)", MARGIN, y, paints.smallMuted)
    val pageText = "Page $pageNumber"
    val width = paints.smallMuted.measureText(pageText)
    canvas.drawText(pageText, MARGIN + CONTENT_WIDTH - width, y, paints.smallMuted)
  }

  private fun buildLayout(
    text: String,
    paint: TextPaint,
    widthPx: Int,
  ): StaticLayout =
    StaticLayout.Builder
      .obtain(text, 0, text.length, paint, widthPx.coerceAtLeast(1))
      .setAlignment(Layout.Alignment.ALIGN_NORMAL)
      .setIncludePad(false)
      .build()

  private fun StaticLayout.drawAt(canvas: Canvas, x: Float, y: Float) {
    canvas.save()
    canvas.translate(x, y)
    draw(canvas)
    canvas.restore()
  }

  private fun formatSignedInt(value: Int): String =
    if (value > 0) "+$value" else value.toString()

  private class PdfPaints {
    val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = COLOR_INK
      textSize = 15f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val subtitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = COLOR_MUTED
      textSize = 9.5f
    }
    val sectionHeader = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = COLOR_ACCENT
      textSize = 10.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val cardTitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = COLOR_INK
      textSize = 11.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val tableHeader = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = COLOR_MUTED
      textSize = 9f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val bodyRegular = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = COLOR_INK
      textSize = 10f
    }
    val bodyMuted = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = COLOR_MUTED
      textSize = 9.5f
    }
    val smallMuted = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = COLOR_MUTED
      textSize = 8.5f
    }
    val scaleNumberCentered = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = COLOR_INK
      textSize = 10f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.CENTER
    }
    val scaleLabelCentered = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = COLOR_MUTED
      textSize = 7.2f
      textAlign = Paint.Align.CENTER
    }
    val borderStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = COLOR_BORDER
      style = Paint.Style.STROKE
      strokeWidth = 0.8f
    }
    val surfaceFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = COLOR_SURFACE_TINT
      style = Paint.Style.FILL
    }
    val accentFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = COLOR_ACCENT_TINT
      style = Paint.Style.FILL
    }
    val whiteFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0xFFFFFFFF.toInt()
      style = Paint.Style.FILL
    }
  }
}

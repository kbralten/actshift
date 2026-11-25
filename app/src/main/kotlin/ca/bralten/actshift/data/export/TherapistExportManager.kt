package ca.bralten.actshift.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import ca.bralten.actshift.domain.InsightsCalculator
import ca.bralten.actshift.domain.model.ActivityWithLog
import ca.bralten.actshift.domain.model.InsightsTimeRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ExportFormat(
  val label: String,
  val subtitle: String,
  val mimeType: String,
  val extension: String,
) {
  PDF(
    label = "PDF Worksheet",
    subtitle = "CCI-style Before & After layout with summary & reflections",
    mimeType = "application/pdf",
    extension = "pdf",
  ),
  CSV(
    label = "CSV Spreadsheet",
    subtitle = "Tabular scores, deltas, and reflection notes",
    mimeType = "text/csv",
    extension = "csv",
  ),
}

class TherapistExportManager(
  private val context: Context,
) {
  private val fileDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

  fun buildFileName(
    format: ExportFormat,
    range: InsightsTimeRange,
    nowMs: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
  ): String {
    val datePart = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMs), zoneId).format(fileDateFormatter)
    val rangeSlug =
      when (range) {
        InsightsTimeRange.LAST_7_DAYS -> "7d"
        InsightsTimeRange.LAST_30_DAYS -> "30d"
        InsightsTimeRange.ALL_TIME -> "all"
      }
    val prefix = if (format == ExportFormat.PDF) "actshift-worksheet" else "actshift-log"
    return "$prefix-$rangeSlug-$datePart.${format.extension}"
  }

  suspend fun exportToCacheForShare(
    allCompleted: List<ActivityWithLog>,
    format: ExportFormat,
    range: InsightsTimeRange,
    countBothInFunAndAchievement: Boolean,
    nowMs: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
  ): Uri =
    withContext(Dispatchers.IO) {
      val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
      exportDir.listFiles()?.forEach { it.delete() }
      val file = File(exportDir, buildFileName(format, range, nowMs, zoneId))
      file.outputStream().use { stream ->
        writeExport(
          outputStream = stream,
          allCompleted = allCompleted,
          format = format,
          range = range,
          countBothInFunAndAchievement = countBothInFunAndAchievement,
          nowMs = nowMs,
          zoneId = zoneId,
        )
      }
      FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
      )
    }

  suspend fun exportToUri(
    targetUri: Uri,
    allCompleted: List<ActivityWithLog>,
    format: ExportFormat,
    range: InsightsTimeRange,
    countBothInFunAndAchievement: Boolean,
    nowMs: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
  ) = withContext(Dispatchers.IO) {
    val stream =
      context.contentResolver.openOutputStream(targetUri)
        ?: error("Unable to open destination for export.")
    stream.use { output ->
      writeExport(
        outputStream = output,
        allCompleted = allCompleted,
        format = format,
        range = range,
        countBothInFunAndAchievement = countBothInFunAndAchievement,
        nowMs = nowMs,
        zoneId = zoneId,
      )
    }
  }

  fun createShareIntent(uri: Uri, format: ExportFormat): Intent {
    val sendIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = format.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Behavioural Activation Log (ActShift)")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    return Intent.createChooser(sendIntent, "Share ${format.label}")
  }

  private fun writeExport(
    outputStream: OutputStream,
    allCompleted: List<ActivityWithLog>,
    format: ExportFormat,
    range: InsightsTimeRange,
    countBothInFunAndAchievement: Boolean,
    nowMs: Long,
    zoneId: ZoneId,
  ) {
    val filtered = InsightsCalculator.filterByTimeRange(allCompleted, range, nowMs, zoneId)
    when (format) {
      ExportFormat.CSV -> CsvWorksheetExporter.writeToStream(filtered, outputStream, zoneId)
      ExportFormat.PDF -> {
        val summary =
          InsightsCalculator.compute(
            items = allCompleted,
            range = range,
            countBothInFunAndAchievement = countBothInFunAndAchievement,
            nowMs = nowMs,
            zoneId = zoneId,
          )
        PdfWorksheetExporter.writeToStream(
          items = filtered,
          insights = summary,
          outputStream = outputStream,
          zoneId = zoneId,
          generatedAtMs = nowMs,
        )
      }
    }
  }
}

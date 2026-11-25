package ca.bralten.actshift.data.export

import ca.bralten.actshift.domain.InsightsCalculator
import ca.bralten.actshift.domain.model.ActivityWithLog
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Exports completed Behavioural Activation logs as RFC 4180 CSV for therapists and spreadsheets.
 */
object CsvWorksheetExporter {
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
  private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

  private val HEADER =
    listOf(
      "Date",
      "Time",
      "Activity",
      "Category",
      "Duration (min)",
      "Before Depression",
      "After Depression",
      "Depression Shift",
      "Before Pleasure",
      "After Pleasure",
      "Pleasure Shift",
      "Before Achievement",
      "After Achievement",
      "Achievement Shift",
      "What Did You Notice?",
    ).joinToString(",")

  fun export(
    items: List<ActivityWithLog>,
    zoneId: ZoneId = ZoneId.systemDefault(),
  ): String {
    val sorted =
      items
        .filter { it.log?.computeDeltaSummary() != null }
        .sortedBy { InsightsCalculator.effectiveTimestampMs(it) }

    val builder = StringBuilder()
    builder.appendLine(HEADER)

    for (item in sorted) {
      val log = item.log ?: continue
      val delta = log.computeDeltaSummary() ?: continue
      val postDep = log.postDepression ?: continue
      val postPle = log.postPleasure ?: continue
      val postAch = log.postAchievement ?: continue

      val zoned =
        ZonedDateTime.ofInstant(
          Instant.ofEpochMilli(InsightsCalculator.effectiveTimestampMs(item)),
          zoneId,
        )

      val row =
        listOf(
          zoned.format(dateFormatter),
          zoned.format(timeFormatter),
          item.activity.title,
          item.activity.category.label,
          item.activity.durationMinutes.toString(),
          log.preDepression.toString(),
          postDep.toString(),
          formatSignedInt(delta.depressionDelta),
          log.prePleasure.toString(),
          postPle.toString(),
          formatSignedInt(delta.pleasureDelta),
          log.preAchievement.toString(),
          postAch.toString(),
          formatSignedInt(delta.achievementDelta),
          log.reflectionNote.orEmpty(),
        ).joinToString(",") { escapeCsvField(it) }

      builder.appendLine(row)
    }

    return builder.toString()
  }

  fun writeToStream(
    items: List<ActivityWithLog>,
    outputStream: OutputStream,
    zoneId: ZoneId = ZoneId.systemDefault(),
  ) {
    val csv = export(items, zoneId)
    outputStream.write(csv.toByteArray(StandardCharsets.UTF_8))
    outputStream.flush()
  }

  fun escapeCsvField(value: String): String {
    val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    if (!needsQuotes) return value
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
  }

  private fun formatSignedInt(value: Int): String =
    if (value > 0) "+$value" else value.toString()
}

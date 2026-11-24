package ca.bralten.actshift.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormatters {
  private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a", Locale.getDefault())
  private val worksheetDateFormatter = DateTimeFormatter.ofPattern("MMMM d · h:mm a", Locale.getDefault())
  private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

  fun formatActivityStart(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
    val zoned = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zoneId)
    val today = LocalDate.now(zoneId)
    val date = zoned.toLocalDate()
    return when (date) {
      today -> "Today · ${zoned.format(timeFormatter)}"
      today.plusDays(1) -> "Tomorrow · ${zoned.format(timeFormatter)}"
      today.minusDays(1) -> "Yesterday · ${zoned.format(timeFormatter)}"
      else -> zoned.format(dateTimeFormatter)
    }
  }

  fun formatWorksheetDate(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zoneId).format(worksheetDateFormatter)

  fun formatDate(date: LocalDate): String = date.format(dateFormatter)

  fun formatTime(time: LocalTime): String = time.format(timeFormatter)

  fun formatCountdown(remainingSeconds: Long): String {
    val clamped = remainingSeconds.coerceAtLeast(0L)
    val mins = clamped / 60L
    val secs = clamped % 60L
    return "%02d:%02d".format(mins, secs)
  }
}

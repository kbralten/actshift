package ca.bralten.actshift.domain

import ca.bralten.actshift.domain.model.RecurrenceType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Expands recurrence rules (ONCE, DAILY, WEEKDAYS, WEEKLY) into concrete epoch-ms timestamps
 * while preserving local wall-clock time across DST transitions.
 */
object RecurrenceExpander {
  fun expandOccurrences(
    startEpochMs: Long,
    recurrenceRule: String?,
    windowStartEpochMs: Long,
    windowEndEpochMs: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
  ): List<Long> {
    if (windowEndEpochMs < windowStartEpochMs) return emptyList()
    val anchor = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startEpochMs), zoneId)
    val (recurrenceType, weeklyDays) = RecurrenceType.fromRRule(recurrenceRule)

    if (recurrenceType == RecurrenceType.ONCE) {
      return if (startEpochMs in windowStartEpochMs..windowEndEpochMs) {
        listOf(startEpochMs)
      } else {
        emptyList()
      }
    }

    val windowStartDate = Instant.ofEpochMilli(windowStartEpochMs).atZone(zoneId).toLocalDate()
    val windowEndDate = Instant.ofEpochMilli(windowEndEpochMs).atZone(zoneId).toLocalDate()
    val anchorDate = anchor.toLocalDate()
    val localTime = anchor.toLocalTime()

    val firstDate: LocalDate = if (anchorDate.isAfter(windowStartDate)) anchorDate else windowStartDate
    if (firstDate.isAfter(windowEndDate)) return emptyList()

    val effectiveWeeklyDays =
      when (recurrenceType) {
        RecurrenceType.WEEKDAYS -> setOf(
          java.time.DayOfWeek.MONDAY,
          java.time.DayOfWeek.TUESDAY,
          java.time.DayOfWeek.WEDNESDAY,
          java.time.DayOfWeek.THURSDAY,
          java.time.DayOfWeek.FRIDAY,
        )
        RecurrenceType.WEEKLY -> weeklyDays.ifEmpty { setOf(anchor.dayOfWeek) }
        else -> emptySet()
      }

    val results = mutableListOf<Long>()
    var cursor = firstDate
    while (!cursor.isAfter(windowEndDate)) {
      val matches =
        when (recurrenceType) {
          RecurrenceType.ONCE -> false
          RecurrenceType.DAILY -> true
          RecurrenceType.WEEKDAYS, RecurrenceType.WEEKLY -> cursor.dayOfWeek in effectiveWeeklyDays
        }
      if (matches) {
        val candidateMs = ZonedDateTime.of(cursor, localTime, zoneId).toInstant().toEpochMilli()
        if (candidateMs >= startEpochMs && candidateMs in windowStartEpochMs..windowEndEpochMs) {
          results.add(candidateMs)
        }
      }
      cursor = cursor.plusDays(1)
    }
    return results
  }
}

package ca.bralten.actshift.domain

import ca.bralten.actshift.domain.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class RecurrenceExpanderTest {

  private val toronto = ZoneId.of("America/Toronto")

  @Test
  fun onceReturnsSingleOccurrenceInsideWindow() {
    val start = ZonedDateTime.of(2026, 11, 24, 9, 0, 0, 0, toronto).toInstant().toEpochMilli()
    val windowStart = start - 60_000L
    val windowEnd = start + 86_400_000L

    val results = RecurrenceExpander.expandOccurrences(start, null, windowStart, windowEnd, toronto)
    assertEquals(listOf(start), results)
  }

  @Test
  fun dailyPreservesLocalWallClockAcrossDstTransition() {
    // 2026-10-31 to 2026-11-03 crosses North American fall-back DST on Nov 1, 2026
    val start = ZonedDateTime.of(2026, 10, 31, 9, 30, 0, 0, toronto).toInstant().toEpochMilli()
    val windowEnd = ZonedDateTime.of(2026, 11, 3, 23, 59, 0, 0, toronto).toInstant().toEpochMilli()

    val results =
      RecurrenceExpander.expandOccurrences(
        startEpochMs = start,
        recurrenceRule = RecurrenceType.DAILY.toRRule(),
        windowStartEpochMs = start,
        windowEndEpochMs = windowEnd,
        zoneId = toronto,
      )

    assertEquals(4, results.size)
    results.forEach { epoch ->
      val zdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(epoch), toronto)
      assertEquals(9, zdt.hour)
      assertEquals(30, zdt.minute)
    }
  }

  @Test
  fun weekdaysSkipsSaturdayAndSunday() {
    // 2026-11-27 is Friday -> Mon Nov 30
    val start = ZonedDateTime.of(2026, 11, 27, 10, 0, 0, 0, toronto).toInstant().toEpochMilli()
    val end = ZonedDateTime.of(2026, 12, 1, 23, 59, 0, 0, toronto).toInstant().toEpochMilli()

    val results =
      RecurrenceExpander.expandOccurrences(
        startEpochMs = start,
        recurrenceRule = RecurrenceType.WEEKDAYS.toRRule(),
        windowStartEpochMs = start,
        windowEndEpochMs = end,
        zoneId = toronto,
      )

    val days =
      results.map {
        ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), toronto).dayOfWeek
      }
    assertEquals(listOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY), days)
  }

  @Test
  fun weeklyMatchesConfiguredByDay() {
    val start = ZonedDateTime.of(2026, 11, 23, 14, 0, 0, 0, toronto).toInstant().toEpochMilli() // Monday
    val end = ZonedDateTime.of(2026, 12, 7, 23, 59, 0, 0, toronto).toInstant().toEpochMilli()

    val rrule = RecurrenceType.WEEKLY.toRRule(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY))
    val results = RecurrenceExpander.expandOccurrences(start, rrule, start, end, toronto)
    val days =
      results.map {
        ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), toronto).dayOfWeek
      }
    assertEquals(
      listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
      days,
    )
  }
}

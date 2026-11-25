package ca.bralten.actshift.domain

import ca.bralten.actshift.domain.model.ScheduleTimeGranularity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class QuickTimePresetsTest {

  private val toronto = ZoneId.of("America/Toronto")

  @Test
  fun nextWholeHourAndClosestWholeHourAt1037() {
    val now = ZonedDateTime.of(2026, 11, 24, 10, 37, 15, 0, toronto)
    assertEquals(
      ZonedDateTime.of(2026, 11, 24, 11, 0, 0, 0, toronto),
      QuickTimePresets.nextWholeHour(now),
    )
    assertEquals(
      ZonedDateTime.of(2026, 11, 24, 12, 0, 0, 0, toronto),
      QuickTimePresets.closestWholeHourToOneHour(now),
    )

    val presets = QuickTimePresets.buildQuickPresets(now)
    assertTrue(presets.any { it.label.startsWith("Next hour (11:00)") })
    assertTrue(presets.any { it.label.startsWith("About 1h (12:00)") })
  }

  @Test
  fun closestWholeHourAt1020OmitsDuplicateAbout1hPreset() {
    val now = ZonedDateTime.of(2026, 11, 24, 10, 20, 0, 0, toronto)
    assertEquals(
      ZonedDateTime.of(2026, 11, 24, 11, 0, 0, 0, toronto),
      QuickTimePresets.nextWholeHour(now),
    )
    assertEquals(
      ZonedDateTime.of(2026, 11, 24, 11, 0, 0, 0, toronto),
      QuickTimePresets.closestWholeHourToOneHour(now),
    )

    val presets = QuickTimePresets.buildQuickPresets(now)
    assertTrue(presets.any { it.label.startsWith("Next hour (11:00)") })
    assertFalse(presets.any { it.label.startsWith("About 1h") })
  }

  @Test
  fun closestWholeHourBreaksTieForwardAt1030AndHandlesTopOfHour() {
    val at1030 = ZonedDateTime.of(2026, 11, 24, 10, 30, 0, 0, toronto)
    assertEquals(
      ZonedDateTime.of(2026, 11, 24, 12, 0, 0, 0, toronto),
      QuickTimePresets.closestWholeHourToOneHour(at1030),
    )

    val at1000 = ZonedDateTime.of(2026, 11, 24, 10, 0, 0, 0, toronto)
    assertEquals(
      ZonedDateTime.of(2026, 11, 24, 11, 0, 0, 0, toronto),
      QuickTimePresets.nextWholeHour(at1000),
    )
    assertEquals(
      ZonedDateTime.of(2026, 11, 24, 11, 0, 0, 0, toronto),
      QuickTimePresets.closestWholeHourToOneHour(at1000),
    )
  }

  @Test
  fun midnightRolloverAt2337() {
    val at2337 = ZonedDateTime.of(2026, 11, 24, 23, 37, 0, 0, toronto)
    assertEquals(
      ZonedDateTime.of(2026, 11, 25, 0, 0, 0, 0, toronto),
      QuickTimePresets.nextWholeHour(at2337),
    )
    assertEquals(
      ZonedDateTime.of(2026, 11, 25, 1, 0, 0, 0, toronto),
      QuickTimePresets.closestWholeHourToOneHour(at2337),
    )
  }

  @Test
  fun halfHourGranularityComputesNext30mAndAbout30mCorrectly() {
    val at1010 = ZonedDateTime.of(2026, 11, 24, 10, 10, 0, 0, toronto)
    assertEquals(
      ZonedDateTime.of(2026, 11, 24, 10, 30, 0, 0, toronto),
      QuickTimePresets.nextHalfHour(at1010),
    )
    assertEquals(
      ZonedDateTime.of(2026, 11, 24, 10, 30, 0, 0, toronto),
      QuickTimePresets.closestHalfHourToThirtyMinutes(at1010),
    )
    val presetsAt1010 = QuickTimePresets.buildQuickPresets(at1010, ScheduleTimeGranularity.HALF_HOUR)
    assertTrue(presetsAt1010.any { it.label.startsWith("Next 30m (10:30)") })
    assertFalse(presetsAt1010.any { it.label.startsWith("About 30m") })
    assertTrue(presetsAt1010.any { it.label == "In 30m" })

    val at1020 = ZonedDateTime.of(2026, 11, 24, 10, 20, 0, 0, toronto)
    assertEquals(
      ZonedDateTime.of(2026, 11, 24, 10, 30, 0, 0, toronto),
      QuickTimePresets.nextHalfHour(at1020),
    )
    assertEquals(
      ZonedDateTime.of(2026, 11, 24, 11, 0, 0, 0, toronto),
      QuickTimePresets.closestHalfHourToThirtyMinutes(at1020),
    )
    val presetsAt1020 = QuickTimePresets.buildQuickPresets(at1020, ScheduleTimeGranularity.HALF_HOUR)
    assertTrue(presetsAt1020.any { it.label.startsWith("Next 30m (10:30)") })
    assertTrue(presetsAt1020.any { it.label.startsWith("About 30m (11:00)") })

    val at2337 = ZonedDateTime.of(2026, 11, 24, 23, 37, 0, 0, toronto)
    assertEquals(
      ZonedDateTime.of(2026, 11, 25, 0, 0, 0, 0, toronto),
      QuickTimePresets.nextHalfHour(at2337),
    )

    assertEquals("Next Hour", QuickTimePresets.nextSlotButtonLabel(ScheduleTimeGranularity.HOUR))
    assertEquals("Next 30m", QuickTimePresets.nextSlotButtonLabel(ScheduleTimeGranularity.HALF_HOUR))
    assertEquals(
      ZonedDateTime.of(2026, 11, 24, 11, 0, 0, 0, toronto),
      QuickTimePresets.nextSlotDateTime(at1010, ScheduleTimeGranularity.HOUR),
    )
    assertEquals(
      ZonedDateTime.of(2026, 11, 24, 10, 30, 0, 0, toronto),
      QuickTimePresets.nextSlotDateTime(at1010, ScheduleTimeGranularity.HALF_HOUR),
    )
  }
}

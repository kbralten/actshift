package ca.bralten.actshift.domain

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
}

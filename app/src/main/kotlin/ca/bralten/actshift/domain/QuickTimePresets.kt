package ca.bralten.actshift.domain

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class QuickTimePreset(
  val label: String,
  val targetDateTime: ZonedDateTime,
)

/**
 * Computes calm, predictable quick-pick start times including whole-hour anchors.
 */
object QuickTimePresets {
  private val compactHourFormatter = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())

  fun nextWholeHour(now: ZonedDateTime): ZonedDateTime =
    now.truncatedTo(ChronoUnit.HOURS).plusHours(1)

  fun closestWholeHourToOneHour(now: ZonedDateTime): ZonedDateTime {
    val oneHourFromNow = now.plusMinutes(60)
    val floorHour = oneHourFromNow.truncatedTo(ChronoUnit.HOURS)
    val ceilHour = floorHour.plusHours(1)
    val distToFloor = Duration.between(floorHour, oneHourFromNow).abs()
    val distToCeil = Duration.between(oneHourFromNow, ceilHour).abs()
    return if (distToCeil <= distToFloor) ceilHour else floorHour
  }

  fun buildQuickPresets(now: ZonedDateTime): List<QuickTimePreset> {
    val in15m = now.plusMinutes(15).truncatedTo(ChronoUnit.MINUTES)
    val nextHour = nextWholeHour(now)
    val aboutHour = closestWholeHourToOneHour(now)
    val in1h = now.plusMinutes(60).truncatedTo(ChronoUnit.MINUTES)
    val tomorrowMorning =
      ZonedDateTime.of(now.toLocalDate().plusDays(1), LocalTime.of(9, 0), now.zone)

    return buildList {
      add(QuickTimePreset("In 15m", in15m))
      add(QuickTimePreset("Next hour (${formatCompactTime(nextHour)})", nextHour))
      if (aboutHour != nextHour) {
        add(QuickTimePreset("About 1h (${formatCompactTime(aboutHour)})", aboutHour))
      }
      add(QuickTimePreset("In 1h", in1h))
      add(QuickTimePreset("Tomorrow 9am", tomorrowMorning))
    }
  }

  private fun formatCompactTime(dateTime: ZonedDateTime): String =
    dateTime.format(compactHourFormatter)
}

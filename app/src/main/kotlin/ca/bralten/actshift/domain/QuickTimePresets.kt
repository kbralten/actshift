package ca.bralten.actshift.domain

import ca.bralten.actshift.domain.model.ScheduleTimeGranularity
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
 * Computes calm, predictable quick-pick start times including whole-hour or half-hour anchors.
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

  fun nextHalfHour(now: ZonedDateTime): ZonedDateTime {
    val truncated = now.truncatedTo(ChronoUnit.MINUTES)
    return if (truncated.minute < 30) {
      truncated.withMinute(30)
    } else {
      truncated.truncatedTo(ChronoUnit.HOURS).plusHours(1)
    }
  }

  fun closestHalfHourToThirtyMinutes(now: ZonedDateTime): ZonedDateTime {
    val thirtyFromNow = now.plusMinutes(30).truncatedTo(ChronoUnit.MINUTES)
    val floorSlot =
      if (thirtyFromNow.minute < 30) {
        thirtyFromNow.withMinute(0)
      } else {
        thirtyFromNow.withMinute(30)
      }
    val ceilSlot = floorSlot.plusMinutes(30)
    val distToFloor = Duration.between(floorSlot, thirtyFromNow).abs()
    val distToCeil = Duration.between(thirtyFromNow, ceilSlot).abs()
    return if (distToCeil <= distToFloor) ceilSlot else floorSlot
  }

  fun nextSlotDateTime(
    now: ZonedDateTime,
    granularity: ScheduleTimeGranularity,
  ): ZonedDateTime =
    when (granularity) {
      ScheduleTimeGranularity.HOUR -> nextWholeHour(now)
      ScheduleTimeGranularity.HALF_HOUR -> nextHalfHour(now)
    }

  fun nextSlotButtonLabel(granularity: ScheduleTimeGranularity): String =
    when (granularity) {
      ScheduleTimeGranularity.HOUR -> "Next Hour"
      ScheduleTimeGranularity.HALF_HOUR -> "Next 30m"
    }

  fun buildQuickPresets(
    now: ZonedDateTime,
    granularity: ScheduleTimeGranularity = ScheduleTimeGranularity.HOUR,
  ): List<QuickTimePreset> {
    val in15m = now.plusMinutes(15).truncatedTo(ChronoUnit.MINUTES)
    val tomorrowMorning =
      ZonedDateTime.of(now.toLocalDate().plusDays(1), LocalTime.of(9, 0), now.zone)

    return when (granularity) {
      ScheduleTimeGranularity.HOUR -> {
        val nextHour = nextWholeHour(now)
        val aboutHour = closestWholeHourToOneHour(now)
        val in1h = now.plusMinutes(60).truncatedTo(ChronoUnit.MINUTES)
        buildList {
          add(QuickTimePreset("In 15m", in15m))
          add(QuickTimePreset("Next hour (${formatCompactTime(nextHour)})", nextHour))
          if (aboutHour != nextHour) {
            add(QuickTimePreset("About 1h (${formatCompactTime(aboutHour)})", aboutHour))
          }
          add(QuickTimePreset("In 1h", in1h))
          add(QuickTimePreset("Tomorrow 9am", tomorrowMorning))
        }
      }

      ScheduleTimeGranularity.HALF_HOUR -> {
        val nextHalf = nextHalfHour(now)
        val aboutHalf = closestHalfHourToThirtyMinutes(now)
        val in30m = now.plusMinutes(30).truncatedTo(ChronoUnit.MINUTES)
        buildList {
          add(QuickTimePreset("In 15m", in15m))
          add(QuickTimePreset("Next 30m (${formatCompactTime(nextHalf)})", nextHalf))
          if (aboutHalf != nextHalf) {
            add(QuickTimePreset("About 30m (${formatCompactTime(aboutHalf)})", aboutHalf))
          }
          add(QuickTimePreset("In 30m", in30m))
          add(QuickTimePreset("Tomorrow 9am", tomorrowMorning))
        }
      }
    }
  }

  private fun formatCompactTime(dateTime: ZonedDateTime): String =
    dateTime.format(compactHourFormatter)
}

package ca.bralten.actshift.widget

import ca.bralten.actshift.domain.model.ActivityStatus
import ca.bralten.actshift.domain.model.ScheduledActivity
import ca.bralten.actshift.ui.components.TimeFormatters
import java.time.ZoneId

enum class WidgetMode {
  IN_PROGRESS,
  NEXT_SCHEDULED,
  EMPTY,
}

enum class WidgetActionType {
  START_PRE_CHECKIN,
  POST_CHECKIN,
  OPEN_TIMER,
  SNOOZE_30M,
  OPEN_QUICK_PICK,
  OPEN_NEW_ACTIVITY,
  OPEN_APP,
}

data class WidgetAction(
  val label: String,
  val type: WidgetActionType,
  val activityId: Long = -1L,
)

data class WidgetSnapshot(
  val mode: WidgetMode,
  val badgeText: String,
  val title: String,
  val subtitle: String,
  val primaryAction: WidgetAction,
  val secondaryAction: WidgetAction,
)

/**
 * Pure-Kotlin builder mapping upcoming/active activities to home screen widget state.
 */
object WidgetSnapshotBuilder {

  fun build(
    upcomingAndActive: List<ScheduledActivity>,
    nowMs: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
  ): WidgetSnapshot {
    val active =
      upcomingAndActive
        .filter { it.status == ActivityStatus.IN_PROGRESS }
        .minByOrNull { it.startedAtEpochMs ?: it.scheduledStartEpochMs }

    if (active != null) {
      val timerEnd = active.timerEndEpochMs
      val subtitle =
        if (timerEnd != null && timerEnd > nowMs) {
          val remainingMins = ((timerEnd - nowMs + 59_999L) / 60_000L).coerceAtLeast(1L)
          "Timer running · ${remainingMins}m left · ${active.category.label}"
        } else {
          "Check-in started · ${active.durationMinutes}m · ${active.category.label}"
        }
      val secondary =
        if (timerEnd != null) {
          WidgetAction("Open Timer", WidgetActionType.OPEN_TIMER, active.id)
        } else {
          WidgetAction("Open App", WidgetActionType.OPEN_APP)
        }
      return WidgetSnapshot(
        mode = WidgetMode.IN_PROGRESS,
        badgeText = "IN PROGRESS",
        title = active.title,
        subtitle = subtitle,
        primaryAction = WidgetAction("I'm Done", WidgetActionType.POST_CHECKIN, active.id),
        secondaryAction = secondary,
      )
    }

    val next =
      upcomingAndActive
        .filter { it.status == ActivityStatus.SCHEDULED }
        .minByOrNull { it.scheduledStartEpochMs }

    if (next != null) {
      return WidgetSnapshot(
        mode = WidgetMode.NEXT_SCHEDULED,
        badgeText = "NEXT ACTIVATION",
        title = next.title,
        subtitle = "${TimeFormatters.formatActivityStart(next.scheduledStartEpochMs, zoneId)} · ${next.durationMinutes}m · ${next.category.label}",
        primaryAction = WidgetAction("Start Check-In", WidgetActionType.START_PRE_CHECKIN, next.id),
        secondaryAction = WidgetAction("Snooze 30m", WidgetActionType.SNOOZE_30M, next.id),
      )
    }

    return WidgetSnapshot(
      mode = WidgetMode.EMPTY,
      badgeText = "ACTSHIFT",
      title = "No upcoming activation",
      subtitle = "Pick a gentle 5–15 min activity to shift your mood.",
      primaryAction = WidgetAction("Quick-Pick Idea", WidgetActionType.OPEN_QUICK_PICK),
      secondaryAction = WidgetAction("Schedule", WidgetActionType.OPEN_NEW_ACTIVITY),
    )
  }
}

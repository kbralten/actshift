package ca.bralten.actshift.domain

import ca.bralten.actshift.domain.model.ActivationLog
import ca.bralten.actshift.domain.model.ActivityStatus
import ca.bralten.actshift.domain.model.ScheduledActivity

/**
 * Time-based rules for gently transitioning missed activities and prompting for abandoned check-ins.
 */
object AbandonedActivityPolicy {
  const val ABANDONED_THRESHOLD_MS: Long = 12L * 60 * 60 * 1000
  const val UNCOMPLETED_GRACE_MS: Long = 2L * 60 * 60 * 1000

  fun abandonedCutoffEpochMs(nowMs: Long): Long = nowMs - ABANDONED_THRESHOLD_MS

  fun isAbandonedLog(log: ActivationLog, nowMs: Long): Boolean =
    !log.isComplete && (nowMs - log.preLoggedAtEpochMs) >= ABANDONED_THRESHOLD_MS

  fun isExpiredScheduled(activity: ScheduledActivity, nowMs: Long): Boolean =
    activity.status == ActivityStatus.SCHEDULED &&
      nowMs >= (activity.scheduledEndEpochMs + UNCOMPLETED_GRACE_MS)

  fun filterAbandonedLogs(logs: List<ActivationLog>, nowMs: Long): List<ActivationLog> =
    logs.filter { isAbandonedLog(it, nowMs) }

  fun filterExpiredScheduled(
    activities: List<ScheduledActivity>,
    nowMs: Long,
  ): List<ScheduledActivity> = activities.filter { isExpiredScheduled(it, nowMs) }
}

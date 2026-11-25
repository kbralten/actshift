package ca.bralten.actshift.domain

import ca.bralten.actshift.domain.model.ActivationLog
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.ActivityStatus
import ca.bralten.actshift.domain.model.ScheduledActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AbandonedActivityPolicyTest {

  @Test
  fun identifiesAbandonedLogAfterTwelveHoursWithoutPostCheckIn() {
    val now = 100_000_000L
    val elevenHoursAgo = now - (11L * 60 * 60 * 1000)
    val thirteenHoursAgo = now - (13L * 60 * 60 * 1000)

    val recentIncomplete = ActivationLog(1L, 1L, 5, 1, 1, elevenHoursAgo, null, null, null, null, null)
    val oldIncomplete = ActivationLog(2L, 2L, 5, 1, 1, thirteenHoursAgo, null, null, null, null, null)
    val oldComplete = ActivationLog(3L, 3L, 5, 1, 1, thirteenHoursAgo, 3, 2, 3, thirteenHoursAgo + 600_000L, null)

    assertFalse(AbandonedActivityPolicy.isAbandonedLog(recentIncomplete, now))
    assertTrue(AbandonedActivityPolicy.isAbandonedLog(oldIncomplete, now))
    assertFalse(AbandonedActivityPolicy.isAbandonedLog(oldComplete, now))
  }

  @Test
  fun identifiesExpiredScheduledActivityEightHoursPastEndAndPastScheduledEnd() {
    val start = 10_000_000L
    val durationMin = 10 // end = start + 600_000
    val activity = ScheduledActivity(
      id = 1L,
      templateId = null,
      title = "Get the mail",
      category = ActivityCategory.ACHIEVEMENT,
      scheduledStartEpochMs = start,
      durationMinutes = durationMin,
      status = ActivityStatus.SCHEDULED,
      calendarEventId = null,
      calendarInstanceBeginMs = null,
      startedAtEpochMs = null,
      timerEndEpochMs = null,
      completedAtEpochMs = null,
      snoozeCount = 0,
    )

    val beforeEnd = activity.scheduledEndEpochMs - 1L
    val atEnd = activity.scheduledEndEpochMs
    val sevenHoursAfterEnd = activity.scheduledEndEpochMs + 7L * 60 * 60 * 1000
    val eightHoursAfterEnd = activity.scheduledEndEpochMs + 8L * 60 * 60 * 1000

    assertFalse(AbandonedActivityPolicy.isPastScheduledEnd(activity, beforeEnd))
    assertTrue(AbandonedActivityPolicy.isPastScheduledEnd(activity, atEnd))
    assertTrue(AbandonedActivityPolicy.isPastScheduledEnd(activity, sevenHoursAfterEnd))
    assertFalse(AbandonedActivityPolicy.isPastScheduledEnd(activity.copy(status = ActivityStatus.IN_PROGRESS), atEnd))
    assertFalse(AbandonedActivityPolicy.isPastScheduledEnd(activity.copy(status = ActivityStatus.COMPLETED), atEnd))

    assertFalse(AbandonedActivityPolicy.isExpiredScheduled(activity, sevenHoursAfterEnd))
    assertTrue(AbandonedActivityPolicy.isExpiredScheduled(activity, eightHoursAfterEnd))
    assertFalse(
      AbandonedActivityPolicy.isExpiredScheduled(
        activity.copy(status = ActivityStatus.IN_PROGRESS),
        eightHoursAfterEnd,
      ),
    )
  }
}

package ca.bralten.actshift.widget

import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.ActivityStatus
import ca.bralten.actshift.domain.model.ScheduledActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class WidgetSnapshotBuilderTest {

  private val utc = ZoneId.of("UTC")

  @Test
  fun prioritizesInProgressWithActiveTimerOverScheduled() {
    val nowMs = 1_000_000L
    val active =
      sampleActivity(
        id = 7L,
        title = "Walk the dog",
        category = ActivityCategory.BOTH,
        status = ActivityStatus.IN_PROGRESS,
        startMs = nowMs - 120_000L,
        startedAtMs = nowMs - 60_000L,
        timerEndMs = nowMs + 8L * 60_000L + 5_000L, // ~8m 5s -> ceil 9m left
      )
    val upcoming =
      sampleActivity(
        id = 8L,
        title = "Get the mail",
        category = ActivityCategory.ACHIEVEMENT,
        status = ActivityStatus.SCHEDULED,
        startMs = nowMs + 30L * 60_000L,
      )

    val snapshot = WidgetSnapshotBuilder.build(listOf(upcoming, active), nowMs = nowMs, zoneId = utc)
    assertEquals(WidgetMode.IN_PROGRESS, snapshot.mode)
    assertEquals("IN PROGRESS", snapshot.badgeText)
    assertEquals("Walk the dog", snapshot.title)
    assertTrue(snapshot.subtitle.contains("9m left"))
    assertEquals(WidgetActionType.POST_CHECKIN, snapshot.primaryAction.type)
    assertEquals(7L, snapshot.primaryAction.activityId)
    assertEquals(WidgetActionType.OPEN_TIMER, snapshot.secondaryAction.type)
    assertEquals(7L, snapshot.secondaryAction.activityId)
  }

  @Test
  fun inProgressWithoutTimerRoutesSecondaryActionToOpenApp() {
    val nowMs = 1_000_000L
    val active =
      sampleActivity(
        id = 5L,
        title = "Sit in sun",
        category = ActivityCategory.PLEASURE,
        status = ActivityStatus.IN_PROGRESS,
        startMs = nowMs - 60_000L,
        startedAtMs = nowMs - 60_000L,
        timerEndMs = null,
      )

    val snapshot = WidgetSnapshotBuilder.build(listOf(active), nowMs = nowMs, zoneId = utc)
    assertEquals(WidgetMode.IN_PROGRESS, snapshot.mode)
    assertTrue(snapshot.subtitle.contains("Check-in started"))
    assertEquals(WidgetActionType.POST_CHECKIN, snapshot.primaryAction.type)
    assertEquals(WidgetActionType.OPEN_APP, snapshot.secondaryAction.type)
  }

  @Test
  fun selectsEarliestScheduledActivityWhenNoneInProgress() {
    val base = ZonedDateTime.of(2026, 11, 25, 10, 0, 0, 0, utc).toInstant().toEpochMilli()
    val later = sampleActivity(2L, "Later task", ActivityCategory.ACHIEVEMENT, ActivityStatus.SCHEDULED, base + 3_600_000L)
    val earlier = sampleActivity(1L, "Drink water", ActivityCategory.ACHIEVEMENT, ActivityStatus.SCHEDULED, base + 600_000L)

    val snapshot = WidgetSnapshotBuilder.build(listOf(later, earlier), nowMs = base, zoneId = utc)
    assertEquals(WidgetMode.NEXT_SCHEDULED, snapshot.mode)
    assertEquals("NEXT ACTIVATION", snapshot.badgeText)
    assertEquals("Drink water", snapshot.title)
    assertTrue(snapshot.subtitle.contains("10m"))
    assertTrue(snapshot.subtitle.contains("Achievement"))
    assertEquals(WidgetActionType.START_PRE_CHECKIN, snapshot.primaryAction.type)
    assertEquals(1L, snapshot.primaryAction.activityId)
    assertEquals(WidgetActionType.SNOOZE_30M, snapshot.secondaryAction.type)
    assertEquals(1L, snapshot.secondaryAction.activityId)
  }

  @Test
  fun returnsEmptyStateWithQuickPickAndScheduleActions() {
    val snapshot = WidgetSnapshotBuilder.build(emptyList(), nowMs = 1_000L, zoneId = utc)
    assertEquals(WidgetMode.EMPTY, snapshot.mode)
    assertEquals("No upcoming activation", snapshot.title)
    assertEquals(WidgetActionType.OPEN_QUICK_PICK, snapshot.primaryAction.type)
    assertEquals(WidgetActionType.OPEN_NEW_ACTIVITY, snapshot.secondaryAction.type)
  }

  private fun sampleActivity(
    id: Long,
    title: String,
    category: ActivityCategory,
    status: ActivityStatus,
    startMs: Long,
    startedAtMs: Long? = null,
    timerEndMs: Long? = null,
  ): ScheduledActivity =
    ScheduledActivity(
      id = id,
      templateId = null,
      title = title,
      category = category,
      scheduledStartEpochMs = startMs,
      durationMinutes = 10,
      status = status,
      calendarEventId = null,
      calendarInstanceBeginMs = null,
      startedAtEpochMs = startedAtMs,
      timerEndEpochMs = timerEndMs,
      completedAtEpochMs = null,
      snoozeCount = 0,
    )
}

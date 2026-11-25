package ca.bralten.actshift.data.repository

import ca.bralten.actshift.data.calendar.CalendarSyncEngine
import ca.bralten.actshift.data.local.dao.ActivationLogDao
import ca.bralten.actshift.data.local.dao.ActivityTemplateDao
import ca.bralten.actshift.data.local.dao.ScheduledActivityDao
import ca.bralten.actshift.data.local.entity.ActivationLogEntity
import ca.bralten.actshift.data.local.entity.ActivityTemplateEntity
import ca.bralten.actshift.data.local.entity.ScheduledActivityEntity
import ca.bralten.actshift.domain.AbandonedActivityPolicy
import ca.bralten.actshift.domain.RecurrenceExpander
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.ActivityStatus
import ca.bralten.actshift.domain.model.ActivityWithLog
import ca.bralten.actshift.domain.model.RecurrenceType
import ca.bralten.actshift.domain.model.ScheduledActivity
import ca.bralten.actshift.notifications.AlarmScheduler
import ca.bralten.actshift.notifications.NotificationHelper
import ca.bralten.actshift.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Single source of truth coordinating Room persistence, dedicated calendar sync, and alarms.
 */
class ActivationRepository(
  private val templateDao: ActivityTemplateDao,
  private val scheduledActivityDao: ScheduledActivityDao,
  private val activationLogDao: ActivationLogDao,
  private val calendarSyncEngine: CalendarSyncEngine,
  private val alarmScheduler: AlarmScheduler,
  private val notificationHelper: NotificationHelper,
  private val widgetUpdater: WidgetUpdater = WidgetUpdater.NoOp,
) {

  fun observeUpcomingAndActive(): Flow<List<ScheduledActivity>> =
    scheduledActivityDao.observeUpcomingAndActive().map { list -> list.map { it.toDomain() } }

  fun observeUncompleted(): Flow<List<ScheduledActivity>> =
    scheduledActivityDao.observeUncompleted().map { list -> list.map { it.toDomain() } }

  fun observeCompletedHistory(): Flow<List<ActivityWithLog>> =
    activationLogDao.observeCompletedHistory().map { list -> list.map { it.toDomain() } }

  fun observeActivityWithLog(activityId: Long): Flow<ActivityWithLog?> =
    activationLogDao.observeActivityWithLog(activityId).map { it?.toDomain() }

  fun observeAbandonedActivities(nowMs: Long = System.currentTimeMillis()): Flow<List<ActivityWithLog>> =
    activationLogDao
      .observeAbandonedActivities(AbandonedActivityPolicy.abandonedCutoffEpochMs(nowMs))
      .map { list -> list.map { it.toDomain() } }

  suspend fun getActivity(activityId: Long): ScheduledActivity? =
    scheduledActivityDao.getById(activityId)?.toDomain()

  suspend fun getActivityWithLog(activityId: Long): ActivityWithLog? =
    activationLogDao.getActivityWithLog(activityId)?.toDomain()

  suspend fun createActivity(
    title: String,
    category: ActivityCategory,
    durationMinutes: Int,
    startEpochMs: Long,
    recurrenceType: RecurrenceType,
    weeklyDays: Set<DayOfWeek> = emptySet(),
  ): Long {
    val cleanTitle = title.trim().ifEmpty { "Mindful Activity" }
    val safeDuration = durationMinutes.coerceIn(1, 240)
    val nowMs = System.currentTimeMillis()
    val rrule = recurrenceType.toRRule(weeklyDays)

    if (rrule == null) {
      val insertedId =
        scheduledActivityDao.insert(
          ScheduledActivityEntity(
            templateId = null,
            title = cleanTitle,
            category = category,
            scheduledStartEpochMs = startEpochMs,
            durationMinutes = safeDuration,
            status = ActivityStatus.SCHEDULED,
          ),
        )
      val entity = scheduledActivityDao.getById(insertedId)
      if (entity != null) {
        calendarSyncEngine.pushOneOffActivity(entity)
        alarmScheduler.scheduleStartAlarm(entity, nowMs)
      }
      widgetUpdater.requestUpdate()
      return insertedId
    }

    val startZoned = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startEpochMs), ZoneId.systemDefault())
    val startTimeMinutes = startZoned.hour * 60 + startZoned.minute
    val templateId =
      templateDao.insert(
        ActivityTemplateEntity(
          title = cleanTitle,
          category = category,
          durationMinutes = safeDuration,
          recurrenceRule = rrule,
          defaultStartTimeMinutes = startTimeMinutes,
          isActive = true,
          createdAtEpochMs = startEpochMs,
        ),
      )

    val template = templateDao.getById(templateId)
    val calendarEventId =
      if (template != null) {
        calendarSyncEngine.pushRecurringTemplate(template, startEpochMs)
      } else {
        null
      }

    val occurrences =
      RecurrenceExpander.expandOccurrences(
        startEpochMs = startEpochMs,
        recurrenceRule = rrule,
        windowStartEpochMs = minOf(nowMs - 60_000L, startEpochMs),
        windowEndEpochMs = maxOf(nowMs, startEpochMs) + ROLLING_WINDOW_MS,
      )

    var firstId = -1L
    for (occurrenceMs in occurrences) {
      val id =
        scheduledActivityDao.insert(
          ScheduledActivityEntity(
            templateId = templateId,
            title = cleanTitle,
            category = category,
            scheduledStartEpochMs = occurrenceMs,
            durationMinutes = safeDuration,
            status = ActivityStatus.SCHEDULED,
            calendarEventId = calendarEventId,
          ),
        )
      if (firstId == -1L) firstId = id
      scheduledActivityDao.getById(id)?.let { alarmScheduler.scheduleStartAlarm(it, nowMs) }
    }
    widgetUpdater.requestUpdate()
    return firstId
  }

  suspend fun updateActivity(
    activityId: Long,
    title: String,
    category: ActivityCategory,
    durationMinutes: Int,
    startEpochMs: Long,
  ) {
    val cleanTitle = title.trim().ifEmpty { "Mindful Activity" }
    val safeDuration = durationMinutes.coerceIn(1, 240)
    scheduledActivityDao.updateDetails(
      id = activityId,
      title = cleanTitle,
      category = category,
      scheduledStartEpochMs = startEpochMs,
      durationMinutes = safeDuration,
    )
    scheduledActivityDao.getById(activityId)?.let { updated ->
      calendarSyncEngine.updateActivityEvent(updated)
      alarmScheduler.cancelAlarmsForActivity(activityId)
      if (updated.status == ActivityStatus.SCHEDULED) {
        alarmScheduler.scheduleStartAlarm(updated)
      }
    }
    widgetUpdater.requestUpdate()
  }

  suspend fun updateActivityCategory(activityId: Long, category: ActivityCategory) {
    val existing = scheduledActivityDao.getById(activityId) ?: return
    scheduledActivityDao.updateDetails(
      id = activityId,
      title = existing.title,
      category = category,
      scheduledStartEpochMs = existing.scheduledStartEpochMs,
      durationMinutes = existing.durationMinutes,
    )
    widgetUpdater.requestUpdate()
  }

  suspend fun deleteActivity(activityId: Long) {
    val existing = scheduledActivityDao.getById(activityId) ?: return
    alarmScheduler.cancelAlarmsForActivity(activityId)
    notificationHelper.cancelNotificationsForActivity(activityId)
    if (existing.templateId == null) {
      calendarSyncEngine.deleteEventIfPresent(existing.calendarEventId)
    }
    scheduledActivityDao.deleteById(activityId)
    widgetUpdater.requestUpdate()
  }

  suspend fun snooze30Minutes(activityId: Long, nowMs: Long = System.currentTimeMillis()) {
    val existing = scheduledActivityDao.getById(activityId) ?: return
    val base = maxOf(existing.scheduledStartEpochMs, nowMs)
    val newStart = base + 30L * 60_000L
    scheduledActivityDao.reschedule(activityId, newStart)
    notificationHelper.cancelNotificationsForActivity(activityId)
    scheduledActivityDao.getById(activityId)?.let { updated ->
      calendarSyncEngine.updateActivityEvent(updated)
      alarmScheduler.scheduleStartAlarm(updated, nowMs)
    }
    widgetUpdater.requestUpdate()
  }

  suspend fun moveToTomorrow(activityId: Long, nowMs: Long = System.currentTimeMillis()) {
    val existing = scheduledActivityDao.getById(activityId) ?: return
    val zone = ZoneId.systemDefault()
    val tomorrowStart =
      ZonedDateTime
        .ofInstant(Instant.ofEpochMilli(existing.scheduledStartEpochMs), zone)
        .plusDays(1)
        .toInstant()
        .toEpochMilli()
    val safeStart = maxOf(tomorrowStart, nowMs + 60_000L)
    scheduledActivityDao.reschedule(activityId, safeStart)
    notificationHelper.cancelNotificationsForActivity(activityId)
    scheduledActivityDao.getById(activityId)?.let { updated ->
      calendarSyncEngine.updateActivityEvent(updated)
      alarmScheduler.scheduleStartAlarm(updated, nowMs)
    }
    widgetUpdater.requestUpdate()
  }

  suspend fun rescheduleActivity(activityId: Long, newStartEpochMs: Long) {
    scheduledActivityDao.reschedule(activityId, newStartEpochMs)
    notificationHelper.cancelNotificationsForActivity(activityId)
    scheduledActivityDao.getById(activityId)?.let { updated ->
      calendarSyncEngine.updateActivityEvent(updated)
      alarmScheduler.scheduleStartAlarm(updated)
    }
    widgetUpdater.requestUpdate()
  }

  suspend fun dismissUncompleted(activityId: Long) {
    alarmScheduler.cancelAlarmsForActivity(activityId)
    notificationHelper.cancelNotificationsForActivity(activityId)
    scheduledActivityDao.updateStatus(activityId, ActivityStatus.DISCARDED)
    widgetUpdater.requestUpdate()
  }

  suspend fun markUncompleted(activityId: Long) {
    alarmScheduler.cancelAlarmsForActivity(activityId)
    notificationHelper.cancelNotificationsForActivity(activityId)
    scheduledActivityDao.updateStatus(activityId, ActivityStatus.UNCOMPLETED)
    widgetUpdater.requestUpdate()
  }

  suspend fun recordPreCheckIn(
    activityId: Long,
    depression: Int,
    pleasure: Int,
    achievement: Int,
    startTimer: Boolean,
    nowMs: Long = System.currentTimeMillis(),
  ) {
    val activity = scheduledActivityDao.getById(activityId) ?: return
    val existingLog = activationLogDao.getByActivityId(activityId)
    activationLogDao.upsert(
      ActivationLogEntity(
        id = existingLog?.id ?: 0L,
        activityId = activityId,
        preDepression = depression.coerceIn(0, 8),
        prePleasure = pleasure.coerceIn(0, 8),
        preAchievement = achievement.coerceIn(0, 8),
        preLoggedAtEpochMs = nowMs,
      ),
    )
    val timerEnd = if (startTimer) nowMs + activity.durationMinutes * 60_000L else null
    scheduledActivityDao.markInProgress(activityId, startedAtMs = nowMs, timerEndMs = timerEnd)
    notificationHelper.cancelNotificationsForActivity(activityId)
    alarmScheduler.cancelAlarmsForActivity(activityId)

    val completionTriggerMs =
      if (timerEnd != null) {
        notificationHelper.showActiveTimerNotification(activity, timerEnd)
        timerEnd
      } else {
        nowMs + maxOf(activity.durationMinutes, 15) * 60_000L
      }
    alarmScheduler.scheduleCompletionAlarm(activityId, completionTriggerMs, nowMs)
    widgetUpdater.requestUpdate()
  }

  suspend fun setTimerEnd(
    activityId: Long,
    timerEndEpochMs: Long?,
    pausedRemainingSeconds: Long? = null,
  ) {
    scheduledActivityDao.updateTimerEnd(activityId, timerEndEpochMs)
    val activity = scheduledActivityDao.getById(activityId) ?: return
    if (timerEndEpochMs != null) {
      notificationHelper.showActiveTimerNotification(activity, timerEndEpochMs)
      alarmScheduler.scheduleCompletionAlarm(activityId, timerEndEpochMs)
    } else if (pausedRemainingSeconds != null) {
      notificationHelper.showPausedTimerNotification(activity, pausedRemainingSeconds)
      alarmScheduler.cancelAlarmsForActivity(activityId)
    } else {
      notificationHelper.cancelActiveTimerNotification(activityId)
    }
    widgetUpdater.requestUpdate()
  }

  suspend fun extendTimer(activityId: Long, extraMinutes: Int = 5, nowMs: Long = System.currentTimeMillis()) {
    val existing = scheduledActivityDao.getById(activityId) ?: return
    val base = maxOf(existing.timerEndEpochMs ?: nowMs, nowMs)
    val newEnd = base + extraMinutes * 60_000L
    scheduledActivityDao.updateTimerEnd(activityId, newEnd)
    notificationHelper.showActiveTimerNotification(existing, newEnd)
    alarmScheduler.scheduleCompletionAlarm(activityId, newEnd, nowMs)
    widgetUpdater.requestUpdate()
  }

  suspend fun recordPostCheckIn(
    activityId: Long,
    depression: Int,
    pleasure: Int,
    achievement: Int,
    reflectionNote: String?,
    nowMs: Long = System.currentTimeMillis(),
  ) {
    val cleanNote = reflectionNote?.trim()?.takeIf { it.isNotEmpty() }
    activationLogDao.updatePostLog(
      activityId = activityId,
      postDepression = depression.coerceIn(0, 8),
      postPleasure = pleasure.coerceIn(0, 8),
      postAchievement = achievement.coerceIn(0, 8),
      postLoggedAtEpochMs = nowMs,
      reflectionNote = cleanNote,
    )
    scheduledActivityDao.markCompleted(activityId, completedAtMs = nowMs)
    alarmScheduler.cancelAlarmsForActivity(activityId)
    notificationHelper.cancelNotificationsForActivity(activityId)
    widgetUpdater.requestUpdate()
  }

  suspend fun updateReflectionNote(activityId: Long, reflectionNote: String?) {
    val cleanNote = reflectionNote?.trim()?.takeIf { it.isNotEmpty() }
    activationLogDao.updateReflectionNote(activityId = activityId, reflectionNote = cleanNote)
  }

  suspend fun discardAbandonedActivity(activityId: Long) {
    alarmScheduler.cancelAlarmsForActivity(activityId)
    notificationHelper.cancelNotificationsForActivity(activityId)
    scheduledActivityDao.updateStatus(activityId, ActivityStatus.DISCARDED)
    widgetUpdater.requestUpdate()
  }

  suspend fun handleStartAlarmFired(activityId: Long, nowMs: Long = System.currentTimeMillis()) {
    val activity = scheduledActivityDao.getById(activityId) ?: return
    if (activity.status != ActivityStatus.SCHEDULED) return
    notificationHelper.showStartNotification(activity)
    val completionTrigger = maxOf(activity.scheduledStartEpochMs, nowMs) + maxOf(activity.durationMinutes, 15) * 60_000L
    alarmScheduler.scheduleCompletionAlarm(activity.id, completionTrigger, nowMs)
  }

  suspend fun handleCompletionAlarmFired(activityId: Long) {
    val activityWithLog = activationLogDao.getActivityWithLog(activityId) ?: return
    val status = activityWithLog.activity.status
    if (status == ActivityStatus.COMPLETED || status == ActivityStatus.DISCARDED) return
    if (activityWithLog.log?.postLoggedAtEpochMs != null) return
    notificationHelper.cancelActiveTimerNotification(activityId)
    notificationHelper.showCompletionNotification(activityWithLog.activity)
  }

  suspend fun ensureCalendarConnected(): Long? = calendarSyncEngine.ensureDedicatedCalendar()

  suspend fun runMaintenanceSweep(nowMs: Long = System.currentTimeMillis()) {
    expandActiveRecurringTemplates(nowMs)
    calendarSyncEngine.syncFromDedicatedCalendar(nowMs)
    transitionExpiredToUncompleted(nowMs)
    rescheduleAllAlarms(nowMs)
    widgetUpdater.requestUpdate()
  }

  suspend fun rescheduleAllAlarms(nowMs: Long = System.currentTimeMillis()) {
    val active = scheduledActivityDao.getUpcomingAndActive()
    for (activity in active) {
      if (activity.status == ActivityStatus.SCHEDULED && activity.scheduledStartEpochMs > nowMs - 60_000L) {
        alarmScheduler.scheduleStartAlarm(activity, nowMs)
      } else if (activity.status == ActivityStatus.IN_PROGRESS) {
        val log = activationLogDao.getByActivityId(activity.id)
        if (log != null && log.postLoggedAtEpochMs == null) {
          val timerEnd = activity.timerEndEpochMs
          if (timerEnd != null && timerEnd > nowMs) {
            alarmScheduler.scheduleCompletionAlarm(activity.id, timerEnd, nowMs)
            notificationHelper.showActiveTimerNotification(activity, timerEnd)
          } else {
            val triggerAt = (activity.startedAtEpochMs ?: nowMs) + maxOf(activity.durationMinutes, 15) * 60_000L
            if (triggerAt > nowMs) {
              alarmScheduler.scheduleCompletionAlarm(activity.id, triggerAt, nowMs)
            }
          }
        }
      }
    }
  }

  private suspend fun expandActiveRecurringTemplates(nowMs: Long) {
    val templates = templateDao.getActiveTemplates()
    val zone = ZoneId.systemDefault()
    val windowStart = nowMs
    val windowEnd = nowMs + ROLLING_WINDOW_MS
    for (template in templates) {
      val rule = template.recurrenceRule ?: continue
      val anchorDate = Instant.ofEpochMilli(template.createdAtEpochMs).atZone(zone).toLocalDate()
      val anchorTime = LocalTime.of(
        (template.defaultStartTimeMinutes / 60).coerceIn(0, 23),
        (template.defaultStartTimeMinutes % 60).coerceIn(0, 59),
      )
      val anchorEpochMs = ZonedDateTime.of(anchorDate, anchorTime, zone).toInstant().toEpochMilli()
      val occurrences =
        RecurrenceExpander.expandOccurrences(
          startEpochMs = anchorEpochMs,
          recurrenceRule = rule,
          windowStartEpochMs = windowStart,
          windowEndEpochMs = windowEnd,
          zoneId = zone,
        )
      for (occurrenceMs in occurrences) {
        val existing = scheduledActivityDao.findByTemplateAndStart(template.id, occurrenceMs)
        if (existing == null) {
          scheduledActivityDao.insert(
            ScheduledActivityEntity(
              templateId = template.id,
              title = template.title,
              category = template.category,
              scheduledStartEpochMs = occurrenceMs,
              durationMinutes = template.durationMinutes,
              status = ActivityStatus.SCHEDULED,
              calendarEventId = template.calendarEventId,
            ),
          )
        }
      }
    }
  }

  private suspend fun transitionExpiredToUncompleted(nowMs: Long) {
    val scheduled = scheduledActivityDao.getAllScheduled()
    val expired = AbandonedActivityPolicy.filterExpiredScheduled(scheduled.map { it.toDomain() }, nowMs)
    for (activity in expired) {
      scheduledActivityDao.updateStatus(activity.id, ActivityStatus.UNCOMPLETED)
      alarmScheduler.cancelAlarmsForActivity(activity.id)
      notificationHelper.cancelNotificationsForActivity(activity.id)
    }
  }

  companion object {
    private const val ROLLING_WINDOW_MS = 14L * 24 * 60 * 60 * 1000
  }
}

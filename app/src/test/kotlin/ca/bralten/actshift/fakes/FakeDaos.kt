package ca.bralten.actshift.fakes

import ca.bralten.actshift.data.local.dao.ActivationLogDao
import ca.bralten.actshift.data.local.dao.ActivityTemplateDao
import ca.bralten.actshift.data.local.dao.ScheduledActivityDao
import ca.bralten.actshift.data.local.entity.ActivationLogEntity
import ca.bralten.actshift.data.local.entity.ActivityTemplateEntity
import ca.bralten.actshift.data.local.entity.ActivityWithLogEntity
import ca.bralten.actshift.data.local.entity.ScheduledActivityEntity
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.ActivityStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FakeActivityTemplateDao : ActivityTemplateDao {
  private val items = MutableStateFlow<List<ActivityTemplateEntity>>(emptyList())
  private var nextId = 1L

  override suspend fun insert(template: ActivityTemplateEntity): Long {
    val id = if (template.id == 0L) nextId++ else template.id
    val saved = template.copy(id = id)
    items.value = items.value.filterNot { it.id == id } + saved
    return id
  }

  override suspend fun update(template: ActivityTemplateEntity) {
    items.value = items.value.map { if (it.id == template.id) template else it }
  }

  override fun observeActiveTemplates(): Flow<List<ActivityTemplateEntity>> =
    items.map { list -> list.filter { it.isActive } }

  override suspend fun getActiveTemplates(): List<ActivityTemplateEntity> =
    items.value.filter { it.isActive }

  override suspend fun getById(id: Long): ActivityTemplateEntity? =
    items.value.firstOrNull { it.id == id }

  override suspend fun deactivate(id: Long) {
    items.value = items.value.map { if (it.id == id) it.copy(isActive = false) else it }
  }

  override suspend fun updateCalendarEventId(id: Long, calendarEventId: Long?) {
    items.value = items.value.map { if (it.id == id) it.copy(calendarEventId = calendarEventId) else it }
  }
}

class FakeScheduledActivityDao : ScheduledActivityDao {
  val items = MutableStateFlow<List<ScheduledActivityEntity>>(emptyList())
  private var nextId = 1L

  override suspend fun insert(activity: ScheduledActivityEntity): Long {
    val id = if (activity.id == 0L) nextId++ else activity.id
    val saved = activity.copy(id = id)
    items.value = items.value.filterNot { it.id == id } + saved
    return id
  }

  override suspend fun update(activity: ScheduledActivityEntity) {
    items.value = items.value.map { if (it.id == activity.id) activity else it }
  }

  override suspend fun getById(id: Long): ScheduledActivityEntity? =
    items.value.firstOrNull { it.id == id }

  override fun observeById(id: Long): Flow<ScheduledActivityEntity?> =
    items.map { list -> list.firstOrNull { it.id == id } }

  override fun observeUpcomingAndActive(): Flow<List<ScheduledActivityEntity>> =
    items.map { list ->
      list.filter { it.status == ActivityStatus.SCHEDULED || it.status == ActivityStatus.IN_PROGRESS }
        .sortedBy { it.scheduledStartEpochMs }
    }

  override fun observeUncompleted(): Flow<List<ScheduledActivityEntity>> =
    items.map { list -> list.filter { it.status == ActivityStatus.UNCOMPLETED } }

  override suspend fun getUpcomingAndActive(): List<ScheduledActivityEntity> =
    items.value.filter { it.status == ActivityStatus.SCHEDULED || it.status == ActivityStatus.IN_PROGRESS }

  override suspend fun getAllScheduled(): List<ScheduledActivityEntity> =
    items.value.filter { it.status == ActivityStatus.SCHEDULED }

  override suspend fun findByTemplateAndStart(templateId: Long, startMs: Long): ScheduledActivityEntity? =
    items.value.firstOrNull { it.templateId == templateId && it.scheduledStartEpochMs == startMs }

  override suspend fun findByCalendarInstance(eventId: Long, instanceBeginMs: Long): ScheduledActivityEntity? =
    items.value.firstOrNull { it.calendarEventId == eventId && it.calendarInstanceBeginMs == instanceBeginMs }

  override suspend fun findScheduledByCalendarEventId(eventId: Long): List<ScheduledActivityEntity> =
    items.value.filter { it.calendarEventId == eventId && it.status == ActivityStatus.SCHEDULED }

  override suspend fun updateStatus(id: Long, status: ActivityStatus) {
    items.value = items.value.map { if (it.id == id) it.copy(status = status) else it }
  }

  override suspend fun updateDetails(
    id: Long,
    title: String,
    category: ActivityCategory,
    scheduledStartEpochMs: Long,
    durationMinutes: Int,
  ) {
    items.value = items.value.map {
      if (it.id == id) {
        it.copy(
          title = title,
          category = category,
          scheduledStartEpochMs = scheduledStartEpochMs,
          durationMinutes = durationMinutes,
        )
      } else {
        it
      }
    }
  }

  override suspend fun reschedule(id: Long, newStartMs: Long) {
    items.value = items.value.map {
      if (it.id == id) {
        it.copy(
          scheduledStartEpochMs = newStartMs,
          snoozeCount = it.snoozeCount + 1,
          status = ActivityStatus.SCHEDULED,
        )
      } else {
        it
      }
    }
  }

  override suspend fun updateCalendarLink(id: Long, eventId: Long?, instanceBeginMs: Long?) {
    items.value = items.value.map {
      if (it.id == id) it.copy(calendarEventId = eventId, calendarInstanceBeginMs = instanceBeginMs) else it
    }
  }

  override suspend fun markInProgress(id: Long, startedAtMs: Long, timerEndMs: Long?) {
    items.value = items.value.map {
      if (it.id == id) {
        it.copy(status = ActivityStatus.IN_PROGRESS, startedAtEpochMs = startedAtMs, timerEndEpochMs = timerEndMs)
      } else {
        it
      }
    }
  }

  override suspend fun updateTimerEnd(id: Long, timerEndMs: Long?) {
    items.value = items.value.map { if (it.id == id) it.copy(timerEndEpochMs = timerEndMs) else it }
  }

  override suspend fun markCompleted(id: Long, completedAtMs: Long) {
    items.value = items.value.map {
      if (it.id == id) {
        it.copy(status = ActivityStatus.COMPLETED, completedAtEpochMs = completedAtMs, timerEndEpochMs = null)
      } else {
        it
      }
    }
  }

  override suspend fun deleteById(id: Long) {
    items.value = items.value.filterNot { it.id == id }
  }

  override suspend fun deleteFutureForTemplate(templateId: Long) {
    items.value = items.value.filterNot { it.templateId == templateId && it.status == ActivityStatus.SCHEDULED }
  }
}

class FakeActivationLogDao(
  private val scheduledDao: FakeScheduledActivityDao,
) : ActivationLogDao {
  val logs = MutableStateFlow<List<ActivationLogEntity>>(emptyList())
  private var nextId = 1L

  override suspend fun upsert(log: ActivationLogEntity): Long {
    val id = if (log.id == 0L) nextId++ else log.id
    val saved = log.copy(id = id)
    logs.value = logs.value.filterNot { it.activityId == log.activityId } + saved
    return id
  }

  override suspend fun getByActivityId(activityId: Long): ActivationLogEntity? =
    logs.value.firstOrNull { it.activityId == activityId }

  override suspend fun updatePostLog(
    activityId: Long,
    postDepression: Int,
    postPleasure: Int,
    postAchievement: Int,
    postLoggedAtEpochMs: Long,
    reflectionNote: String?,
  ) {
    logs.value = logs.value.map {
      if (it.activityId == activityId) {
        it.copy(
          postDepression = postDepression,
          postPleasure = postPleasure,
          postAchievement = postAchievement,
          postLoggedAtEpochMs = postLoggedAtEpochMs,
          reflectionNote = reflectionNote,
        )
      } else {
        it
      }
    }
  }

  override suspend fun updateReflectionNote(
    activityId: Long,
    reflectionNote: String?,
  ) {
    logs.value = logs.value.map {
      if (it.activityId == activityId) {
        it.copy(reflectionNote = reflectionNote)
      } else {
        it
      }
    }
  }

  override fun observeActivityWithLog(activityId: Long): Flow<ActivityWithLogEntity?> =
    combine(scheduledDao.items, logs) { activities, allLogs ->
      val activity = activities.firstOrNull { it.id == activityId } ?: return@combine null
      ActivityWithLogEntity(activity, allLogs.firstOrNull { it.activityId == activityId })
    }

  override suspend fun getActivityWithLog(activityId: Long): ActivityWithLogEntity? {
    val activity = scheduledDao.getById(activityId) ?: return null
    return ActivityWithLogEntity(activity, getByActivityId(activityId))
  }

  override fun observeCompletedHistory(): Flow<List<ActivityWithLogEntity>> =
    combine(scheduledDao.items, logs) { activities, allLogs ->
      activities
        .filter { it.status == ActivityStatus.COMPLETED }
        .mapNotNull { activity ->
          val log = allLogs.firstOrNull { it.activityId == activity.id && it.postLoggedAtEpochMs != null }
          log?.let { ActivityWithLogEntity(activity, it) }
        }
    }

  override fun observeAbandonedActivities(cutoffEpochMs: Long): Flow<List<ActivityWithLogEntity>> =
    combine(scheduledDao.items, logs) { activities, allLogs ->
      activities
        .filter { it.status == ActivityStatus.IN_PROGRESS }
        .mapNotNull { activity ->
          val log = allLogs.firstOrNull {
            it.activityId == activity.id && it.postLoggedAtEpochMs == null && it.preLoggedAtEpochMs <= cutoffEpochMs
          }
          log?.let { ActivityWithLogEntity(activity, it) }
        }
    }

  override suspend fun getAbandonedActivities(cutoffEpochMs: Long): List<ActivityWithLogEntity> =
    scheduledDao.items.value
      .filter { it.status == ActivityStatus.IN_PROGRESS }
      .mapNotNull { activity ->
        val log = logs.value.firstOrNull {
          it.activityId == activity.id && it.postLoggedAtEpochMs == null && it.preLoggedAtEpochMs <= cutoffEpochMs
        }
        log?.let { ActivityWithLogEntity(activity, it) }
      }
}

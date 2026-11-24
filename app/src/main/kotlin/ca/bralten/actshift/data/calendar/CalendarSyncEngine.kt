package ca.bralten.actshift.data.calendar

import ca.bralten.actshift.data.local.dao.ActivityTemplateDao
import ca.bralten.actshift.data.local.dao.ScheduledActivityDao
import ca.bralten.actshift.data.local.entity.ActivityTemplateEntity
import ca.bralten.actshift.data.local.entity.ScheduledActivityEntity
import ca.bralten.actshift.data.prefs.UserPreferencesRepository
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.ActivityStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates the dedicated "Behavioural Activation" local calendar with Room's mirror table.
 * Strictly isolates reads/writes to the single dedicated calendar ID.
 */
class CalendarSyncEngine(
  private val gateway: CalendarContractGateway,
  private val preferencesRepository: UserPreferencesRepository,
  private val scheduledActivityDao: ScheduledActivityDao,
  private val templateDao: ActivityTemplateDao,
) {
  private val syncMutex = Mutex()

  fun hasCalendarPermissions(): Boolean = gateway.hasCalendarPermissions()

  suspend fun ensureDedicatedCalendar(): Long? =
    syncMutex.withLock {
      if (!gateway.hasCalendarPermissions()) return@withLock null
      val existingId = preferencesRepository.getCurrent().dedicatedCalendarId
      if (existingId != null) {
        preferencesRepository.setDedicatedCalendar(existingId, syncEnabled = true)
        return@withLock existingId
      }
      val createdId = gateway.findOrCreateDedicatedCalendar() ?: return@withLock null
      preferencesRepository.setDedicatedCalendar(createdId, syncEnabled = true)
      createdId
    }

  suspend fun pushOneOffActivity(activity: ScheduledActivityEntity): Long? {
    val calendarId = activeCalendarIdOrNull() ?: return null
    val eventId =
      gateway.upsertEvent(
        calendarId = calendarId,
        existingEventId = activity.calendarEventId,
        title = activity.title,
        startEpochMs = activity.scheduledStartEpochMs,
        durationMinutes = activity.durationMinutes,
        rrule = null,
      ) ?: return null
    scheduledActivityDao.updateCalendarLink(
      id = activity.id,
      eventId = eventId,
      instanceBeginMs = activity.scheduledStartEpochMs,
    )
    return eventId
  }

  suspend fun pushRecurringTemplate(
    template: ActivityTemplateEntity,
    firstStartEpochMs: Long,
  ): Long? {
    val calendarId = activeCalendarIdOrNull() ?: return null
    val eventId =
      gateway.upsertEvent(
        calendarId = calendarId,
        existingEventId = template.calendarEventId,
        title = template.title,
        startEpochMs = firstStartEpochMs,
        durationMinutes = template.durationMinutes,
        rrule = template.recurrenceRule,
      ) ?: return null
    templateDao.updateCalendarEventId(template.id, eventId)
    return eventId
  }

  suspend fun updateActivityEvent(activity: ScheduledActivityEntity) {
    val calendarId = activeCalendarIdOrNull() ?: return
    if (activity.templateId != null) return
    val eventId =
      gateway.upsertEvent(
        calendarId = calendarId,
        existingEventId = activity.calendarEventId,
        title = activity.title,
        startEpochMs = activity.scheduledStartEpochMs,
        durationMinutes = activity.durationMinutes,
        rrule = null,
      )
    if (eventId != null) {
      scheduledActivityDao.updateCalendarLink(
        id = activity.id,
        eventId = eventId,
        instanceBeginMs = activity.scheduledStartEpochMs,
      )
    }
  }

  suspend fun deleteEventIfPresent(eventId: Long?) {
    if (eventId == null) return
    val calendarId = activeCalendarIdOrNull() ?: return
    gateway.deleteEvent(calendarId, eventId)
  }

  /**
   * Queries the dedicated calendar for [-24h, +14d] and ingests external events or updates.
   * Returns IDs of scheduled activities that were inserted or updated.
   */
  suspend fun syncFromDedicatedCalendar(nowMs: Long = System.currentTimeMillis()): List<Long> =
    syncMutex.withLock {
      val calendarId = activeCalendarIdOrNull() ?: return@withLock emptyList()
      val windowStart = nowMs - LOOKBACK_MS
      val windowEnd = nowMs + LOOKAHEAD_MS
      val instances = gateway.queryInstances(calendarId, windowStart, windowEnd)
      val affectedIds = mutableListOf<Long>()

      for (instance in instances) {
        if (instance.calendarId != calendarId) continue

        val existing =
          scheduledActivityDao.findByCalendarInstance(
            eventId = instance.eventId,
            instanceBeginMs = instance.beginMs,
          )

        if (existing == null) {
          // Check if an unlinked template occurrence already exists at the same start time
          val sameTimeMatch =
            scheduledActivityDao
              .findScheduledByCalendarEventId(instance.eventId)
              .firstOrNull { it.calendarInstanceBeginMs == null && it.scheduledStartEpochMs == instance.beginMs }

          if (sameTimeMatch != null) {
            scheduledActivityDao.updateCalendarLink(
              id = sameTimeMatch.id,
              eventId = instance.eventId,
              instanceBeginMs = instance.beginMs,
            )
            affectedIds.add(sameTimeMatch.id)
          } else {
            val insertedId =
              scheduledActivityDao.insert(
                ScheduledActivityEntity(
                  templateId = null,
                  title = instance.title,
                  category = ActivityCategory.BOTH,
                  scheduledStartEpochMs = instance.beginMs,
                  durationMinutes = instance.durationMinutes,
                  status = ActivityStatus.SCHEDULED,
                  calendarEventId = instance.eventId,
                  calendarInstanceBeginMs = instance.beginMs,
                ),
              )
            affectedIds.add(insertedId)
          }
        } else if (existing.status == ActivityStatus.SCHEDULED && existing.snoozeCount == 0) {
          if (
            existing.title != instance.title ||
            existing.scheduledStartEpochMs != instance.beginMs ||
            existing.durationMinutes != instance.durationMinutes
          ) {
            scheduledActivityDao.updateDetails(
              id = existing.id,
              title = instance.title,
              category = existing.category,
              scheduledStartEpochMs = instance.beginMs,
              durationMinutes = instance.durationMinutes,
            )
            scheduledActivityDao.updateCalendarLink(
              id = existing.id,
              eventId = instance.eventId,
              instanceBeginMs = instance.beginMs,
            )
            affectedIds.add(existing.id)
          }
        }
      }
      affectedIds
    }

  private suspend fun activeCalendarIdOrNull(): Long? {
    if (!gateway.hasCalendarPermissions()) return null
    val prefs = preferencesRepository.getCurrent()
    if (!prefs.calendarSyncEnabled) return null
    return prefs.dedicatedCalendarId ?: gateway.findOrCreateDedicatedCalendar()?.also {
      preferencesRepository.setDedicatedCalendar(it, syncEnabled = true)
    }
  }

  companion object {
    private const val LOOKBACK_MS = 24L * 60 * 60 * 1000
    private const val LOOKAHEAD_MS = 14L * 24 * 60 * 60 * 1000
  }
}

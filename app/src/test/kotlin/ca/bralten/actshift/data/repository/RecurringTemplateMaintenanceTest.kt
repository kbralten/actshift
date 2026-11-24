package ca.bralten.actshift.data.repository

import android.content.ContextWrapper
import ca.bralten.actshift.data.calendar.CalendarContractGateway
import ca.bralten.actshift.data.calendar.CalendarInstanceRecord
import ca.bralten.actshift.data.calendar.CalendarSyncEngine
import ca.bralten.actshift.data.prefs.UserPreferencesRepository
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.RecurrenceType
import ca.bralten.actshift.fakes.FakeActivationLogDao
import ca.bralten.actshift.fakes.FakeActivityTemplateDao
import ca.bralten.actshift.fakes.FakeScheduledActivityDao
import ca.bralten.actshift.fakes.InMemoryPreferencesDataStore
import ca.bralten.actshift.notifications.AlarmScheduler
import ca.bralten.actshift.notifications.NotificationHelper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class RecurringTemplateMaintenanceTest {

  @Test
  fun maintenanceSweepDoesNotDuplicateRecurringActivitiesAtCreationHour() = runTest {
    val prefsRepo = UserPreferencesRepository(InMemoryPreferencesDataStore())
    val templateDao = FakeActivityTemplateDao()
    val scheduledDao = FakeScheduledActivityDao()
    val logDao = FakeActivationLogDao(scheduledDao)

    val noopGateway =
      object : CalendarContractGateway {
        override fun hasCalendarPermissions() = false
        override fun findOrCreateDedicatedCalendar() = null
        override fun upsertEvent(calendarId: Long, existingEventId: Long?, title: String, startEpochMs: Long, durationMinutes: Int, rrule: String?) = null
        override fun deleteEvent(calendarId: Long, eventId: Long) = false
        override fun queryInstances(calendarId: Long, windowStartMs: Long, windowEndMs: Long) = emptyList<CalendarInstanceRecord>()
      }

    val stubContext = object : ContextWrapper(null) {
      override fun getSystemService(name: String): Any? = null
      override fun getPackageName(): String = "ca.bralten.actshift"
      override fun checkSelfPermission(permission: String): Int = -1
    }

    val repo =
      ActivationRepository(
        templateDao = templateDao,
        scheduledActivityDao = scheduledDao,
        activationLogDao = logDao,
        calendarSyncEngine = CalendarSyncEngine(noopGateway, prefsRepo, scheduledDao, templateDao),
        alarmScheduler = AlarmScheduler(stubContext),
        notificationHelper = NotificationHelper(stubContext),
      )

    val zone = ZoneId.systemDefault()
    val tomorrowAtEight =
      ZonedDateTime.now(zone)
        .plusDays(1)
        .withHour(8)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
        .toInstant()
        .toEpochMilli()

    repo.createActivity(
      title = "Morning walk",
      category = ActivityCategory.BOTH,
      durationMinutes = 10,
      startEpochMs = tomorrowAtEight,
      recurrenceType = RecurrenceType.DAILY,
    )

    val initialCount = scheduledDao.items.value.size
    assertEquals(15, initialCount)

    // Run maintenance sweep at a different wall-clock time (right now)
    repo.runMaintenanceSweep(System.currentTimeMillis())

    val afterSweep = scheduledDao.items.value
    assertEquals(initialCount, afterSweep.size)
    afterSweep.forEach { entity ->
      val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(entity.scheduledStartEpochMs), zone)
      assertEquals(8, zdt.hour)
      assertEquals(0, zdt.minute)
    }
  }
}

package ca.bralten.actshift.data.calendar

import ca.bralten.actshift.data.prefs.UserPreferencesRepository
import ca.bralten.actshift.fakes.FakeActivityTemplateDao
import ca.bralten.actshift.fakes.FakeScheduledActivityDao
import ca.bralten.actshift.fakes.InMemoryPreferencesDataStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarSyncEngineTest {

  private class FakeCalendarContractGateway(
    var permissionsGranted: Boolean = true,
  ) : CalendarContractGateway {
    var createdCalendarId: Long = 42L
    var queriedCalendarIds = mutableListOf<Long>()
    val instances = mutableListOf<CalendarInstanceRecord>()
    var nextEventId = 100L

    override fun hasCalendarPermissions(): Boolean = permissionsGranted

    override fun findOrCreateDedicatedCalendar(): Long? =
      if (permissionsGranted) createdCalendarId else null

    override fun upsertEvent(
      calendarId: Long,
      existingEventId: Long?,
      title: String,
      startEpochMs: Long,
      durationMinutes: Int,
      rrule: String?,
    ): Long? {
      if (!permissionsGranted) return null
      return existingEventId ?: nextEventId++
    }

    override fun deleteEvent(calendarId: Long, eventId: Long): Boolean = permissionsGranted

    override fun queryInstances(
      calendarId: Long,
      windowStartMs: Long,
      windowEndMs: Long,
    ): List<CalendarInstanceRecord> {
      queriedCalendarIds.add(calendarId)
      return instances.filter { it.calendarId == calendarId }
    }
  }

  @Test
  fun createsDedicatedCalendarAndIgnoresOtherCalendars() = runTest {
    val prefsRepo = UserPreferencesRepository(InMemoryPreferencesDataStore())
    val scheduledDao = FakeScheduledActivityDao()
    val templateDao = FakeActivityTemplateDao()
    val gateway = FakeCalendarContractGateway(permissionsGranted = true)
    val engine = CalendarSyncEngine(gateway, prefsRepo, scheduledDao, templateDao)

    val calId = engine.ensureDedicatedCalendar()
    assertEquals(42L, calId)
    assertEquals(42L, prefsRepo.getCurrent().dedicatedCalendarId)
    assertTrue(prefsRepo.getCurrent().calendarSyncEnabled)

    val now = 1_700_000_000_000L
    gateway.instances.add(
      CalendarInstanceRecord(eventId = 500L, calendarId = 999L, title = "Work Meeting", beginMs = now + 1000, endMs = now + 601_000),
    )
    gateway.instances.add(
      CalendarInstanceRecord(eventId = 501L, calendarId = 42L, title = "Walk dog", beginMs = now + 2000, endMs = now + 602_000),
    )

    val affected = engine.syncFromDedicatedCalendar(now)
    assertEquals(1, affected.size)
    assertEquals(listOf(42L), gateway.queriedCalendarIds)

    val stored = scheduledDao.items.value
    assertEquals(1, stored.size)
    assertEquals("Walk dog", stored.first().title)
    assertEquals(501L, stored.first().calendarEventId)
  }

  @Test
  fun gracefullyNoOpsWhenPermissionsDenied() = runTest {
    val prefsRepo = UserPreferencesRepository(InMemoryPreferencesDataStore())
    val scheduledDao = FakeScheduledActivityDao()
    val templateDao = FakeActivityTemplateDao()
    val gateway = FakeCalendarContractGateway(permissionsGranted = false)
    val engine = CalendarSyncEngine(gateway, prefsRepo, scheduledDao, templateDao)

    assertNull(engine.ensureDedicatedCalendar())
    assertTrue(engine.syncFromDedicatedCalendar().isEmpty())
    assertTrue(gateway.queriedCalendarIds.isEmpty())
  }
}

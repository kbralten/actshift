package ca.bralten.actshift.ui.checkin

import android.content.ContextWrapper
import ca.bralten.actshift.data.calendar.CalendarContractGateway
import ca.bralten.actshift.data.calendar.CalendarInstanceRecord
import ca.bralten.actshift.data.calendar.CalendarSyncEngine
import ca.bralten.actshift.data.local.entity.ActivationLogEntity
import ca.bralten.actshift.data.local.entity.ScheduledActivityEntity
import ca.bralten.actshift.data.prefs.UserPreferencesRepository
import ca.bralten.actshift.data.repository.ActivationRepository
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.ActivityStatus
import ca.bralten.actshift.fakes.FakeActivationLogDao
import ca.bralten.actshift.fakes.FakeActivityTemplateDao
import ca.bralten.actshift.fakes.FakeScheduledActivityDao
import ca.bralten.actshift.fakes.InMemoryPreferencesDataStore
import ca.bralten.actshift.notifications.AlarmScheduler
import ca.bralten.actshift.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostCheckInViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun blindPostCheckInUiStateHasZeroPreScoreFieldsAndSavesAtomically() = runTest {
    val propNames = PostCheckInUiState::class.java.declaredFields.map { it.name.lowercase() }
    assertFalse(propNames.any { it.startsWith("pre") })

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

    // Minimal ContextWrapper(null) suffices here since AlarmScheduler / NotificationHelper
    // only use Context if alarms or notifications are actually dispatched.
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

    val activityId =
      scheduledDao.insert(
        ScheduledActivityEntity(
          title = "Get the mail",
          category = ActivityCategory.ACHIEVEMENT,
          scheduledStartEpochMs = 1000L,
          durationMinutes = 10,
          status = ActivityStatus.IN_PROGRESS,
        ),
      )
    logDao.upsert(
      ActivationLogEntity(
        activityId = activityId,
        preDepression = 6,
        prePleasure = 0,
        preAchievement = 0,
        preLoggedAtEpochMs = 1500L,
      ),
    )

    val vm = PostCheckInViewModel(activityId, repo)
    advanceUntilIdle()

    assertFalse(vm.uiState.value.canSubmit)
    vm.setPostDepression(3)
    vm.setPostPleasure(0)
    vm.setPostAchievement(1)
    vm.updateReflectionNote("Easier once I stepped outside")
    assertTrue(vm.uiState.value.canSubmit)

    var navigated = false
    vm.submit { navigated = true }
    advanceUntilIdle()

    assertTrue(navigated)
    val saved = logDao.getByActivityId(activityId)!!
    assertEquals(3, saved.postDepression)
    assertEquals(0, saved.postPleasure)
    assertEquals(1, saved.postAchievement)
    assertEquals("Easier once I stepped outside", saved.reflectionNote)
    assertEquals(ActivityStatus.COMPLETED, scheduledDao.getById(activityId)!!.status)
  }
}

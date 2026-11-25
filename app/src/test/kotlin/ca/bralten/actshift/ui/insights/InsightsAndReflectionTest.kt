package ca.bralten.actshift.ui.insights

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
import ca.bralten.actshift.ui.history.HistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsAndReflectionTest {

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
  fun insightsViewModelReactsToBothCategoryPreferenceToggle() = runTest {
    val harness = createHarness()
    seedCompletedActivity(harness, title = "Walk dog", category = ActivityCategory.BOTH, depPre = 6, depPost = 2)
    seedCompletedActivity(harness, title = "Listen to song", category = ActivityCategory.PLEASURE, depPre = 4, depPost = 2)

    val vm = InsightsViewModel(harness.repository, harness.prefsRepo)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      vm.uiState.collect {}
    }
    advanceUntilIdle()

    assertTrue(vm.uiState.value.countBothInFunAndAchievement)
    assertEquals(2, vm.uiState.value.summary.deltaBreakdown.pleasure.count)
    assertEquals(1, vm.uiState.value.summary.deltaBreakdown.achievement.count)

    harness.prefsRepo.setCountBothInFunAndAchievement(false)
    advanceUntilIdle()

    assertFalse(vm.uiState.value.countBothInFunAndAchievement)
    assertEquals(1, vm.uiState.value.summary.deltaBreakdown.pleasure.count)
    assertEquals(0, vm.uiState.value.summary.deltaBreakdown.achievement.count)
    assertEquals(1, vm.uiState.value.summary.deltaBreakdown.both.count)
  }

  @Test
  fun historyViewModelUpdatesReflectionNotesAndFilters() = runTest {
    val harness = createHarness()
    val firstId = seedCompletedActivity(harness, title = "Get the mail", category = ActivityCategory.ACHIEVEMENT, depPre = 6, depPost = 3)
    seedCompletedActivity(harness, title = "Sit in sun", category = ActivityCategory.PLEASURE, depPre = 4, depPost = 2)

    val vm = HistoryViewModel(harness.repository, harness.prefsRepo)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      vm.uiState.collect {}
    }
    advanceUntilIdle()

    assertEquals(2, vm.uiState.value.filteredLogs.size)

    vm.updateReflectionNote(firstId, "  Fresh air woke me up  ")
    advanceUntilIdle()
    assertEquals("Fresh air woke me up", harness.logDao.getByActivityId(firstId)?.reflectionNote)

    vm.toggleOnlyWithReflections()
    advanceUntilIdle()
    assertEquals(1, vm.uiState.value.filteredLogs.size)
    assertEquals(firstId, vm.uiState.value.filteredLogs.first().activity.id)

    vm.updateReflectionNote(firstId, "   ")
    advanceUntilIdle()
    assertNull(harness.logDao.getByActivityId(firstId)?.reflectionNote)
    assertEquals(0, vm.uiState.value.filteredLogs.size)
  }

  private suspend fun seedCompletedActivity(
    harness: TestHarness,
    title: String,
    category: ActivityCategory,
    depPre: Int,
    depPost: Int,
  ): Long {
    val now = System.currentTimeMillis()
    val id =
      harness.scheduledDao.insert(
        ScheduledActivityEntity(
          title = title,
          category = category,
          scheduledStartEpochMs = now - 600_000L,
          durationMinutes = 10,
          status = ActivityStatus.COMPLETED,
          completedAtEpochMs = now,
        ),
      )
    harness.logDao.upsert(
      ActivationLogEntity(
        activityId = id,
        preDepression = depPre,
        prePleasure = 1,
        preAchievement = 1,
        preLoggedAtEpochMs = now - 600_000L,
        postDepression = depPost,
        postPleasure = 3,
        postAchievement = 3,
        postLoggedAtEpochMs = now,
        reflectionNote = null,
      ),
    )
    return id
  }

  private data class TestHarness(
    val prefsRepo: UserPreferencesRepository,
    val scheduledDao: FakeScheduledActivityDao,
    val logDao: FakeActivationLogDao,
    val repository: ActivationRepository,
  )

  private fun createHarness(): TestHarness {
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

    val repository =
      ActivationRepository(
        templateDao = templateDao,
        scheduledActivityDao = scheduledDao,
        activationLogDao = logDao,
        calendarSyncEngine = CalendarSyncEngine(noopGateway, prefsRepo, scheduledDao, templateDao),
        alarmScheduler = AlarmScheduler(stubContext),
        notificationHelper = NotificationHelper(stubContext),
      )
    return TestHarness(prefsRepo, scheduledDao, logDao, repository)
  }
}

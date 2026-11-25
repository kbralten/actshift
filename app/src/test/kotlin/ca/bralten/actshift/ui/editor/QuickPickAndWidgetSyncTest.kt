package ca.bralten.actshift.ui.editor

import android.content.ContextWrapper
import ca.bralten.actshift.data.calendar.CalendarContractGateway
import ca.bralten.actshift.data.calendar.CalendarInstanceRecord
import ca.bralten.actshift.data.calendar.CalendarSyncEngine
import ca.bralten.actshift.data.prefs.UserPreferencesRepository
import ca.bralten.actshift.data.repository.ActivationRepository
import ca.bralten.actshift.domain.ActivitySuggestion
import ca.bralten.actshift.domain.EnergyLevel
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.RecurrenceType
import ca.bralten.actshift.fakes.FakeActivationLogDao
import ca.bralten.actshift.fakes.FakeActivityTemplateDao
import ca.bralten.actshift.fakes.FakeScheduledActivityDao
import ca.bralten.actshift.fakes.InMemoryPreferencesDataStore
import ca.bralten.actshift.notifications.AlarmScheduler
import ca.bralten.actshift.notifications.NotificationHelper
import ca.bralten.actshift.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class QuickPickAndWidgetSyncTest {

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
  fun editorViewModelInitializesFromPrefillAndAppliesQuickPickSuggestion() {
    val harness = createHarness()
    val vm =
      ActivityEditorViewModel(
        activityId = null,
        repository = harness.repository,
        prefillTitle = "Get the mail",
        prefillDurationMinutes = 5,
        prefillCategory = ActivityCategory.ACHIEVEMENT,
      )

    assertEquals("Get the mail", vm.uiState.value.title)
    assertEquals(5, vm.uiState.value.durationMinutes)
    assertFalse(vm.uiState.value.isCustomDuration)
    assertEquals(ActivityCategory.ACHIEVEMENT, vm.uiState.value.category)
    assertTrue(vm.uiState.value.canSave)

    vm.applySuggestion(
      ActivitySuggestion(
        id = "mod_both_1",
        title = "Walk the dog",
        durationMinutes = 15,
        category = ActivityCategory.BOTH,
        energyLevel = EnergyLevel.MODERATE,
      ),
    )

    assertEquals("Walk the dog", vm.uiState.value.title)
    assertEquals(15, vm.uiState.value.durationMinutes)
    assertFalse(vm.uiState.value.isCustomDuration)
    assertEquals(ActivityCategory.BOTH, vm.uiState.value.category)
  }

  @Test
  fun repositoryTriggersWidgetUpdateAcrossActivityLifecycleMutations() = runTest {
    var updateCount = 0
    val harness = createHarness(WidgetUpdater { updateCount += 1 })

    val id =
      harness.repository.createActivity(
        title = "Drink water",
        category = ActivityCategory.ACHIEVEMENT,
        durationMinutes = 5,
        startEpochMs = 100_000L,
        recurrenceType = RecurrenceType.ONCE,
      )
    assertEquals(1, updateCount)

    harness.repository.snooze30Minutes(id, nowMs = 90_000L)
    assertEquals(2, updateCount)

    harness.repository.recordPreCheckIn(
      activityId = id,
      depression = 5,
      pleasure = 1,
      achievement = 1,
      startTimer = false,
      nowMs = 110_000L,
    )
    assertEquals(3, updateCount)

    harness.repository.recordPostCheckIn(
      activityId = id,
      depression = 3,
      pleasure = 2,
      achievement = 3,
      reflectionNote = "Felt good to start",
      nowMs = 120_000L,
    )
    assertEquals(4, updateCount)
  }

  private data class Harness(val repository: ActivationRepository)

  private fun createHarness(widgetUpdater: WidgetUpdater = WidgetUpdater.NoOp): Harness {
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
        widgetUpdater = widgetUpdater,
      )
    return Harness(repository)
  }
}

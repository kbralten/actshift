package ca.bralten.actshift.ui.ideas

import android.content.ContextWrapper
import ca.bralten.actshift.data.calendar.CalendarContractGateway
import ca.bralten.actshift.data.calendar.CalendarInstanceRecord
import ca.bralten.actshift.data.calendar.CalendarSyncEngine
import ca.bralten.actshift.data.prefs.UserPreferencesRepository
import ca.bralten.actshift.data.repository.ActivationRepository
import ca.bralten.actshift.data.repository.QuickIdeaRepository
import ca.bralten.actshift.domain.EnergyLevel
import ca.bralten.actshift.domain.QuickTimePresets
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.ScheduleTimeGranularity
import ca.bralten.actshift.fakes.FakeActivationLogDao
import ca.bralten.actshift.fakes.FakeActivityTemplateDao
import ca.bralten.actshift.fakes.FakeQuickIdeaDao
import ca.bralten.actshift.fakes.FakeScheduledActivityDao
import ca.bralten.actshift.fakes.InMemoryPreferencesDataStore
import ca.bralten.actshift.notifications.AlarmScheduler
import ca.bralten.actshift.notifications.NotificationHelper
import ca.bralten.actshift.ui.editor.ActivityEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class QuickIdeaCustomizationAndGranularityTest {

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
  fun quickIdeaRepositorySeedsOnceSupportsCrudAndRestoresMissingStarters() = runTest {
    val harness = createHarness()

    harness.quickIdeaRepo.ensureSeeded()
    harness.quickIdeaRepo.ensureSeeded()
    assertEquals(27, harness.quickIdeaDao.countAll())

    val customId =
      harness.quickIdeaRepo.addIdea(
        title = "Sit on the porch with tea",
        durationMinutes = 10,
        category = ActivityCategory.PLEASURE,
        energyLevel = EnergyLevel.LOW,
      )
    val afterAdd = harness.quickIdeaRepo.observeIdeas().first()
    assertEquals(28, afterAdd.size)
    val custom = afterAdd.first { it.dbId == customId }
    assertFalse(custom.isStarter)
    assertEquals(EnergyLevel.LOW, custom.energyLevel)

    harness.quickIdeaRepo.updateIdea(
      dbId = customId,
      title = "Sit on the porch with herbal tea",
      durationMinutes = 15,
      category = ActivityCategory.BOTH,
      energyLevel = EnergyLevel.MEDIUM,
    )
    val updated = harness.quickIdeaRepo.observeIdeas().first().first { it.dbId == customId }
    assertEquals("Sit on the porch with herbal tea", updated.title)
    assertEquals(15, updated.durationMinutes)
    assertEquals(ActivityCategory.BOTH, updated.category)
    assertEquals(EnergyLevel.MEDIUM, updated.energyLevel)

    // Delete one starter idea, then restore
    val starterToDelete = afterAdd.first { it.isStarter }
    harness.quickIdeaRepo.deleteIdea(starterToDelete.dbId)
    assertEquals(27, harness.quickIdeaDao.countAll())

    val restoredCount = harness.quickIdeaRepo.restoreStarterIdeas()
    assertEquals(1, restoredCount)
    assertEquals(28, harness.quickIdeaDao.countAll())
  }

  @Test
  fun granularityPreferenceUpdatesQuickIdeasAndEditorPresetsAndSchedulesNextSlot() = runTest {
    val harness = createHarness()

    val ideasVm = QuickIdeasViewModel(harness.quickIdeaRepo, harness.activationRepo, harness.prefsRepo)
    val editorVm =
      ActivityEditorViewModel(
        activityId = null,
        repository = harness.activationRepo,
        preferencesRepository = harness.prefsRepo,
        quickIdeaRepository = harness.quickIdeaRepo,
      )

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { ideasVm.uiState.collect {} }
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { editorVm.uiState.collect {} }
    advanceUntilIdle()

    assertEquals(ScheduleTimeGranularity.HOUR, ideasVm.uiState.value.granularity)
    assertTrue(editorVm.uiState.value.quickPresets.any { it.label.startsWith("Next hour") })

    harness.prefsRepo.setScheduleTimeGranularity(ScheduleTimeGranularity.HALF_HOUR)
    advanceUntilIdle()

    assertEquals(ScheduleTimeGranularity.HALF_HOUR, ideasVm.uiState.value.granularity)
    assertTrue(editorVm.uiState.value.quickPresets.any { it.label.startsWith("Next 30m") })
    assertTrue(editorVm.uiState.value.quickPresets.any { it.label == "In 30m" })

    val sampleIdea = ideasVm.uiState.value.filteredIdeas.first()
    val before = ZonedDateTime.now()
    ideasVm.scheduleNextSlot(sampleIdea)
    advanceUntilIdle()
    val after = ZonedDateTime.now()

    val created = harness.scheduledDao.items.value.single()
    val expectedBefore = QuickTimePresets.nextHalfHour(before).toInstant().toEpochMilli()
    val expectedAfter = QuickTimePresets.nextHalfHour(after).toInstant().toEpochMilli()
    assertTrue(created.scheduledStartEpochMs in expectedBefore..expectedAfter)

    val scheduledZoned = ZonedDateTime.ofInstant(Instant.ofEpochMilli(created.scheduledStartEpochMs), ZoneId.systemDefault())
    assertTrue(scheduledZoned.minute == 0 || scheduledZoned.minute == 30)
  }

  private data class Harness(
    val prefsRepo: UserPreferencesRepository,
    val quickIdeaDao: FakeQuickIdeaDao,
    val scheduledDao: FakeScheduledActivityDao,
    val quickIdeaRepo: QuickIdeaRepository,
    val activationRepo: ActivationRepository,
  )

  private fun createHarness(): Harness {
    val prefsRepo = UserPreferencesRepository(InMemoryPreferencesDataStore())
    val templateDao = FakeActivityTemplateDao()
    val scheduledDao = FakeScheduledActivityDao()
    val logDao = FakeActivationLogDao(scheduledDao)
    val quickIdeaDao = FakeQuickIdeaDao()

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

    val activationRepo =
      ActivationRepository(
        templateDao = templateDao,
        scheduledActivityDao = scheduledDao,
        activationLogDao = logDao,
        calendarSyncEngine = CalendarSyncEngine(noopGateway, prefsRepo, scheduledDao, templateDao),
        alarmScheduler = AlarmScheduler(stubContext),
        notificationHelper = NotificationHelper(stubContext),
      )
    val quickIdeaRepo = QuickIdeaRepository(quickIdeaDao, prefsRepo)

    return Harness(prefsRepo, quickIdeaDao, scheduledDao, quickIdeaRepo, activationRepo)
  }
}

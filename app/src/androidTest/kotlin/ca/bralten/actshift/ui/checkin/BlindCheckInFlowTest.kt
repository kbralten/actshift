package ca.bralten.actshift.ui.checkin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.bralten.actshift.data.calendar.CalendarContractGateway
import ca.bralten.actshift.data.calendar.CalendarInstanceRecord
import ca.bralten.actshift.data.calendar.CalendarSyncEngine
import ca.bralten.actshift.data.local.ActShiftDatabase
import ca.bralten.actshift.data.local.entity.ScheduledActivityEntity
import ca.bralten.actshift.data.prefs.UserPreferencesRepository
import ca.bralten.actshift.data.repository.ActivationRepository
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.ActivityStatus
import ca.bralten.actshift.notifications.AlarmScheduler
import ca.bralten.actshift.notifications.NotificationHelper
import ca.bralten.actshift.ui.theme.ActShiftTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlindCheckInFlowTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var db: ActShiftDatabase
  private lateinit var repository: ActivationRepository
  private var activityId: Long = 0L

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    db = Room.inMemoryDatabaseBuilder(context, ActShiftDatabase::class.java).build()
    val prefs = UserPreferencesRepository(context)
    val noopGateway =
      object : CalendarContractGateway {
        override fun hasCalendarPermissions() = false
        override fun findOrCreateDedicatedCalendar() = null
        override fun upsertEvent(calendarId: Long, existingEventId: Long?, title: String, startEpochMs: Long, durationMinutes: Int, rrule: String?) = null
        override fun deleteEvent(calendarId: Long, eventId: Long) = false
        override fun queryInstances(calendarId: Long, windowStartMs: Long, windowEndMs: Long) = emptyList<CalendarInstanceRecord>()
      }
    repository =
      ActivationRepository(
        templateDao = db.activityTemplateDao(),
        scheduledActivityDao = db.scheduledActivityDao(),
        activationLogDao = db.activationLogDao(),
        calendarSyncEngine = CalendarSyncEngine(noopGateway, prefs, db.scheduledActivityDao(), db.activityTemplateDao()),
        alarmScheduler = AlarmScheduler(context),
        notificationHelper = NotificationHelper(context),
      )

    activityId = runBlocking {
      db.scheduledActivityDao().insert(
        ScheduledActivityEntity(
          title = "Get the mail",
          category = ActivityCategory.ACHIEVEMENT,
          scheduledStartEpochMs = System.currentTimeMillis() + 60_000L,
          durationMinutes = 10,
          status = ActivityStatus.SCHEDULED,
        ),
      )
    }
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun blindPostCheckInHidesPreScoresAndRevealDisplaysDeltas() {
    composeRule.setContent {
      ActShiftTheme {
        var step by remember { mutableStateOf("pre") }
        when (step) {
          "pre" -> {
            val vm = remember { PreCheckInViewModel(activityId, repository) }
            PreCheckInScreen(
              viewModel = vm,
              onNavigateBack = {},
              onStartWithTimer = { step = "post" },
              onStartWithoutTimer = { step = "post" },
            )
          }
          "post" -> {
            val vm = remember { PostCheckInViewModel(activityId, repository) }
            PostCheckInScreen(
              viewModel = vm,
              onNavigateBack = {},
              onSaved = { step = "reveal" },
            )
          }
          "reveal" -> {
            val vm = remember { RevealViewModel(activityId, repository) }
            RevealScreen(viewModel = vm, onDone = {})
          }
        }
      }
    }

    // Phase 1: Pre Check-in requires selecting all 3 ratings
    composeRule.onNodeWithTag("begin_activity_button").assertIsNotEnabled()
    composeRule.onNodeWithTag("pre_depression_6").performScrollTo().performClick()
    composeRule.onNodeWithTag("pre_pleasure_0").performScrollTo().performClick()
    composeRule.onNodeWithTag("pre_achievement_0").performScrollTo().performClick()
    composeRule.onNodeWithTag("begin_activity_button").assertIsEnabled().performClick()

    // Phase 2: Blind Post Check-in MUST NOT show pre scores (6 · Higher, Before, etc.)
    composeRule.onNodeWithText("After Check-In").assertExists()
    composeRule.onNodeWithText("6 · Higher").assertDoesNotExist()
    composeRule.onNodeWithText("Before").assertDoesNotExist()

    composeRule.onNodeWithTag("save_see_shift_button").assertIsNotEnabled()
    composeRule.onNodeWithTag("post_depression_3").performScrollTo().performClick()
    composeRule.onNodeWithTag("post_pleasure_0").performScrollTo().performClick()
    composeRule.onNodeWithTag("post_achievement_1").performScrollTo().performClick()
    composeRule.onNodeWithTag("reflection_input").performScrollTo().performTextInput("Quick walk outside")
    composeRule.onNodeWithTag("save_see_shift_button").assertIsEnabled().performClick()

    // Phase 3: Reveal shows Before -> After and deltas
    composeRule.onNodeWithText("Before vs. After").assertExists()
    composeRule.onNodeWithText("6 → 3").assertExists()
    composeRule.onNodeWithText("(-3)").assertExists()
    composeRule.onNodeWithText("0 → 1").assertExists()
    composeRule.onNodeWithText("(+1)").assertExists()
    composeRule.onNodeWithText("Quick walk outside").assertExists()
  }
}

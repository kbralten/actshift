package ca.bralten.actshift.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.bralten.actshift.data.prefs.UserPreferences
import ca.bralten.actshift.data.prefs.UserPreferencesRepository
import ca.bralten.actshift.data.repository.ActivationRepository
import ca.bralten.actshift.data.repository.QuickIdeaRepository
import ca.bralten.actshift.domain.ActivitySuggestion
import ca.bralten.actshift.domain.ActivitySuggestionLibrary
import ca.bralten.actshift.domain.QuickTimePresets
import ca.bralten.actshift.domain.model.ActivityStatus
import ca.bralten.actshift.domain.model.ActivityWithLog
import ca.bralten.actshift.domain.model.RecurrenceType
import ca.bralten.actshift.domain.model.ScheduleTimeGranularity
import ca.bralten.actshift.domain.model.ScheduledActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class TodayUiState(
  val inProgressActivities: List<ScheduledActivity> = emptyList(),
  val nextActivity: ScheduledActivity? = null,
  val todayRemaining: List<ScheduledActivity> = emptyList(),
  val upcomingLater: List<ScheduledActivity> = emptyList(),
  val uncompletedActivities: List<ScheduledActivity> = emptyList(),
  val abandonedPrompt: ActivityWithLog? = null,
  val uncompletedExpanded: Boolean = false,
  val quickIdeas: List<ActivitySuggestion> = ActivitySuggestionLibrary.allSuggestions,
  val scheduleTimeGranularity: ScheduleTimeGranularity = ScheduleTimeGranularity.HOUR,
)

class TodayViewModel(
  private val repository: ActivationRepository,
  private val quickIdeaRepository: QuickIdeaRepository? = null,
  preferencesRepository: UserPreferencesRepository? = null,
) : ViewModel() {

  private val uncompletedExpanded = MutableStateFlow(false)

  init {
    if (quickIdeaRepository != null) {
      viewModelScope.launch { quickIdeaRepository.ensureSeeded() }
    }
  }

  private val baseStateFlow =
    combine(
      repository.observeUpcomingAndActive(),
      repository.observeUncompleted(),
      repository.observeAbandonedActivities(),
      uncompletedExpanded,
    ) { upcomingAndActive, uncompleted, abandoned, expanded ->
      val inProgress = upcomingAndActive.filter { it.status == ActivityStatus.IN_PROGRESS }
      val scheduled = upcomingAndActive.filter { it.status == ActivityStatus.SCHEDULED }
      val next = scheduled.firstOrNull()
      val rest = if (scheduled.size > 1) scheduled.drop(1) else emptyList()

      val zone = ZoneId.systemDefault()
      val today = LocalDate.now(zone)
      val todayRest =
        rest.filter {
          Instant.ofEpochMilli(it.scheduledStartEpochMs).atZone(zone).toLocalDate() == today
        }
      val later =
        rest.filter {
          Instant.ofEpochMilli(it.scheduledStartEpochMs).atZone(zone).toLocalDate().isAfter(today)
        }

      TodayUiState(
        inProgressActivities = inProgress,
        nextActivity = next,
        todayRemaining = todayRest,
        upcomingLater = later,
        uncompletedActivities = uncompleted,
        abandonedPrompt = abandoned.firstOrNull(),
        uncompletedExpanded = expanded,
      )
    }

  val uiState: StateFlow<TodayUiState> =
    combine(
      baseStateFlow,
      quickIdeaRepository?.observeIdeas() ?: flowOf(ActivitySuggestionLibrary.allSuggestions),
      preferencesRepository?.preferences ?: flowOf(UserPreferences()),
    ) { base, ideas, prefs ->
      base.copy(
        quickIdeas = ideas,
        scheduleTimeGranularity = prefs.scheduleTimeGranularity,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = TodayUiState(),
    )

  fun refreshAndSync() {
    viewModelScope.launch {
      repository.runMaintenanceSweep()
    }
  }

  fun toggleUncompletedExpanded() {
    uncompletedExpanded.value = !uncompletedExpanded.value
  }

  fun snooze30Minutes(activityId: Long) {
    viewModelScope.launch { repository.snooze30Minutes(activityId) }
  }

  fun moveToTomorrow(activityId: Long) {
    viewModelScope.launch { repository.moveToTomorrow(activityId) }
  }

  fun deleteActivity(activityId: Long) {
    viewModelScope.launch { repository.deleteActivity(activityId) }
  }

  fun rescheduleUncompletedToSoon(activityId: Long) {
    viewModelScope.launch {
      val oneHourLater = System.currentTimeMillis() + 60L * 60_000L
      repository.rescheduleActivity(activityId, oneHourLater)
    }
  }

  fun dismissUncompleted(activityId: Long) {
    viewModelScope.launch { repository.dismissUncompleted(activityId) }
  }

  fun moveToUncompleted(activityId: Long) {
    viewModelScope.launch { repository.markUncompleted(activityId) }
  }

  fun discardAbandoned(activityId: Long) {
    viewModelScope.launch { repository.discardAbandonedActivity(activityId) }
  }

  fun startQuickPickNow(
    suggestion: ActivitySuggestion,
    onCreated: (Long) -> Unit,
  ) {
    viewModelScope.launch {
      val id =
        repository.createActivity(
          title = suggestion.title,
          category = suggestion.category,
          durationMinutes = suggestion.durationMinutes,
          startEpochMs = System.currentTimeMillis(),
          recurrenceType = RecurrenceType.ONCE,
        )
      onCreated(id)
    }
  }

  fun scheduleQuickPickNextSlot(
    suggestion: ActivitySuggestion,
    onScheduled: (Long) -> Unit,
  ) {
    viewModelScope.launch {
      val targetMs =
        QuickTimePresets
          .nextSlotDateTime(ZonedDateTime.now(), uiState.value.scheduleTimeGranularity)
          .toInstant()
          .toEpochMilli()
      repository.createActivity(
        title = suggestion.title,
        category = suggestion.category,
        durationMinutes = suggestion.durationMinutes,
        startEpochMs = targetMs,
        recurrenceType = RecurrenceType.ONCE,
      )
      onScheduled(targetMs)
    }
  }

  companion object {
    fun factory(
      repository: ActivationRepository,
      quickIdeaRepository: QuickIdeaRepository? = null,
      preferencesRepository: UserPreferencesRepository? = null,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          TodayViewModel(repository, quickIdeaRepository, preferencesRepository) as T
      }
  }
}

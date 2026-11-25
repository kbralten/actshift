package ca.bralten.actshift.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.bralten.actshift.data.repository.ActivationRepository
import ca.bralten.actshift.domain.ActivitySuggestion
import ca.bralten.actshift.domain.QuickTimePreset
import ca.bralten.actshift.domain.QuickTimePresets
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.RecurrenceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class ActivityEditorUiState(
  val isEditing: Boolean = false,
  val title: String = "",
  val durationMinutes: Int = 10,
  val isCustomDuration: Boolean = false,
  val customDurationText: String = "20",
  val category: ActivityCategory = ActivityCategory.BOTH,
  val scheduledDate: LocalDate = LocalDate.now(),
  val scheduledTime: LocalTime = LocalTime.now().plusMinutes(15).withSecond(0).withNano(0),
  val quickPresets: List<QuickTimePreset> = QuickTimePresets.buildQuickPresets(ZonedDateTime.now()),
  val recurrenceType: RecurrenceType = RecurrenceType.ONCE,
  val weeklyDays: Set<DayOfWeek> = setOf(LocalDate.now().dayOfWeek),
  val isSaving: Boolean = false,
) {
  val canSave: Boolean
    get() = title.isNotBlank() && effectiveDurationMinutes in 1..240 && !isSaving

  val effectiveDurationMinutes: Int
    get() =
      if (isCustomDuration) {
        customDurationText.toIntOrNull() ?: 0
      } else {
        durationMinutes
      }
}

class ActivityEditorViewModel(
  private val activityId: Long?,
  private val repository: ActivationRepository,
  prefillTitle: String? = null,
  prefillDurationMinutes: Int? = null,
  prefillCategory: ActivityCategory? = null,
) : ViewModel() {

  private val _uiState =
    MutableStateFlow(
      if (activityId == null) {
        val duration = prefillDurationMinutes?.coerceIn(1, 240) ?: 10
        val isPreset = duration in listOf(5, 10, 15)
        ActivityEditorUiState(
          isEditing = false,
          title = prefillTitle.orEmpty(),
          durationMinutes = duration,
          isCustomDuration = !isPreset,
          customDurationText = duration.toString(),
          category = prefillCategory ?: ActivityCategory.BOTH,
        )
      } else {
        ActivityEditorUiState(isEditing = true)
      },
    )
  val uiState: StateFlow<ActivityEditorUiState> = _uiState.asStateFlow()

  init {
    if (activityId != null) {
      viewModelScope.launch {
        val existing = repository.getActivity(activityId) ?: return@launch
        val zoned =
          ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(existing.scheduledStartEpochMs),
            ZoneId.systemDefault(),
          )
        val isPreset = existing.durationMinutes in listOf(5, 10, 15)
        _uiState.update {
          it.copy(
            isEditing = true,
            title = existing.title,
            durationMinutes = existing.durationMinutes,
            isCustomDuration = !isPreset,
            customDurationText = existing.durationMinutes.toString(),
            category = existing.category,
            scheduledDate = zoned.toLocalDate(),
            scheduledTime = zoned.toLocalTime().withSecond(0).withNano(0),
          )
        }
      }
    }
  }

  fun updateTitle(title: String) {
    _uiState.update { it.copy(title = title) }
  }

  fun selectPresetDuration(minutes: Int) {
    _uiState.update { it.copy(durationMinutes = minutes, isCustomDuration = false) }
  }

  fun enableCustomDuration() {
    _uiState.update { it.copy(isCustomDuration = true) }
  }

  fun updateCustomDurationText(text: String) {
    val digitsOnly = text.filter { it.isDigit() }.take(3)
    _uiState.update { it.copy(customDurationText = digitsOnly, isCustomDuration = true) }
  }

  fun selectCategory(category: ActivityCategory) {
    _uiState.update { it.copy(category = category) }
  }

  fun applySuggestion(suggestion: ActivitySuggestion) {
    val isPreset = suggestion.durationMinutes in listOf(5, 10, 15)
    _uiState.update {
      it.copy(
        title = suggestion.title,
        durationMinutes = suggestion.durationMinutes,
        isCustomDuration = !isPreset,
        customDurationText = suggestion.durationMinutes.toString(),
        category = suggestion.category,
      )
    }
  }

  fun updateDate(date: LocalDate) {
    _uiState.update { it.copy(scheduledDate = date) }
  }

  fun updateTime(time: LocalTime) {
    _uiState.update { it.copy(scheduledTime = time.withSecond(0).withNano(0)) }
  }

  fun applyQuickPreset(preset: QuickTimePreset) {
    _uiState.update {
      it.copy(
        scheduledDate = preset.targetDateTime.toLocalDate(),
        scheduledTime = preset.targetDateTime.toLocalTime().withSecond(0).withNano(0),
        quickPresets = QuickTimePresets.buildQuickPresets(ZonedDateTime.now()),
      )
    }
  }

  fun selectRecurrence(type: RecurrenceType) {
    _uiState.update { it.copy(recurrenceType = type) }
  }

  fun toggleWeeklyDay(day: DayOfWeek) {
    _uiState.update { state ->
      val next =
        if (day in state.weeklyDays && state.weeklyDays.size > 1) {
          state.weeklyDays - day
        } else {
          state.weeklyDays + day
        }
      state.copy(weeklyDays = next)
    }
  }

  fun save(onSaved: () -> Unit) {
    val snapshot = _uiState.value
    if (!snapshot.canSave) return
    _uiState.update { it.copy(isSaving = true) }

    viewModelScope.launch {
      val startMs =
        ZonedDateTime.of(
          snapshot.scheduledDate,
          snapshot.scheduledTime,
          ZoneId.systemDefault(),
        ).toInstant().toEpochMilli()

      if (activityId == null) {
        repository.createActivity(
          title = snapshot.title,
          category = snapshot.category,
          durationMinutes = snapshot.effectiveDurationMinutes,
          startEpochMs = startMs,
          recurrenceType = snapshot.recurrenceType,
          weeklyDays = snapshot.weeklyDays,
        )
      } else {
        repository.updateActivity(
          activityId = activityId,
          title = snapshot.title,
          category = snapshot.category,
          durationMinutes = snapshot.effectiveDurationMinutes,
          startEpochMs = startMs,
        )
      }
      _uiState.update { it.copy(isSaving = false) }
      onSaved()
    }
  }

  companion object {
    fun factory(
      activityId: Long?,
      repository: ActivationRepository,
      prefillTitle: String? = null,
      prefillDurationMinutes: Int? = null,
      prefillCategory: ActivityCategory? = null,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          ActivityEditorViewModel(
            activityId = activityId,
            repository = repository,
            prefillTitle = prefillTitle,
            prefillDurationMinutes = prefillDurationMinutes,
            prefillCategory = prefillCategory,
          ) as T
      }
  }
}

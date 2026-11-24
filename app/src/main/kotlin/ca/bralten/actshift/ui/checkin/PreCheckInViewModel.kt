package ca.bralten.actshift.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.bralten.actshift.data.repository.ActivationRepository
import ca.bralten.actshift.domain.model.ActivityCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PreCheckInUiState(
  val activityId: Long,
  val activityTitle: String = "",
  val durationMinutes: Int = 10,
  val category: ActivityCategory = ActivityCategory.BOTH,
  val depression: Int? = null,
  val pleasure: Int? = null,
  val achievement: Int? = null,
  val startTimer: Boolean = true,
  val isLoading: Boolean = true,
  val isSaving: Boolean = false,
) {
  val canSubmit: Boolean
    get() = depression != null && pleasure != null && achievement != null && !isSaving && !isLoading
}

class PreCheckInViewModel(
  private val activityId: Long,
  private val repository: ActivationRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(PreCheckInUiState(activityId = activityId))
  val uiState: StateFlow<PreCheckInUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      val activity = repository.getActivity(activityId)
      _uiState.update {
        if (activity != null) {
          it.copy(
            activityTitle = activity.title,
            durationMinutes = activity.durationMinutes,
            category = activity.category,
            isLoading = false,
          )
        } else {
          it.copy(isLoading = false)
        }
      }
    }
  }

  fun selectCategory(category: ActivityCategory) {
    _uiState.update { it.copy(category = category) }
    viewModelScope.launch {
      repository.updateActivityCategory(activityId, category)
    }
  }

  fun setDepression(score: Int) {
    _uiState.update { it.copy(depression = score.coerceIn(0, 8)) }
  }

  fun setPleasure(score: Int) {
    _uiState.update { it.copy(pleasure = score.coerceIn(0, 8)) }
  }

  fun setAchievement(score: Int) {
    _uiState.update { it.copy(achievement = score.coerceIn(0, 8)) }
  }

  fun setStartTimer(enabled: Boolean) {
    _uiState.update { it.copy(startTimer = enabled) }
  }

  fun submit(onSaved: (startTimer: Boolean) -> Unit) {
    val snapshot = _uiState.value
    val dep = snapshot.depression ?: return
    val ple = snapshot.pleasure ?: return
    val ach = snapshot.achievement ?: return
    if (snapshot.isSaving) return

    _uiState.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      repository.recordPreCheckIn(
        activityId = activityId,
        depression = dep,
        pleasure = ple,
        achievement = ach,
        startTimer = snapshot.startTimer,
      )
      _uiState.update { it.copy(isSaving = false) }
      onSaved(snapshot.startTimer)
    }
  }

  companion object {
    fun factory(
      activityId: Long,
      repository: ActivationRepository,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          PreCheckInViewModel(activityId, repository) as T
      }
  }
}

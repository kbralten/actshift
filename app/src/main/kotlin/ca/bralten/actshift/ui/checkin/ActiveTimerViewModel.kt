package ca.bralten.actshift.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.bralten.actshift.data.repository.ActivationRepository
import ca.bralten.actshift.domain.model.ActivityCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveTimerUiState(
  val activityId: Long,
  val activityTitle: String = "",
  val category: ActivityCategory = ActivityCategory.BOTH,
  val durationMinutes: Int = 10,
  val remainingSeconds: Long = 0L,
  val isPaused: Boolean = false,
  val isFinished: Boolean = false,
)

class ActiveTimerViewModel(
  private val activityId: Long,
  private val repository: ActivationRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ActiveTimerUiState(activityId = activityId))
  val uiState: StateFlow<ActiveTimerUiState> = _uiState.asStateFlow()

  private var timerEndEpochMs: Long? = null
  private var pausedRemainingMs: Long? = null

  init {
    viewModelScope.launch {
      repository.observeActivityWithLog(activityId).collect { item ->
        val activity = item?.activity ?: return@collect
        if (!_uiState.value.isPaused) {
          timerEndEpochMs = activity.timerEndEpochMs
        }
        _uiState.update {
          it.copy(
            activityTitle = activity.title,
            category = activity.category,
            durationMinutes = activity.durationMinutes,
          )
        }
      }
    }

    viewModelScope.launch {
      while (true) {
        val state = _uiState.value
        val remainingMs =
          if (state.isPaused) {
            pausedRemainingMs ?: 0L
          } else {
            val endMs = timerEndEpochMs
            if (endMs != null) (endMs - System.currentTimeMillis()).coerceAtLeast(0L) else 0L
          }
        val seconds = (remainingMs + 999L) / 1000L
        val finished = !state.isPaused && timerEndEpochMs != null && remainingMs <= 0L
        _uiState.update {
          it.copy(
            remainingSeconds = seconds,
            isFinished = finished,
          )
        }
        delay(500L)
      }
    }
  }

  fun togglePause() {
    val currentlyPaused = _uiState.value.isPaused
    viewModelScope.launch {
      if (currentlyPaused) {
        val rem = pausedRemainingMs ?: 0L
        val newEnd = System.currentTimeMillis() + rem
        timerEndEpochMs = newEnd
        pausedRemainingMs = null
        repository.setTimerEnd(activityId, newEnd)
        _uiState.update { it.copy(isPaused = false) }
      } else {
        val rem = ((timerEndEpochMs ?: System.currentTimeMillis()) - System.currentTimeMillis()).coerceAtLeast(0L)
        pausedRemainingMs = rem
        val remainingSeconds = (rem + 999L) / 1000L
        repository.setTimerEnd(activityId, null, pausedRemainingSeconds = remainingSeconds)
        _uiState.update { it.copy(isPaused = true) }
      }
    }
  }

  fun extendFiveMinutes() {
    viewModelScope.launch {
      if (_uiState.value.isPaused) {
        val nextRemainingMs = (pausedRemainingMs ?: 0L) + 5L * 60_000L
        pausedRemainingMs = nextRemainingMs
        val remainingSeconds = (nextRemainingMs + 999L) / 1000L
        repository.setTimerEnd(activityId, null, pausedRemainingSeconds = remainingSeconds)
      } else {
        repository.extendTimer(activityId, extraMinutes = 5)
      }
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
          ActiveTimerViewModel(activityId, repository) as T
      }
  }
}

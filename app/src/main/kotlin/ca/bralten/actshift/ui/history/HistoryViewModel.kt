package ca.bralten.actshift.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.bralten.actshift.data.prefs.UserPreferencesRepository
import ca.bralten.actshift.data.repository.ActivationRepository
import ca.bralten.actshift.domain.model.ActivityWithLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
  val allLogs: List<ActivityWithLog> = emptyList(),
  val filteredLogs: List<ActivityWithLog> = emptyList(),
  val onlyWithReflections: Boolean = false,
  val countBothInFunAndAchievement: Boolean = true,
)

class HistoryViewModel(
  private val repository: ActivationRepository,
  preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

  private val onlyWithReflections = MutableStateFlow(false)

  val uiState: StateFlow<HistoryUiState> =
    combine(
      repository.observeCompletedHistory(),
      preferencesRepository.preferences,
      onlyWithReflections,
    ) { logs, prefs, reflectionsOnly ->
      val filtered =
        if (reflectionsOnly) {
          logs.filter { !it.log?.reflectionNote.isNullOrBlank() }
        } else {
          logs
        }
      HistoryUiState(
        allLogs = logs,
        filteredLogs = filtered,
        onlyWithReflections = reflectionsOnly,
        countBothInFunAndAchievement = prefs.countBothInFunAndAchievement,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = HistoryUiState(),
    )

  fun toggleOnlyWithReflections() {
    onlyWithReflections.value = !onlyWithReflections.value
  }

  fun updateReflectionNote(activityId: Long, note: String) {
    viewModelScope.launch {
      repository.updateReflectionNote(activityId, note)
    }
  }

  companion object {
    fun factory(
      repository: ActivationRepository,
      preferencesRepository: UserPreferencesRepository,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          HistoryViewModel(repository, preferencesRepository) as T
      }
  }
}

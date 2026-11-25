package ca.bralten.actshift.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.bralten.actshift.data.prefs.UserPreferencesRepository
import ca.bralten.actshift.data.repository.ActivationRepository
import ca.bralten.actshift.domain.InsightsCalculator
import ca.bralten.actshift.domain.model.ActivityWithLog
import ca.bralten.actshift.domain.model.InsightsSummary
import ca.bralten.actshift.domain.model.InsightsTimeRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class InsightsUiState(
  val allCompletedLogs: List<ActivityWithLog> = emptyList(),
  val selectedRange: InsightsTimeRange = InsightsTimeRange.ALL_TIME,
  val countBothInFunAndAchievement: Boolean = true,
  val summary: InsightsSummary =
    InsightsCalculator.compute(
      items = emptyList(),
      range = InsightsTimeRange.ALL_TIME,
      countBothInFunAndAchievement = true,
    ),
)

class InsightsViewModel(
  repository: ActivationRepository,
  preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

  private val selectedRange = MutableStateFlow(InsightsTimeRange.ALL_TIME)

  val uiState: StateFlow<InsightsUiState> =
    combine(
      repository.observeCompletedHistory(),
      preferencesRepository.preferences,
      selectedRange,
    ) { logs, prefs, range ->
      InsightsUiState(
        allCompletedLogs = logs,
        selectedRange = range,
        countBothInFunAndAchievement = prefs.countBothInFunAndAchievement,
        summary =
          InsightsCalculator.compute(
            items = logs,
            range = range,
            countBothInFunAndAchievement = prefs.countBothInFunAndAchievement,
          ),
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = InsightsUiState(),
    )

  fun selectRange(range: InsightsTimeRange) {
    selectedRange.value = range
  }

  companion object {
    fun factory(
      repository: ActivationRepository,
      preferencesRepository: UserPreferencesRepository,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          InsightsViewModel(repository, preferencesRepository) as T
      }
  }
}

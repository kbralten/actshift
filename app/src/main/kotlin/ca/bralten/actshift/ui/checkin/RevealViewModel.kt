package ca.bralten.actshift.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.bralten.actshift.data.repository.ActivationRepository
import ca.bralten.actshift.domain.DeltaFeedbackGenerator
import ca.bralten.actshift.domain.model.ActivityWithLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RevealUiState(
  val item: ActivityWithLog? = null,
  val feedbackCopy: String = "",
)

class RevealViewModel(
  private val activityId: Long,
  private val repository: ActivationRepository,
) : ViewModel() {

  val uiState: StateFlow<RevealUiState> =
    repository
      .observeActivityWithLog(activityId)
      .map { item ->
        val delta = item?.log?.computeDeltaSummary()
        RevealUiState(
          item = item,
          feedbackCopy = if (delta != null) DeltaFeedbackGenerator.generateFeedback(delta) else "",
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RevealUiState(),
      )

  fun saveReflectionNote(note: String) {
    viewModelScope.launch {
      repository.updateReflectionNote(activityId, note)
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
          RevealViewModel(activityId, repository) as T
      }
  }
}

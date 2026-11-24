package ca.bralten.actshift.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.bralten.actshift.data.repository.ActivationRepository
import ca.bralten.actshift.domain.model.ActivityWithLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
  repository: ActivationRepository,
) : ViewModel() {

  val completedLogs: StateFlow<List<ActivityWithLog>> =
    repository
      .observeCompletedHistory()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
      )

  companion object {
    fun factory(repository: ActivationRepository): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          HistoryViewModel(repository) as T
      }
  }
}

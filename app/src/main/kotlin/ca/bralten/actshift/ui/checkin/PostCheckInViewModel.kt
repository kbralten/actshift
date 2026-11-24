package ca.bralten.actshift.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.bralten.actshift.data.repository.ActivationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Intentionally excludes all pre-activity scores to guarantee blind post-activity logging.
 */
data class PostCheckInUiState(
  val activityId: Long,
  val activityTitle: String = "",
  val durationMinutes: Int = 10,
  val postDepression: Int? = null,
  val postPleasure: Int? = null,
  val postAchievement: Int? = null,
  val reflectionNote: String = "",
  val isLoading: Boolean = true,
  val isSaving: Boolean = false,
) {
  val canSubmit: Boolean
    get() =
      postDepression != null &&
        postPleasure != null &&
        postAchievement != null &&
        !isSaving &&
        !isLoading
}

class PostCheckInViewModel(
  private val activityId: Long,
  private val repository: ActivationRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(PostCheckInUiState(activityId = activityId))
  val uiState: StateFlow<PostCheckInUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      val activity = repository.getActivity(activityId)
      _uiState.update {
        if (activity != null) {
          it.copy(
            activityTitle = activity.title,
            durationMinutes = activity.durationMinutes,
            isLoading = false,
          )
        } else {
          it.copy(isLoading = false)
        }
      }
    }
  }

  fun setPostDepression(score: Int) {
    _uiState.update { it.copy(postDepression = score.coerceIn(0, 8)) }
  }

  fun setPostPleasure(score: Int) {
    _uiState.update { it.copy(postPleasure = score.coerceIn(0, 8)) }
  }

  fun setPostAchievement(score: Int) {
    _uiState.update { it.copy(postAchievement = score.coerceIn(0, 8)) }
  }

  fun updateReflectionNote(note: String) {
    _uiState.update { it.copy(reflectionNote = note) }
  }

  fun submit(onSaved: () -> Unit) {
    val snapshot = _uiState.value
    val dep = snapshot.postDepression ?: return
    val ple = snapshot.postPleasure ?: return
    val ach = snapshot.postAchievement ?: return
    if (snapshot.isSaving) return

    _uiState.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      repository.recordPostCheckIn(
        activityId = activityId,
        depression = dep,
        pleasure = ple,
        achievement = ach,
        reflectionNote = snapshot.reflectionNote,
      )
      _uiState.update { it.copy(isSaving = false) }
      onSaved()
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
          PostCheckInViewModel(activityId, repository) as T
      }
  }
}

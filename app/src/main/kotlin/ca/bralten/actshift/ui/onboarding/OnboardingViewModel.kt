package ca.bralten.actshift.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.bralten.actshift.data.prefs.UserPreferencesRepository
import ca.bralten.actshift.data.repository.ActivationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
  val currentStep: Int = 0,
  val calendarConnected: Boolean = false,
  val notificationsGranted: Boolean = false,
  val exactAlarmAllowed: Boolean = false,
)

class OnboardingViewModel(
  private val preferencesRepository: UserPreferencesRepository,
  private val repository: ActivationRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(OnboardingUiState())
  val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

  fun nextStep() {
    _uiState.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(2)) }
  }

  fun previousStep() {
    _uiState.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
  }

  fun updatePermissionStatus(
    calendarConnected: Boolean,
    notificationsGranted: Boolean,
    exactAlarmAllowed: Boolean,
  ) {
    _uiState.update {
      it.copy(
        calendarConnected = calendarConnected || it.calendarConnected,
        notificationsGranted = notificationsGranted,
        exactAlarmAllowed = exactAlarmAllowed,
      )
    }
  }

  fun connectCalendar() {
    viewModelScope.launch {
      val id = repository.ensureCalendarConnected()
      _uiState.update { it.copy(calendarConnected = id != null) }
    }
  }

  fun completeOnboarding(onCompleted: () -> Unit) {
    viewModelScope.launch {
      preferencesRepository.setOnboardingCompleted(true)
      onCompleted()
    }
  }

  companion object {
    fun factory(
      preferencesRepository: UserPreferencesRepository,
      repository: ActivationRepository,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          OnboardingViewModel(preferencesRepository, repository) as T
      }
  }
}

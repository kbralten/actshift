package ca.bralten.actshift.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.bralten.actshift.data.calendar.CalendarSyncEngine
import ca.bralten.actshift.data.prefs.UserPreferences
import ca.bralten.actshift.data.prefs.UserPreferencesRepository
import ca.bralten.actshift.data.repository.ActivationRepository
import ca.bralten.actshift.domain.model.ThemeMode
import ca.bralten.actshift.notifications.AlarmScheduler
import ca.bralten.actshift.notifications.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
  val preferences: UserPreferences = UserPreferences(),
  val hasCalendarPermission: Boolean = false,
  val hasNotificationPermission: Boolean = false,
  val canScheduleExactAlarms: Boolean = false,
  val syncMessage: String? = null,
)

class SettingsViewModel(
  private val preferencesRepository: UserPreferencesRepository,
  private val repository: ActivationRepository,
  private val calendarSyncEngine: CalendarSyncEngine,
  private val alarmScheduler: AlarmScheduler,
  private val notificationHelper: NotificationHelper,
) : ViewModel() {

  private val permissionTrigger = MutableStateFlow(0)
  private val syncMessage = MutableStateFlow<String?>(null)

  val uiState: StateFlow<SettingsUiState> =
    combine(
      preferencesRepository.preferences,
      permissionTrigger,
      syncMessage,
    ) { prefs, _, message ->
      SettingsUiState(
        preferences = prefs,
        hasCalendarPermission = calendarSyncEngine.hasCalendarPermissions(),
        hasNotificationPermission = notificationHelper.hasNotificationPermission(),
        canScheduleExactAlarms = alarmScheduler.canScheduleExactAlarms(),
        syncMessage = message,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = SettingsUiState(),
    )

  fun refreshPermissions() {
    permissionTrigger.value += 1
  }

  fun connectCalendar() {
    viewModelScope.launch {
      val id = repository.ensureCalendarConnected()
      syncMessage.value =
        if (id != null) {
          "Connected to \"Behavioural Activation\" calendar."
        } else {
          "Calendar permission is needed to create the dedicated calendar."
        }
      refreshPermissions()
    }
  }

  fun setCalendarSyncEnabled(enabled: Boolean) {
    viewModelScope.launch {
      preferencesRepository.setCalendarSyncEnabled(enabled)
      if (enabled) {
        repository.ensureCalendarConnected()
      }
    }
  }

  fun syncNow() {
    viewModelScope.launch {
      repository.runMaintenanceSweep()
      syncMessage.value = "Calendar & schedule synced."
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
  }

  fun setUseDynamicColor(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setUseDynamicColor(enabled) }
  }

  fun setCountBothInFunAndAchievement(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setCountBothInFunAndAchievement(enabled) }
  }

  fun clearSyncMessage() {
    syncMessage.value = null
  }

  companion object {
    fun factory(
      preferencesRepository: UserPreferencesRepository,
      repository: ActivationRepository,
      calendarSyncEngine: CalendarSyncEngine,
      alarmScheduler: AlarmScheduler,
      notificationHelper: NotificationHelper,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          SettingsViewModel(
            preferencesRepository,
            repository,
            calendarSyncEngine,
            alarmScheduler,
            notificationHelper,
          ) as T
      }
  }
}

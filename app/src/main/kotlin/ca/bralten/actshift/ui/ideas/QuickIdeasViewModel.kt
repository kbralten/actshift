package ca.bralten.actshift.ui.ideas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.bralten.actshift.data.prefs.UserPreferencesRepository
import ca.bralten.actshift.data.repository.ActivationRepository
import ca.bralten.actshift.data.repository.QuickIdeaRepository
import ca.bralten.actshift.domain.ActivitySuggestion
import ca.bralten.actshift.domain.ActivitySuggestionLibrary
import ca.bralten.actshift.domain.EnergyLevel
import ca.bralten.actshift.domain.QuickTimePresets
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.RecurrenceType
import ca.bralten.actshift.domain.model.ScheduleTimeGranularity
import ca.bralten.actshift.ui.components.TimeFormatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class QuickIdeasUiState(
  val allIdeas: List<ActivitySuggestion> = emptyList(),
  val filteredIdeas: List<ActivitySuggestion> = emptyList(),
  val selectedEnergy: EnergyLevel = EnergyLevel.LOW,
  val selectedCategory: ActivityCategory? = null,
  val customOnly: Boolean = false,
  val granularity: ScheduleTimeGranularity = ScheduleTimeGranularity.HOUR,
  val snackbarMessage: String? = null,
)

private data class IdeaFilterState(
  val energy: EnergyLevel = EnergyLevel.LOW,
  val category: ActivityCategory? = null,
  val customOnly: Boolean = false,
)

class QuickIdeasViewModel(
  private val quickIdeaRepository: QuickIdeaRepository,
  private val activationRepository: ActivationRepository,
  preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

  private val filterState = MutableStateFlow(IdeaFilterState())
  private val snackbarMessage = MutableStateFlow<String?>(null)

  init {
    viewModelScope.launch { quickIdeaRepository.ensureSeeded() }
  }

  val uiState: StateFlow<QuickIdeasUiState> =
    combine(
      quickIdeaRepository.observeIdeas(),
      preferencesRepository.preferences,
      filterState,
      snackbarMessage,
    ) { ideas, prefs, filter, message ->
      val byFilter =
        ActivitySuggestionLibrary
          .filter(
            energyLevel = filter.energy,
            category = filter.category,
            source = ideas,
          ).let { base ->
            if (filter.customOnly) base.filter { !it.isStarter } else base
          }

      QuickIdeasUiState(
        allIdeas = ideas,
        filteredIdeas = byFilter,
        selectedEnergy = filter.energy,
        selectedCategory = filter.category,
        customOnly = filter.customOnly,
        granularity = prefs.scheduleTimeGranularity,
        snackbarMessage = message,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = QuickIdeasUiState(),
    )

  fun selectEnergy(energy: EnergyLevel) {
    filterState.value = filterState.value.copy(energy = energy)
  }

  fun selectCategory(category: ActivityCategory?) {
    filterState.value = filterState.value.copy(category = category)
  }

  fun toggleCustomOnly() {
    filterState.value = filterState.value.copy(customOnly = !filterState.value.customOnly)
  }

  fun addIdea(
    title: String,
    durationMinutes: Int,
    category: ActivityCategory,
    energyLevel: EnergyLevel,
  ) {
    viewModelScope.launch {
      quickIdeaRepository.addIdea(
        title = title,
        durationMinutes = durationMinutes,
        category = category,
        energyLevel = energyLevel,
      )
      filterState.value = filterState.value.copy(energy = energyLevel)
      snackbarMessage.value = "Added \"${title.trim()}\" to ${energyLevel.label} energy."
    }
  }

  fun updateIdea(
    dbId: Long,
    title: String,
    durationMinutes: Int,
    category: ActivityCategory,
    energyLevel: EnergyLevel,
  ) {
    viewModelScope.launch {
      quickIdeaRepository.updateIdea(
        dbId = dbId,
        title = title,
        durationMinutes = durationMinutes,
        category = category,
        energyLevel = energyLevel,
      )
      snackbarMessage.value = "Updated \"${title.trim()}\"."
    }
  }

  fun deleteIdea(dbId: Long) {
    viewModelScope.launch {
      quickIdeaRepository.deleteIdea(dbId)
      snackbarMessage.value = "Idea removed."
    }
  }

  fun restoreStarterIdeas() {
    viewModelScope.launch {
      val restored = quickIdeaRepository.restoreStarterIdeas()
      snackbarMessage.value =
        if (restored > 0) {
          "Restored $restored starter ${if (restored == 1) "idea" else "ideas"}."
        } else {
          "All starter ideas are already present."
        }
    }
  }

  fun startNow(
    suggestion: ActivitySuggestion,
    onCreated: (Long) -> Unit,
  ) {
    viewModelScope.launch {
      val id =
        activationRepository.createActivity(
          title = suggestion.title,
          category = suggestion.category,
          durationMinutes = suggestion.durationMinutes,
          startEpochMs = System.currentTimeMillis(),
          recurrenceType = RecurrenceType.ONCE,
        )
      onCreated(id)
    }
  }

  fun scheduleNextSlot(suggestion: ActivitySuggestion) {
    viewModelScope.launch {
      val granularity = uiState.value.granularity
      val targetMs =
        QuickTimePresets
          .nextSlotDateTime(ZonedDateTime.now(), granularity)
          .toInstant()
          .toEpochMilli()
      activationRepository.createActivity(
        title = suggestion.title,
        category = suggestion.category,
        durationMinutes = suggestion.durationMinutes,
        startEpochMs = targetMs,
        recurrenceType = RecurrenceType.ONCE,
      )
      snackbarMessage.value =
        "Scheduled \"${suggestion.title}\" for ${TimeFormatters.formatActivityStart(targetMs)}"
    }
  }

  fun clearSnackbarMessage() {
    snackbarMessage.value = null
  }

  companion object {
    fun factory(
      quickIdeaRepository: QuickIdeaRepository,
      activationRepository: ActivationRepository,
      preferencesRepository: UserPreferencesRepository,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          QuickIdeasViewModel(quickIdeaRepository, activationRepository, preferencesRepository) as T
      }
  }
}

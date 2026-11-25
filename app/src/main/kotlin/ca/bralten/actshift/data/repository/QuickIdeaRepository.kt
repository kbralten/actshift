package ca.bralten.actshift.data.repository

import ca.bralten.actshift.data.local.dao.QuickIdeaDao
import ca.bralten.actshift.data.local.entity.QuickIdeaEntity
import ca.bralten.actshift.data.prefs.UserPreferencesRepository
import ca.bralten.actshift.domain.ActivitySuggestion
import ca.bralten.actshift.domain.ActivitySuggestionLibrary
import ca.bralten.actshift.domain.EnergyLevel
import ca.bralten.actshift.domain.model.ActivityCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class QuickIdeaRepository(
  private val quickIdeaDao: QuickIdeaDao,
  private val preferencesRepository: UserPreferencesRepository,
) {
  private val seedMutex = Mutex()

  fun observeIdeas(): Flow<List<ActivitySuggestion>> =
    quickIdeaDao.observeAll().map { entities -> entities.map { it.toDomain() } }

  suspend fun ensureSeeded() {
    seedMutex.withLock {
      if (preferencesRepository.getCurrent().starterIdeasSeeded) return
      if (quickIdeaDao.countAll() == 0) {
        quickIdeaDao.insertAll(starterEntities())
      }
      preferencesRepository.setStarterIdeasSeeded(true)
    }
  }

  suspend fun addIdea(
    title: String,
    durationMinutes: Int,
    category: ActivityCategory,
    energyLevel: EnergyLevel,
  ): Long {
    val cleanTitle = title.trim().ifEmpty { "Mindful Activity" }
    val safeDuration = durationMinutes.coerceIn(1, 240)
    return quickIdeaDao.insert(
      QuickIdeaEntity(
        title = cleanTitle,
        durationMinutes = safeDuration,
        category = category,
        energyLevel = energyLevel,
        isStarter = false,
        starterKey = null,
      ),
    )
  }

  suspend fun updateIdea(
    dbId: Long,
    title: String,
    durationMinutes: Int,
    category: ActivityCategory,
    energyLevel: EnergyLevel,
  ) {
    val cleanTitle = title.trim().ifEmpty { "Mindful Activity" }
    val safeDuration = durationMinutes.coerceIn(1, 240)
    quickIdeaDao.update(
      id = dbId,
      title = cleanTitle,
      durationMinutes = safeDuration,
      category = category,
      energyLevel = energyLevel,
    )
  }

  suspend fun deleteIdea(dbId: Long) {
    quickIdeaDao.deleteById(dbId)
  }

  suspend fun restoreStarterIdeas(): Int =
    seedMutex.withLock {
      val existingKeys = quickIdeaDao.getExistingStarterKeys().toSet()
      val missing =
        starterEntities().filter { entity ->
          val key = entity.starterKey
          key != null && key !in existingKeys
        }
      if (missing.isNotEmpty()) {
        quickIdeaDao.insertAll(missing)
      }
      preferencesRepository.setStarterIdeasSeeded(true)
      missing.size
    }

  private fun starterEntities(): List<QuickIdeaEntity> =
    ActivitySuggestionLibrary.allSuggestions.map { suggestion ->
      QuickIdeaEntity(
        title = suggestion.title,
        durationMinutes = suggestion.durationMinutes,
        category = suggestion.category,
        energyLevel = suggestion.energyLevel,
        isStarter = true,
        starterKey = suggestion.id,
      )
    }
}

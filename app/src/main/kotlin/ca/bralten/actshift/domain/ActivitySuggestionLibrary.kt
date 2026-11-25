package ca.bralten.actshift.domain

import ca.bralten.actshift.domain.model.ActivityCategory
import kotlin.random.Random

enum class EnergyLevel(val label: String, val description: String) {
  VERY_LOW("Very Low", "Zero-pressure, minimal movement"),
  LOW("Low", "Gentle & short around home"),
  MODERATE("Moderate", "Light movement or small errands"),
}

data class ActivitySuggestion(
  val id: String,
  val title: String,
  val durationMinutes: Int,
  val category: ActivityCategory,
  val energyLevel: EnergyLevel,
)

data class QuickPickSession(
  val initialEnergyLevel: EnergyLevel,
  val initialCategory: ActivityCategory?,
  val shuffledSuggestions: List<ActivitySuggestion>,
)

/**
 * Curated bank of low-barrier 5–15 minute Behavioural Activation ideas
 * organized by current energy level and Fun / Achievement category.
 */
object ActivitySuggestionLibrary {

  val allSuggestions: List<ActivitySuggestion> =
    listOf(
      // Very Low (9)
      ActivitySuggestion("vl_ple_1", "Listen to one favourite song", 5, ActivityCategory.PLEASURE, EnergyLevel.VERY_LOW),
      ActivitySuggestion("vl_ple_2", "Sit by a sunny window or step outside", 5, ActivityCategory.PLEASURE, EnergyLevel.VERY_LOW),
      ActivitySuggestion("vl_ple_3", "Make a warm cup of tea or coffee", 5, ActivityCategory.PLEASURE, EnergyLevel.VERY_LOW),
      ActivitySuggestion("vl_ach_1", "Drink a full glass of water", 5, ActivityCategory.ACHIEVEMENT, EnergyLevel.VERY_LOW),
      ActivitySuggestion("vl_ach_2", "Clear 5 items off a table or desk", 5, ActivityCategory.ACHIEVEMENT, EnergyLevel.VERY_LOW),
      ActivitySuggestion("vl_ach_3", "Open the curtains and let daylight in", 5, ActivityCategory.ACHIEVEMENT, EnergyLevel.VERY_LOW),
      ActivitySuggestion("vl_both_1", "Wash your face and brush your teeth", 5, ActivityCategory.BOTH, EnergyLevel.VERY_LOW),
      ActivitySuggestion("vl_both_2", "Stretch gently for 5 minutes", 5, ActivityCategory.BOTH, EnergyLevel.VERY_LOW),
      ActivitySuggestion("vl_both_3", "Send one short text to someone you care about", 5, ActivityCategory.BOTH, EnergyLevel.VERY_LOW),

      // Low (9)
      ActivitySuggestion("low_ple_1", "Read a few pages of a book or comic", 10, ActivityCategory.PLEASURE, EnergyLevel.LOW),
      ActivitySuggestion("low_ple_2", "Watch a short funny clip or nature video", 10, ActivityCategory.PLEASURE, EnergyLevel.LOW),
      ActivitySuggestion("low_ple_3", "Doodle or colour for 10 minutes", 10, ActivityCategory.PLEASURE, EnergyLevel.LOW),
      ActivitySuggestion("low_ach_1", "Get the mail", 5, ActivityCategory.ACHIEVEMENT, EnergyLevel.LOW),
      ActivitySuggestion("low_ach_2", "Load or unload the dishwasher", 10, ActivityCategory.ACHIEVEMENT, EnergyLevel.LOW),
      ActivitySuggestion("low_ach_3", "Reply to one overdue message or email", 10, ActivityCategory.ACHIEVEMENT, EnergyLevel.LOW),
      ActivitySuggestion("low_both_1", "Take a warm shower", 10, ActivityCategory.BOTH, EnergyLevel.LOW),
      ActivitySuggestion("low_both_2", "Make a simple, nourishing snack", 10, ActivityCategory.BOTH, EnergyLevel.LOW),
      ActivitySuggestion("low_both_3", "Water a plant or tend a small corner", 10, ActivityCategory.BOTH, EnergyLevel.LOW),

      // Moderate (9)
      ActivitySuggestion("mod_ple_1", "Take a leisurely walk around the block", 15, ActivityCategory.PLEASURE, EnergyLevel.MODERATE),
      ActivitySuggestion("mod_ple_2", "Sit at a café or park bench for a few minutes", 15, ActivityCategory.PLEASURE, EnergyLevel.MODERATE),
      ActivitySuggestion("mod_ple_3", "Work on a hobby for 15 minutes", 15, ActivityCategory.PLEASURE, EnergyLevel.MODERATE),
      ActivitySuggestion("mod_ach_1", "Run one quick errand", 15, ActivityCategory.ACHIEVEMENT, EnergyLevel.MODERATE),
      ActivitySuggestion("mod_ach_2", "Fold a small pile of laundry", 10, ActivityCategory.ACHIEVEMENT, EnergyLevel.MODERATE),
      ActivitySuggestion("mod_ach_3", "Tidy one room for 10 minutes", 10, ActivityCategory.ACHIEVEMENT, EnergyLevel.MODERATE),
      ActivitySuggestion("mod_both_1", "Walk the dog or explore a nearby street", 15, ActivityCategory.BOTH, EnergyLevel.MODERATE),
      ActivitySuggestion("mod_both_2", "Cook a simple meal with music on", 15, ActivityCategory.BOTH, EnergyLevel.MODERATE),
      ActivitySuggestion("mod_both_3", "Call a friend or family member to catch up", 15, ActivityCategory.BOTH, EnergyLevel.MODERATE),
    )

  fun filter(
    energyLevel: EnergyLevel? = null,
    category: ActivityCategory? = null,
    source: List<ActivitySuggestion> = allSuggestions,
  ): List<ActivitySuggestion> =
    source.filter { suggestion ->
      (energyLevel == null || suggestion.energyLevel == energyLevel) &&
        (category == null || suggestion.category == category)
    }

  fun createRandomizedSession(random: Random = Random.Default): QuickPickSession {
    val categoryOptions: List<ActivityCategory?> = listOf(null) + ActivityCategory.entries
    return QuickPickSession(
      initialEnergyLevel = EnergyLevel.entries[random.nextInt(EnergyLevel.entries.size)],
      initialCategory = categoryOptions[random.nextInt(categoryOptions.size)],
      shuffledSuggestions = allSuggestions.shuffled(random),
    )
  }

  fun pickRandom(
    energyLevel: EnergyLevel? = null,
    category: ActivityCategory? = null,
    excludeId: String? = null,
    random: Random = Random.Default,
  ): ActivitySuggestion? {
    val candidates = filter(energyLevel = energyLevel, category = category)
    if (candidates.isEmpty()) return null
    val pool =
      if (candidates.size > 1 && excludeId != null) {
        candidates.filterNot { it.id == excludeId }
      } else {
        candidates
      }
    return pool[random.nextInt(pool.size)]
  }
}

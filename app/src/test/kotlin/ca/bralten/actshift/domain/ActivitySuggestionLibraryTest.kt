package ca.bralten.actshift.domain

import ca.bralten.actshift.domain.model.ActivityCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ActivitySuggestionLibraryTest {

  @Test
  fun curatedBankCoversAllEnergyLevelsAndCategoriesWith5To15MinDurations() {
    val all = ActivitySuggestionLibrary.allSuggestions
    assertEquals(27, all.size)
    assertEquals(all.size, all.map { it.id }.toSet().size)
    assertTrue(all.all { it.title.isNotBlank() })
    assertTrue(all.all { it.durationMinutes in setOf(5, 10, 15) })

    for (energy in EnergyLevel.entries) {
      for (category in ActivityCategory.entries) {
        val bucket = ActivitySuggestionLibrary.filter(energyLevel = energy, category = category)
        assertTrue(
          "Expected >= 3 suggestions for $energy x $category",
          bucket.size >= 3,
        )
      }
    }
  }

  @Test
  fun filterReturnsExactSubset() {
    val veryLowFun =
      ActivitySuggestionLibrary.filter(
        energyLevel = EnergyLevel.VERY_LOW,
        category = ActivityCategory.PLEASURE,
      )
    assertEquals(3, veryLowFun.size)
    assertTrue(veryLowFun.all { it.energyLevel == EnergyLevel.VERY_LOW && it.category == ActivityCategory.PLEASURE })

    val allLow = ActivitySuggestionLibrary.filter(energyLevel = EnergyLevel.LOW)
    assertEquals(9, allLow.size)
    assertTrue(allLow.all { it.energyLevel == EnergyLevel.LOW })
  }

  @Test
  fun pickRandomExcludesPreviousIdWhenMultipleCandidatesExist() {
    val random = Random(42)
    val first = ActivitySuggestionLibrary.pickRandom(
      energyLevel = EnergyLevel.MODERATE,
      category = ActivityCategory.BOTH,
      random = random,
    )
    assertNotNull(first)

    repeat(10) {
      val next =
        ActivitySuggestionLibrary.pickRandom(
          energyLevel = EnergyLevel.MODERATE,
          category = ActivityCategory.BOTH,
          excludeId = first!!.id,
          random = random,
        )
      assertNotNull(next)
      assertNotEquals(first.id, next!!.id)
    }
  }

  @Test
  fun createRandomizedSessionShufflesAllIdeasAndVariesInitialFilters() {
    val sessions = (1..20).map { seed ->
      ActivitySuggestionLibrary.createRandomizedSession(Random(seed))
    }

    for (session in sessions) {
      assertEquals(ActivitySuggestionLibrary.allSuggestions.size, session.shuffledSuggestions.size)
      assertEquals(
        ActivitySuggestionLibrary.allSuggestions.map { it.id }.toSet(),
        session.shuffledSuggestions.map { it.id }.toSet(),
      )
    }

    assertTrue(sessions.map { it.initialEnergyLevel }.toSet().size > 1)
    assertTrue(sessions.map { it.initialCategory }.toSet().size > 1)
    assertTrue(sessions.map { s -> s.shuffledSuggestions.map { it.id } }.toSet().size > 1)
  }
}

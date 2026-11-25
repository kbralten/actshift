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
  fun curatedBankCoversLowMedHighEnergyLevelsAndCategoriesWith5To15MinDurations() {
    assertEquals(listOf("Low", "Med", "High"), EnergyLevel.entries.map { it.label })

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
    val lowFun =
      ActivitySuggestionLibrary.filter(
        energyLevel = EnergyLevel.LOW,
        category = ActivityCategory.PLEASURE,
      )
    assertEquals(3, lowFun.size)
    assertTrue(lowFun.all { it.energyLevel == EnergyLevel.LOW && it.category == ActivityCategory.PLEASURE })

    val allMed = ActivitySuggestionLibrary.filter(energyLevel = EnergyLevel.MEDIUM)
    assertEquals(9, allMed.size)
    assertTrue(allMed.all { it.energyLevel == EnergyLevel.MEDIUM })
  }

  @Test
  fun pickRandomExcludesPreviousIdWhenMultipleCandidatesExist() {
    val random = Random(42)
    val first = ActivitySuggestionLibrary.pickRandom(
      energyLevel = EnergyLevel.HIGH,
      category = ActivityCategory.BOTH,
      random = random,
    )
    assertNotNull(first)

    repeat(10) {
      val next =
        ActivitySuggestionLibrary.pickRandom(
          energyLevel = EnergyLevel.HIGH,
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
      ActivitySuggestionLibrary.createRandomizedSession(random = Random(seed))
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

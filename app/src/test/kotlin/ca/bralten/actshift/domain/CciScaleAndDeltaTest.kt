package ca.bralten.actshift.domain

import ca.bralten.actshift.domain.model.ActivationLog
import ca.bralten.actshift.domain.model.CciScaleLevel
import ca.bralten.actshift.domain.model.DeltaSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CciScaleAndDeltaTest {

  @Test
  fun cciScaleMatchesOfficialPdfLabels() {
    val expected = mapOf(
      0 to "Absolutely None",
      1 to "Minimal",
      2 to "Slight",
      3 to "Mild",
      4 to "Moderate",
      5 to "Much",
      6 to "Higher",
      7 to "Very High",
      8 to "Extreme",
    )
    for ((score, label) in expected) {
      val level = CciScaleLevel.fromScore(score)
      assertEquals(score, level.score)
      assertEquals(label, level.label)
    }
  }

  @Test
  fun deltaSummaryMatchesWorksheetExample() {
    // From CCI Worksheet PDF: Get the mail: Dep 6->3, Ple 0->0, Ach 0->1
    val log = ActivationLog(
      id = 1L,
      activityId = 10L,
      preDepression = 6,
      prePleasure = 0,
      preAchievement = 0,
      preLoggedAtEpochMs = 1_000L,
      postDepression = 3,
      postPleasure = 0,
      postAchievement = 1,
      postLoggedAtEpochMs = 2_000L,
      reflectionNote = "Fresh air helped",
    )

    val delta = log.computeDeltaSummary()!!
    assertEquals(-3, delta.depressionDelta)
    assertEquals(0, delta.pleasureDelta)
    assertEquals(1, delta.achievementDelta)
    assertTrue(delta.hasAnyPositiveShift)
    assertFalse(delta.isCompletelyFlat)
  }

  @Test
  fun deltaFeedbackIsGroundedForPositiveFlatAndWorsenedMood() {
    val improvedDep = DeltaFeedbackGenerator.generateFeedback(
      DeltaSummary(depressionDelta = -2, pleasureDelta = 0, achievementDelta = 1),
    )
    assertTrue(improvedDep.contains("needle", ignoreCase = true))

    val improvedPleasureOnly = DeltaFeedbackGenerator.generateFeedback(
      DeltaSummary(depressionDelta = 0, pleasureDelta = 1, achievementDelta = 0),
    )
    assertTrue(improvedPleasureOnly.contains("1-point", ignoreCase = true))

    val flat = DeltaFeedbackGenerator.generateFeedback(
      DeltaSummary(depressionDelta = 0, pleasureDelta = 0, achievementDelta = 0),
    )
    assertTrue(flat.contains("Showing up", ignoreCase = true))

    val worsened = DeltaFeedbackGenerator.generateFeedback(
      DeltaSummary(depressionDelta = 2, pleasureDelta = -1, achievementDelta = 0),
    )
    assertTrue(worsened.contains("honestly", ignoreCase = true))
  }
}

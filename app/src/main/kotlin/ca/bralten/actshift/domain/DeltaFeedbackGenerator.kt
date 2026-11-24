package ca.bralten.actshift.domain

import ca.bralten.actshift.domain.model.DeltaSummary

/**
 * Generates grounded, non-punitive feedback for the Reveal screen.
 */
object DeltaFeedbackGenerator {
  fun generateFeedback(delta: DeltaSummary): String =
    when {
      delta.hasAnyPositiveShift && delta.depressionDelta < 0 ->
        "Notice the shift — taking action moved the needle, even in a small way."
      delta.hasAnyPositiveShift ->
        "Even a 1-point shift in pleasure or achievement is real evidence that action matters."
      delta.isCompletelyFlat ->
        "Showing up is the hardest part. Even a steady score is useful data."
      else ->
        "Thank you for checking in honestly. Tough moments happen — noticing how you feel is part of the practice."
    }
}

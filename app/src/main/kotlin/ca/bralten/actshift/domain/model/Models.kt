package ca.bralten.actshift.domain.model

import java.time.DayOfWeek

/**
 * Activity category aligned with Behavioural Activation's Fun & Achievement balance.
 */
enum class ActivityCategory(val label: String) {
  PLEASURE("Pleasure / Fun"),
  ACHIEVEMENT("Achievement"),
  BOTH("Both / Mixed"),
}

/**
 * Lifecycle status of a scheduled Behavioural Activation instance.
 * Intentionally avoids any punitive "FAILED" state.
 */
enum class ActivityStatus {
  SCHEDULED,
  IN_PROGRESS,
  COMPLETED,
  UNCOMPLETED,
  DISCARDED,
}

/**
 * Official Centre for Clinical Interventions (CCI) 0–8 Behavioural Activation Worksheet scale.
 */
enum class CciScaleLevel(val score: Int, val label: String) {
  ABSOLUTELY_NONE(0, "Absolutely None"),
  MINIMAL(1, "Minimal"),
  SLIGHT(2, "Slight"),
  MILD(3, "Mild"),
  MODERATE(4, "Moderate"),
  MUCH(5, "Much"),
  HIGHER(6, "Higher"),
  VERY_HIGH(7, "Very High"),
  EXTREME(8, "Extreme");

  companion object {
    val ANCHOR_SCORES = setOf(0, 2, 4, 6, 8)

    fun fromScore(score: Int): CciScaleLevel =
      entries.firstOrNull { it.score == score }
        ?: throw IllegalArgumentException("CCI score must be between 0 and 8, got $score")
  }
}

/**
 * Supported recurrence schedules for Behavioural Activation activities.
 */
enum class RecurrenceType(val label: String) {
  ONCE("Once"),
  DAILY("Daily"),
  WEEKDAYS("Weekdays"),
  WEEKLY("Weekly");

  fun toRRule(weeklyDays: Set<DayOfWeek> = emptySet()): String? =
    when (this) {
      ONCE -> null
      DAILY -> "FREQ=DAILY"
      WEEKDAYS -> "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
      WEEKLY -> {
        val days = weeklyDays.sortedBy { it.value }
        if (days.isEmpty()) {
          "FREQ=WEEKLY"
        } else {
          "FREQ=WEEKLY;BYDAY=${days.joinToString(",") { it.toRRuleCode() }}"
        }
      }
    }

  companion object {
    fun fromRRule(rrule: String?): Pair<RecurrenceType, Set<DayOfWeek>> {
      if (rrule.isNullOrBlank()) return ONCE to emptySet()
      val normalized = rrule.trim().uppercase()
      if (normalized.startsWith("FREQ=DAILY")) {
        return DAILY to emptySet()
      }
      if (normalized.startsWith("FREQ=WEEKLY")) {
        val byDayPart =
          normalized
            .split(";")
            .firstOrNull { it.startsWith("BYDAY=") }
            ?.removePrefix("BYDAY=")
        val parsedDays =
          byDayPart
            ?.split(",")
            ?.mapNotNull { code -> dayOfWeekFromRRuleCode(code.trim()) }
            ?.toSet()
            ?: emptySet()
        val weekdays = setOf(
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY,
        )
        return if (parsedDays == weekdays) {
          WEEKDAYS to emptySet()
        } else {
          WEEKLY to parsedDays
        }
      }
      return ONCE to emptySet()
    }

    private fun DayOfWeek.toRRuleCode(): String =
      when (this) {
        DayOfWeek.MONDAY -> "MO"
        DayOfWeek.TUESDAY -> "TU"
        DayOfWeek.WEDNESDAY -> "WE"
        DayOfWeek.THURSDAY -> "TH"
        DayOfWeek.FRIDAY -> "FR"
        DayOfWeek.SATURDAY -> "SA"
        DayOfWeek.SUNDAY -> "SU"
      }

    private fun dayOfWeekFromRRuleCode(code: String): DayOfWeek? =
      when (code) {
        "MO" -> DayOfWeek.MONDAY
        "TU" -> DayOfWeek.TUESDAY
        "WE" -> DayOfWeek.WEDNESDAY
        "TH" -> DayOfWeek.THURSDAY
        "FR" -> DayOfWeek.FRIDAY
        "SA" -> DayOfWeek.SATURDAY
        "SU" -> DayOfWeek.SUNDAY
        else -> null
      }
  }
}

enum class ThemeMode(val label: String) {
  SYSTEM("System"),
  LIGHT("Light"),
  DARK("Dark"),
}

enum class ScheduleTimeGranularity(val label: String) {
  HOUR("1 Hour"),
  HALF_HOUR("30 Min"),
}

data class DeltaSummary(
  val depressionDelta: Int,
  val pleasureDelta: Int,
  val achievementDelta: Int,
) {
  val hasAnyPositiveShift: Boolean
    get() = depressionDelta < 0 || pleasureDelta > 0 || achievementDelta > 0

  val isCompletelyFlat: Boolean
    get() = depressionDelta == 0 && pleasureDelta == 0 && achievementDelta == 0
}

data class ScheduledActivity(
  val id: Long,
  val templateId: Long?,
  val title: String,
  val category: ActivityCategory,
  val scheduledStartEpochMs: Long,
  val durationMinutes: Int,
  val status: ActivityStatus,
  val calendarEventId: Long?,
  val calendarInstanceBeginMs: Long?,
  val startedAtEpochMs: Long?,
  val timerEndEpochMs: Long?,
  val completedAtEpochMs: Long?,
  val snoozeCount: Int,
) {
  val scheduledEndEpochMs: Long
    get() = scheduledStartEpochMs + durationMinutes * 60_000L
}

data class ActivationLog(
  val id: Long,
  val activityId: Long,
  val preDepression: Int,
  val prePleasure: Int,
  val preAchievement: Int,
  val preLoggedAtEpochMs: Long,
  val postDepression: Int?,
  val postPleasure: Int?,
  val postAchievement: Int?,
  val postLoggedAtEpochMs: Long?,
  val reflectionNote: String?,
) {
  val isComplete: Boolean
    get() = postDepression != null && postPleasure != null && postAchievement != null && postLoggedAtEpochMs != null

  fun computeDeltaSummary(): DeltaSummary? {
    val postDep = postDepression ?: return null
    val postPle = postPleasure ?: return null
    val postAch = postAchievement ?: return null
    return DeltaSummary(
      depressionDelta = postDep - preDepression,
      pleasureDelta = postPle - prePleasure,
      achievementDelta = postAch - preAchievement,
    )
  }
}

data class ActivityWithLog(
  val activity: ScheduledActivity,
  val log: ActivationLog?,
)

package ca.bralten.actshift.domain

import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.ActivityWithLog
import ca.bralten.actshift.domain.model.AverageDeltaStats
import ca.bralten.actshift.domain.model.CategoryDeltaBreakdown
import ca.bralten.actshift.domain.model.DeltaSummary
import ca.bralten.actshift.domain.model.FunAchievementBalance
import ca.bralten.actshift.domain.model.InsightsSummary
import ca.bralten.actshift.domain.model.InsightsTimeRange
import ca.bralten.actshift.domain.model.MoodLifterStat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.abs

/**
 * Pure-Kotlin aggregation for mood shift averages, Fun vs. Achievement balance,
 * and top mood-lifting activities.
 */
object InsightsCalculator {

  fun effectiveTimestampMs(item: ActivityWithLog): Long =
    item.activity.completedAtEpochMs
      ?: item.log?.postLoggedAtEpochMs
      ?: item.activity.scheduledStartEpochMs

  fun filterByTimeRange(
    items: List<ActivityWithLog>,
    range: InsightsTimeRange,
    nowMs: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
  ): List<ActivityWithLog> {
    val completed = items.filter { it.log?.computeDeltaSummary() != null }
    val days = range.days ?: return completed
    val startOfWindow =
      ZonedDateTime
        .ofInstant(Instant.ofEpochMilli(nowMs), zoneId)
        .toLocalDate()
        .minusDays(days - 1)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()
    return completed.filter { effectiveTimestampMs(it) >= startOfWindow }
  }

  fun compute(
    items: List<ActivityWithLog>,
    range: InsightsTimeRange,
    countBothInFunAndAchievement: Boolean,
    nowMs: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
  ): InsightsSummary {
    val filtered = filterByTimeRange(items, range, nowMs, zoneId)
    val paired = filtered.mapNotNull { item ->
      item.log?.computeDeltaSummary()?.let { delta -> item to delta }
    }

    val overall = averageStatsFor(paired.map { it.second })
    val pleasureItems =
      paired.filter { (item, _) ->
        item.activity.category == ActivityCategory.PLEASURE ||
          (countBothInFunAndAchievement && item.activity.category == ActivityCategory.BOTH)
      }
    val achievementItems =
      paired.filter { (item, _) ->
        item.activity.category == ActivityCategory.ACHIEVEMENT ||
          (countBothInFunAndAchievement && item.activity.category == ActivityCategory.BOTH)
      }
    val bothItems = paired.filter { (item, _) -> item.activity.category == ActivityCategory.BOTH }

    val breakdown =
      CategoryDeltaBreakdown(
        overall = overall,
        pleasure = averageStatsFor(pleasureItems.map { it.second }),
        achievement = averageStatsFor(achievementItems.map { it.second }),
        both = averageStatsFor(bothItems.map { it.second }),
        countBothInFunAndAchievement = countBothInFunAndAchievement,
      )

    val balance = computeBalance(filtered, countBothInFunAndAchievement)
    val topMoodLifters = computeTopMoodLifters(paired)

    return InsightsSummary(
      timeRange = range,
      totalCompletedCount = filtered.size,
      deltaBreakdown = breakdown,
      balance = balance,
      topMoodLifters = topMoodLifters,
      headlineCopy = formatHeadlineCopy(overall),
    )
  }

  private fun averageStatsFor(deltas: List<DeltaSummary>): AverageDeltaStats {
    if (deltas.isEmpty()) {
      return AverageDeltaStats(
        count = 0,
        avgDepressionDelta = null,
        avgPleasureDelta = null,
        avgAchievementDelta = null,
      )
    }
    return AverageDeltaStats(
      count = deltas.size,
      avgDepressionDelta = deltas.map { it.depressionDelta }.average(),
      avgPleasureDelta = deltas.map { it.pleasureDelta }.average(),
      avgAchievementDelta = deltas.map { it.achievementDelta }.average(),
    )
  }

  private fun computeBalance(
    items: List<ActivityWithLog>,
    countBothInFunAndAchievement: Boolean,
  ): FunAchievementBalance {
    val pleasureCount = items.count { it.activity.category == ActivityCategory.PLEASURE }
    val achievementCount = items.count { it.activity.category == ActivityCategory.ACHIEVEMENT }
    val bothCount = items.count { it.activity.category == ActivityCategory.BOTH }
    val totalCount = items.size

    val funFraction: Float
    val achievementFraction: Float
    val bothFraction: Float
    if (totalCount == 0) {
      funFraction = 0f
      achievementFraction = 0f
      bothFraction = 0f
    } else if (countBothInFunAndAchievement) {
      val effectiveFun = pleasureCount + bothCount
      val effectiveAch = achievementCount + bothCount
      val denom = (effectiveFun + effectiveAch).toFloat()
      funFraction = if (denom > 0f) effectiveFun / denom else 0f
      achievementFraction = if (denom > 0f) effectiveAch / denom else 0f
      bothFraction = 0f
    } else {
      val denom = totalCount.toFloat()
      funFraction = pleasureCount / denom
      achievementFraction = achievementCount / denom
      bothFraction = bothCount / denom
    }

    val guidanceMessage =
      when {
        totalCount == 0 ->
          "Complete an activity to start tracking your Fun & Achievement balance."
        totalCount < 3 ->
          "Complete a few more activities to see your Fun & Achievement balance take shape."
        else -> {
          val effectiveFun = pleasureCount + if (countBothInFunAndAchievement) bothCount else 0
          val effectiveAch = achievementCount + if (countBothInFunAndAchievement) bothCount else 0
          val sum = effectiveFun + effectiveAch
          if (sum == 0) {
            "Your activities have all been Both / Mixed — blending enjoyment and accomplishment."
          } else {
            val funRatio = effectiveFun.toFloat() / sum.toFloat()
            when {
              funRatio <= 0.30f ->
                "Mostly achievement lately — try prescribing a brief activity purely for enjoyment."
              funRatio >= 0.70f ->
                "Mostly pleasure lately — adding one tiny 5-minute task can also build a sense of mastery."
              else ->
                "A balanced mix of fun and achievement — keeping both in the mix supports steadier mood."
            }
          }
        }
      }

    return FunAchievementBalance(
      pleasureCount = pleasureCount,
      achievementCount = achievementCount,
      bothCount = bothCount,
      totalCount = totalCount,
      countBothInFunAndAchievement = countBothInFunAndAchievement,
      funFraction = funFraction,
      achievementFraction = achievementFraction,
      bothFraction = bothFraction,
      guidanceMessage = guidanceMessage,
    )
  }

  private fun computeTopMoodLifters(
    paired: List<Pair<ActivityWithLog, DeltaSummary>>,
  ): List<MoodLifterStat> {
    if (paired.isEmpty()) return emptyList()
    val sortedRecent = paired.sortedByDescending { effectiveTimestampMs(it.first) }
    val grouped = sortedRecent.groupBy { it.first.activity.title.trim().lowercase(Locale.ROOT) }

    return grouped.values
      .mapNotNull { entries ->
        val firstActivity = entries.first().first.activity
        val title = firstActivity.title.trim()
        if (title.isEmpty()) return@mapNotNull null
        val avgDep = entries.map { it.second.depressionDelta }.average()
        val avgPle = entries.map { it.second.pleasureDelta }.average()
        val avgAch = entries.map { it.second.achievementDelta }.average()
        if (avgDep >= 0.0 && avgPle <= 0.0 && avgAch <= 0.0) return@mapNotNull null
        MoodLifterStat(
          title = title,
          category = firstActivity.category,
          completionCount = entries.size,
          avgDepressionDelta = avgDep,
          avgPleasureDelta = avgPle,
          avgAchievementDelta = avgAch,
        )
      }
      .sortedWith(
        compareByDescending<MoodLifterStat> {
          (-it.avgDepressionDelta) + it.avgPleasureDelta + (it.avgAchievementDelta * 0.5)
        }
          .thenBy { it.avgDepressionDelta }
          .thenByDescending { it.completionCount }
          .thenBy { it.title },
      )
      .take(3)
  }

  fun formatSignedAverage(value: Double): String {
    if (abs(value) < 0.05) return "0.0"
    return String.format(Locale.US, "%+.1f", value)
  }

  fun formatMagnitude(value: Double): String =
    String.format(Locale.US, "%.1f", abs(value))

  /**
   * Rounds non-negative fractions summing to ~1.0 into integer percentages summing to 100.
   */
  fun roundedPercentages(fractions: List<Float>): List<Int> {
    if (fractions.isEmpty()) return emptyList()
    val totalFraction = fractions.sum()
    if (totalFraction <= 0f) return List(fractions.size) { 0 }
    val raw = fractions.map { (it / totalFraction) * 100f }
    val floored = raw.map { it.toInt() }.toMutableList()
    var remainder = 100 - floored.sum()
    val order = raw.indices.sortedByDescending { raw[it] - floored[it] }
    var idx = 0
    while (remainder > 0 && order.isNotEmpty()) {
      floored[order[idx % order.size]] += 1
      remainder -= 1
      idx += 1
    }
    return floored
  }

  fun formatHeadlineCopy(overall: AverageDeltaStats): String {
    if (!overall.hasData) {
      return "Complete your first check-in to start building personal evidence of how action shifts your mood."
    }
    val dep = overall.avgDepressionDelta ?: 0.0
    val ple = overall.avgPleasureDelta ?: 0.0
    val ach = overall.avgAchievementDelta ?: 0.0

    if (dep <= -0.05) {
      return "On average, your Depression drops by ${formatMagnitude(dep)} points after completing an activity."
    }
    if (ple >= 0.05 && ach >= 0.05) {
      return "On average, your Pleasure rises by ${formatMagnitude(ple)} and Achievement by ${formatMagnitude(ach)} points after completing an activity."
    }
    if (ple >= 0.05) {
      return "On average, your Pleasure rises by ${formatMagnitude(ple)} points after completing an activity."
    }
    if (ach >= 0.05) {
      return "On average, your Achievement rises by ${formatMagnitude(ach)} points after completing an activity."
    }
    val noun = if (overall.count == 1) "activity" else "activities"
    return "You've logged ${overall.count} $noun. Showing up and checking in honestly is useful evidence."
  }
}

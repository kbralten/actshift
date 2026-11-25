package ca.bralten.actshift.domain.model

enum class InsightsTimeRange(val label: String, val days: Long?) {
  LAST_7_DAYS("7 Days", 7L),
  LAST_30_DAYS("30 Days", 30L),
  ALL_TIME("All Time", null),
}

data class AverageDeltaStats(
  val count: Int,
  val avgDepressionDelta: Double?,
  val avgPleasureDelta: Double?,
  val avgAchievementDelta: Double?,
) {
  val hasData: Boolean
    get() = count > 0
}

data class CategoryDeltaBreakdown(
  val overall: AverageDeltaStats,
  val pleasure: AverageDeltaStats,
  val achievement: AverageDeltaStats,
  val both: AverageDeltaStats,
  val countBothInFunAndAchievement: Boolean,
)

data class FunAchievementBalance(
  val pleasureCount: Int,
  val achievementCount: Int,
  val bothCount: Int,
  val totalCount: Int,
  val countBothInFunAndAchievement: Boolean,
  val funFraction: Float,
  val achievementFraction: Float,
  val bothFraction: Float,
  val guidanceMessage: String,
)

data class MoodLifterStat(
  val title: String,
  val category: ActivityCategory,
  val completionCount: Int,
  val avgDepressionDelta: Double,
  val avgPleasureDelta: Double,
  val avgAchievementDelta: Double,
)

data class InsightsSummary(
  val timeRange: InsightsTimeRange,
  val totalCompletedCount: Int,
  val deltaBreakdown: CategoryDeltaBreakdown,
  val balance: FunAchievementBalance,
  val topMoodLifters: List<MoodLifterStat>,
  val headlineCopy: String,
)

package ca.bralten.actshift.domain

import ca.bralten.actshift.domain.model.ActivationLog
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.ActivityStatus
import ca.bralten.actshift.domain.model.ActivityWithLog
import ca.bralten.actshift.domain.model.AverageDeltaStats
import ca.bralten.actshift.domain.model.InsightsTimeRange
import ca.bralten.actshift.domain.model.ScheduledActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class InsightsCalculatorTest {

  private val utc: ZoneId = ZoneId.of("UTC")

  @Test
  fun categoryBreakdownRespectsCountBothInFunAndAchievementToggle() {
    val logs = listOf(
      completedItem(1L, "Sunlit walk", ActivityCategory.PLEASURE, 1_000L, pre = Triple(5, 1, 1), post = Triple(3, 5, 1)), // dep -2, ple +4, ach 0
      completedItem(2L, "Get the mail", ActivityCategory.ACHIEVEMENT, 2_000L, pre = Triple(6, 0, 0), post = Triple(4, 0, 2)), // dep -2, ple 0, ach +2
      completedItem(3L, "Walk the dog", ActivityCategory.BOTH, 3_000L, pre = Triple(7, 2, 1), post = Triple(2, 4, 4)), // dep -5, ple +2, ach +3
    )

    // Toggle ON: Both counts in Fun, Achievement, and Both
    val included = InsightsCalculator.compute(
      items = logs,
      range = InsightsTimeRange.ALL_TIME,
      countBothInFunAndAchievement = true,
      nowMs = 10_000L,
      zoneId = utc,
    )
    val bOn = included.deltaBreakdown
    assertEquals(3, bOn.overall.count)
    assertEquals(-3.0, bOn.overall.avgDepressionDelta!!, 0.001)
    assertEquals(2.0, bOn.overall.avgPleasureDelta!!, 0.001)
    assertEquals(5.0 / 3.0, bOn.overall.avgAchievementDelta!!, 0.001)

    assertEquals(2, bOn.pleasure.count)
    assertEquals(-3.5, bOn.pleasure.avgDepressionDelta!!, 0.001)
    assertEquals(3.0, bOn.pleasure.avgPleasureDelta!!, 0.001)
    assertEquals(1.5, bOn.pleasure.avgAchievementDelta!!, 0.001)

    assertEquals(2, bOn.achievement.count)
    assertEquals(-3.5, bOn.achievement.avgDepressionDelta!!, 0.001)
    assertEquals(1.0, bOn.achievement.avgPleasureDelta!!, 0.001)
    assertEquals(2.5, bOn.achievement.avgAchievementDelta!!, 0.001)

    assertEquals(1, bOn.both.count)
    assertEquals(-5.0, bOn.both.avgDepressionDelta!!, 0.001)

    // Toggle OFF: Both counts ONLY in Both
    val separate = InsightsCalculator.compute(
      items = logs,
      range = InsightsTimeRange.ALL_TIME,
      countBothInFunAndAchievement = false,
      nowMs = 10_000L,
      zoneId = utc,
    )
    val bOff = separate.deltaBreakdown
    assertEquals(3, bOff.overall.count)
    assertEquals(1, bOff.pleasure.count)
    assertEquals(-2.0, bOff.pleasure.avgDepressionDelta!!, 0.001)
    assertEquals(4.0, bOff.pleasure.avgPleasureDelta!!, 0.001)
    assertEquals(0.0, bOff.pleasure.avgAchievementDelta!!, 0.001)

    assertEquals(1, bOff.achievement.count)
    assertEquals(-2.0, bOff.achievement.avgDepressionDelta!!, 0.001)
    assertEquals(0.0, bOff.achievement.avgPleasureDelta!!, 0.001)
    assertEquals(2.0, bOff.achievement.avgAchievementDelta!!, 0.001)

    assertEquals(1, bOff.both.count)
    assertEquals(-5.0, bOff.both.avgDepressionDelta!!, 0.001)
  }

  @Test
  fun emptyCategoriesReportNullAverages() {
    val logs = listOf(
      completedItem(1L, "Music", ActivityCategory.PLEASURE, 1_000L, pre = Triple(4, 2, 0), post = Triple(2, 4, 0)),
    )
    val summary = InsightsCalculator.compute(logs, InsightsTimeRange.ALL_TIME, countBothInFunAndAchievement = false, nowMs = 2_000L, zoneId = utc)
    assertEquals(0, summary.deltaBreakdown.achievement.count)
    assertNull(summary.deltaBreakdown.achievement.avgDepressionDelta)
    assertEquals(0, summary.deltaBreakdown.both.count)
    assertNull(summary.deltaBreakdown.both.avgPleasureDelta)
  }

  @Test
  fun balanceFractionsAndGuidanceReflectModeAndSkew() {
    val empty = InsightsCalculator.compute(emptyList(), InsightsTimeRange.ALL_TIME, true, 1_000L, utc)
    assertTrue(empty.balance.guidanceMessage.contains("Complete an activity"))

    val twoItems = listOf(
      completedItem(1L, "A", ActivityCategory.PLEASURE, 1_000L, Triple(4, 1, 1), Triple(3, 2, 1)),
      completedItem(2L, "B", ActivityCategory.ACHIEVEMENT, 2_000L, Triple(4, 1, 1), Triple(3, 1, 2)),
    )
    val small = InsightsCalculator.compute(twoItems, InsightsTimeRange.ALL_TIME, true, 3_000L, utc)
    assertTrue(small.balance.guidanceMessage.contains("few more"))

    val achHeavy = listOf(
      completedItem(1L, "A", ActivityCategory.ACHIEVEMENT, 1_000L, Triple(5, 1, 1), Triple(4, 1, 2)),
      completedItem(2L, "B", ActivityCategory.ACHIEVEMENT, 2_000L, Triple(5, 1, 1), Triple(4, 1, 2)),
      completedItem(3L, "C", ActivityCategory.ACHIEVEMENT, 3_000L, Triple(5, 1, 1), Triple(4, 1, 2)),
      completedItem(4L, "D", ActivityCategory.PLEASURE, 4_000L, Triple(5, 1, 1), Triple(4, 3, 1)),
    )
    val achSummary = InsightsCalculator.compute(achHeavy, InsightsTimeRange.ALL_TIME, false, 5_000L, utc)
    assertEquals(0.25f, achSummary.balance.funFraction, 0.001f)
    assertEquals(0.75f, achSummary.balance.achievementFraction, 0.001f)
    assertTrue(achSummary.balance.guidanceMessage.contains("Mostly achievement"))

    val funHeavy = listOf(
      completedItem(1L, "A", ActivityCategory.PLEASURE, 1_000L, Triple(5, 1, 1), Triple(4, 3, 1)),
      completedItem(2L, "B", ActivityCategory.PLEASURE, 2_000L, Triple(5, 1, 1), Triple(4, 3, 1)),
      completedItem(3L, "C", ActivityCategory.PLEASURE, 3_000L, Triple(5, 1, 1), Triple(4, 3, 1)),
      completedItem(4L, "D", ActivityCategory.ACHIEVEMENT, 4_000L, Triple(5, 1, 1), Triple(4, 1, 3)),
    )
    val funSummary = InsightsCalculator.compute(funHeavy, InsightsTimeRange.ALL_TIME, false, 5_000L, utc)
    assertTrue(funSummary.balance.guidanceMessage.contains("Mostly pleasure"))

    val allBoth = listOf(
      completedItem(1L, "A", ActivityCategory.BOTH, 1_000L, Triple(5, 1, 1), Triple(3, 3, 3)),
      completedItem(2L, "B", ActivityCategory.BOTH, 2_000L, Triple(5, 1, 1), Triple(3, 3, 3)),
      completedItem(3L, "C", ActivityCategory.BOTH, 3_000L, Triple(5, 1, 1), Triple(3, 3, 3)),
    )
    val allBothOff = InsightsCalculator.compute(allBoth, InsightsTimeRange.ALL_TIME, false, 5_000L, utc)
    assertEquals(1.0f, allBothOff.balance.bothFraction, 0.001f)
    assertTrue(allBothOff.balance.guidanceMessage.contains("Both / Mixed"))

    val allBothOn = InsightsCalculator.compute(allBoth, InsightsTimeRange.ALL_TIME, true, 5_000L, utc)
    assertEquals(0.5f, allBothOn.balance.funFraction, 0.001f)
    assertEquals(0.5f, allBothOn.balance.achievementFraction, 0.001f)
    assertTrue(allBothOn.balance.guidanceMessage.contains("balanced mix"))
  }

  @Test
  fun timeRangeFiltersDeterministicallyInUtc() {
    val now = ZonedDateTime.of(2026, 11, 25, 15, 0, 0, 0, utc).toInstant().toEpochMilli()
    val twoDaysAgo = ZonedDateTime.of(2026, 11, 23, 10, 0, 0, 0, utc).toInstant().toEpochMilli()
    val tenDaysAgo = ZonedDateTime.of(2026, 11, 15, 10, 0, 0, 0, utc).toInstant().toEpochMilli()
    val fortyDaysAgo = ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, utc).toInstant().toEpochMilli()

    val logs = listOf(
      completedItem(1L, "Recent", ActivityCategory.PLEASURE, twoDaysAgo, Triple(5, 1, 1), Triple(3, 3, 1)),
      completedItem(2L, "Mid", ActivityCategory.ACHIEVEMENT, tenDaysAgo, Triple(5, 1, 1), Triple(4, 1, 3)),
      completedItem(3L, "Old", ActivityCategory.BOTH, fortyDaysAgo, Triple(5, 1, 1), Triple(2, 4, 4)),
    )

    assertEquals(1, InsightsCalculator.filterByTimeRange(logs, InsightsTimeRange.LAST_7_DAYS, now, utc).size)
    assertEquals(2, InsightsCalculator.filterByTimeRange(logs, InsightsTimeRange.LAST_30_DAYS, now, utc).size)
    assertEquals(3, InsightsCalculator.filterByTimeRange(logs, InsightsTimeRange.ALL_TIME, now, utc).size)
  }

  @Test
  fun topMoodLiftersGroupsNormalizesTitlesAndFiltersNonPositive() {
    val logs = listOf(
      completedItem(1L, "Walk outside", ActivityCategory.PLEASURE, 1_000L, Triple(6, 1, 1), Triple(3, 4, 2)), // lift 3 + 3 + 0.5 = 6.5
      completedItem(2L, "walk outside ", ActivityCategory.PLEASURE, 2_000L, Triple(5, 2, 1), Triple(3, 4, 1)), // avg: dep -2.5, ple +2.5, ach +0.5 -> 5.25
      completedItem(3L, "Worsened task", ActivityCategory.ACHIEVEMENT, 3_000L, Triple(3, 2, 2), Triple(4, 2, 2)), // dep +1 -> excluded
      completedItem(4L, "Coffee on porch", ActivityCategory.PLEASURE, 4_000L, Triple(6, 1, 0), Triple(2, 5, 0)), // dep -4, ple +4 -> 8.0
      completedItem(5L, "Tiny tidy", ActivityCategory.ACHIEVEMENT, 5_000L, Triple(5, 0, 1), Triple(4, 0, 3)), // dep -1, ach +2 -> 2.0
      completedItem(6L, "Call friend", ActivityCategory.BOTH, 6_000L, Triple(6, 1, 1), Triple(3, 4, 3)), // dep -3, ple +3, ach +2 -> 7.0
    )

    val summary = InsightsCalculator.compute(logs, InsightsTimeRange.ALL_TIME, true, 10_000L, utc)
    val lifters = summary.topMoodLifters
    assertEquals(3, lifters.size)
    assertEquals("Coffee on porch", lifters[0].title)
    assertEquals("Call friend", lifters[1].title)
    assertEquals("walk outside", lifters[2].title)
    assertEquals(2, lifters[2].completionCount)
  }

  @Test
  fun formattingHelpersPreventNegativeZeroAndGenerateGroundedHeadlines() {
    assertEquals("0.0", InsightsCalculator.formatSignedAverage(-0.01))
    assertEquals("0.0", InsightsCalculator.formatSignedAverage(0.04))
    assertEquals("-2.1", InsightsCalculator.formatSignedAverage(-2.14))
    assertEquals("+1.5", InsightsCalculator.formatSignedAverage(1.5))

    val pcts = InsightsCalculator.roundedPercentages(listOf(1f / 3f, 1f / 3f, 1f / 3f))
    assertEquals(100, pcts.sum())

    val depDrop = InsightsCalculator.formatHeadlineCopy(AverageDeltaStats(3, -2.1, 1.0, 0.5))
    assertTrue(depDrop.contains("drops by 2.1 points"))

    val pleasureRise = InsightsCalculator.formatHeadlineCopy(AverageDeltaStats(2, 0.0, 1.5, 0.0))
    assertTrue(pleasureRise.contains("Pleasure rises by 1.5"))

    val flat = InsightsCalculator.formatHeadlineCopy(AverageDeltaStats(1, 0.0, 0.0, 0.0))
    assertTrue(flat.contains("1 activity"))
  }

  private fun completedItem(
    id: Long,
    title: String,
    category: ActivityCategory,
    completedAtMs: Long,
    pre: Triple<Int, Int, Int>,
    post: Triple<Int, Int, Int>,
  ): ActivityWithLog =
    ActivityWithLog(
      activity = ScheduledActivity(
        id = id,
        templateId = null,
        title = title,
        category = category,
        scheduledStartEpochMs = completedAtMs - 600_000L,
        durationMinutes = 10,
        status = ActivityStatus.COMPLETED,
        calendarEventId = null,
        calendarInstanceBeginMs = null,
        startedAtEpochMs = completedAtMs - 600_000L,
        timerEndEpochMs = null,
        completedAtEpochMs = completedAtMs,
        snoozeCount = 0,
      ),
      log = ActivationLog(
        id = id,
        activityId = id,
        preDepression = pre.first,
        prePleasure = pre.second,
        preAchievement = pre.third,
        preLoggedAtEpochMs = completedAtMs - 600_000L,
        postDepression = post.first,
        postPleasure = post.second,
        postAchievement = post.third,
        postLoggedAtEpochMs = completedAtMs,
        reflectionNote = null,
      ),
    )
}

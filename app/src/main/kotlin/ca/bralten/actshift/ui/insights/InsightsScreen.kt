package ca.bralten.actshift.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.bralten.actshift.data.export.TherapistExportManager
import ca.bralten.actshift.domain.InsightsCalculator
import ca.bralten.actshift.domain.model.AverageDeltaStats
import ca.bralten.actshift.domain.model.FunAchievementBalance
import ca.bralten.actshift.domain.model.InsightsSummary
import ca.bralten.actshift.domain.model.InsightsTimeRange
import ca.bralten.actshift.domain.model.MoodLifterStat
import ca.bralten.actshift.ui.components.CategoryBadge
import ca.bralten.actshift.ui.components.TherapistExportSheet
import ca.bralten.actshift.ui.theme.LocalActShiftColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
  viewModel: InsightsViewModel,
  exportManager: TherapistExportManager,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val summary = uiState.summary
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var showExportSheet by remember { mutableStateOf(false) }

  if (showExportSheet) {
    TherapistExportSheet(
      allCompletedLogs = uiState.allCompletedLogs,
      initialRange = uiState.selectedRange,
      countBothInFunAndAchievement = uiState.countBothInFunAndAchievement,
      exportManager = exportManager,
      onDismiss = { showExportSheet = false },
      onMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
    )
  }

  Scaffold(
    modifier = modifier,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text("Insights") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
        actions = {
          if (uiState.allCompletedLogs.isNotEmpty()) {
            IconButton(onClick = { showExportSheet = true }) {
              Icon(Icons.Default.Share, contentDescription = "Export for therapist")
            }
          }
        },
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          InsightsTimeRange.entries.forEachIndexed { index, range ->
            SegmentedButton(
              selected = uiState.selectedRange == range,
              onClick = { viewModel.selectRange(range) },
              shape = SegmentedButtonDefaults.itemShape(index, InsightsTimeRange.entries.size),
              label = { Text(range.label) },
            )
          }
        }
      }

      if (summary.totalCompletedCount == 0) {
        item {
          EmptyInsightsCard(
            hasLogsOverall = uiState.allCompletedLogs.isNotEmpty(),
            rangeLabel = uiState.selectedRange.label,
            onShowAllTime = { viewModel.selectRange(InsightsTimeRange.ALL_TIME) },
          )
        }
      } else {
        item {
          MoodShiftAveragesCard(
            summary = summary,
            onOpenSettings = onOpenSettings,
          )
        }

        item {
          FunAchievementBalanceCard(balance = summary.balance)
        }

        if (summary.topMoodLifters.isNotEmpty()) {
          item {
            TopMoodLiftersCard(lifters = summary.topMoodLifters)
          }
        }

        item {
          TherapistExportCard(onExportClick = { showExportSheet = true })
        }
      }
    }
  }
}

@Composable
private fun EmptyInsightsCard(
  hasLogsOverall: Boolean,
  rangeLabel: String,
  onShowAllTime: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = if (hasLogsOverall) "No check-ins in $rangeLabel" else "Insights grow with each check-in",
        style = MaterialTheme.typography.titleLarge,
      )
      Text(
        text =
          if (hasLogsOverall) {
            "Switch to All Time to review your mood shift averages and Fun vs. Achievement balance."
          } else {
            "Complete a scheduled activity and log how you feel afterward. ActShift will show your average mood shifts and Fun vs. Achievement balance here."
          },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (hasLogsOverall) {
        OutlinedButton(onClick = onShowAllTime) {
          Text("Show All Time")
        }
      }
    }
  }
}

@Composable
private fun MoodShiftAveragesCard(
  summary: InsightsSummary,
  onOpenSettings: () -> Unit,
) {
  val breakdown = summary.deltaBreakdown

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Average Mood Shifts",
        style = MaterialTheme.typography.titleLarge,
      )

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = summary.headlineCopy,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.padding(14.dp),
        )
      }

      AverageDeltaRow(
        label = "Overall",
        stats = breakdown.overall,
        emphasize = true,
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Text(
        text = "By Category",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      AverageDeltaRow(label = "Pleasure / Fun", stats = breakdown.pleasure)
      AverageDeltaRow(label = "Achievement", stats = breakdown.achievement)
      AverageDeltaRow(label = "Both / Mixed", stats = breakdown.both)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text =
            if (breakdown.countBothInFunAndAchievement) {
              "Both / Mixed activities count in Fun, Achievement, and Both."
            } else {
              "Both / Mixed activities count in the Both average only."
            },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpenSettings) {
          Text("Change")
        }
      }
    }
  }
}

@Composable
private fun AverageDeltaRow(
  label: String,
  stats: AverageDeltaStats,
  emphasize: Boolean = false,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
        fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Medium,
      )
      val countText =
        when (stats.count) {
          0 -> "0 logs"
          1 -> "1 log"
          else -> "${stats.count} logs"
        }
      Text(
        text = countText,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      DeltaStatChip(
        label = "Dep",
        value = stats.avgDepressionDelta,
        lowerIsPositive = true,
        modifier = Modifier.weight(1f),
      )
      DeltaStatChip(
        label = "Pleasure",
        value = stats.avgPleasureDelta,
        lowerIsPositive = false,
        modifier = Modifier.weight(1f),
      )
      DeltaStatChip(
        label = "Achiev.",
        value = stats.avgAchievementDelta,
        lowerIsPositive = false,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun DeltaStatChip(
  label: String,
  value: Double?,
  lowerIsPositive: Boolean,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalActShiftColors.current
  val isBeneficial =
    value != null && if (lowerIsPositive) value <= -0.05 else value >= 0.05
  val container =
    if (isBeneficial) semantic.positiveShiftContainer else semantic.neutralShiftContainer
  val content =
    if (isBeneficial) semantic.onPositiveShiftContainer else semantic.onNeutralShiftContainer

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = container,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = content.copy(alpha = 0.85f),
      )
      Text(
        text = value?.let(InsightsCalculator::formatSignedAverage) ?: "—",
        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
        fontWeight = FontWeight.SemiBold,
        color = content,
      )
    }
  }
}

@Composable
private fun FunAchievementBalanceCard(balance: FunAchievementBalance) {
  val funColor = MaterialTheme.colorScheme.tertiary
  val bothColor = MaterialTheme.colorScheme.primary
  val achievementColor = MaterialTheme.colorScheme.secondary

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Fun vs. Achievement Balance",
        style = MaterialTheme.typography.titleLarge,
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(16.dp)
          .clip(RoundedCornerShape(999.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
      ) {
        if (balance.funFraction > 0f) {
          Box(
            modifier = Modifier
              .weight(balance.funFraction)
              .fillMaxHeight()
              .background(funColor),
          )
        }
        if (!balance.countBothInFunAndAchievement && balance.bothFraction > 0f) {
          Box(
            modifier = Modifier
              .weight(balance.bothFraction)
              .fillMaxHeight()
              .background(bothColor),
          )
        }
        if (balance.achievementFraction > 0f) {
          Box(
            modifier = Modifier
              .weight(balance.achievementFraction)
              .fillMaxHeight()
              .background(achievementColor),
          )
        }
      }

      if (balance.countBothInFunAndAchievement) {
        val pcts =
          InsightsCalculator.roundedPercentages(
            listOf(balance.funFraction, balance.achievementFraction),
          )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          BalanceLegendItem(
            color = funColor,
            label = "Fun ${pcts[0]}% (${balance.pleasureCount + balance.bothCount})",
          )
          BalanceLegendItem(
            color = achievementColor,
            label = "Achievement ${pcts[1]}% (${balance.achievementCount + balance.bothCount})",
          )
        }
        if (balance.bothCount > 0) {
          val noun = if (balance.bothCount == 1) "activity" else "activities"
          Text(
            text = "${balance.bothCount} Both / Mixed $noun counted toward both Fun and Achievement.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        val pcts =
          InsightsCalculator.roundedPercentages(
            listOf(balance.funFraction, balance.bothFraction, balance.achievementFraction),
          )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          BalanceLegendItem(
            color = funColor,
            label = "Fun ${pcts[0]}% (${balance.pleasureCount})",
          )
          BalanceLegendItem(
            color = bothColor,
            label = "Both ${pcts[1]}% (${balance.bothCount})",
          )
          BalanceLegendItem(
            color = achievementColor,
            label = "Achiev. ${pcts[2]}% (${balance.achievementCount})",
          )
        }
      }

      Text(
        text = balance.guidanceMessage,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun BalanceLegendItem(
  color: Color,
  label: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(
      modifier = Modifier
        .size(10.dp)
        .clip(CircleShape)
        .background(color),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopMoodLiftersCard(lifters: List<MoodLifterStat>) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Top Mood-Lifters",
        style = MaterialTheme.typography.titleLarge,
      )

      lifters.forEachIndexed { index, lifter ->
        if (index > 0) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                if (lifter.completionCount > 1) {
                  "${lifter.title} (×${lifter.completionCount})"
                } else {
                  lifter.title
                },
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.weight(1f),
            )
            CategoryBadge(category = lifter.category)
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            MiniLiftBadge(
              label = "Dep ${InsightsCalculator.formatSignedAverage(lifter.avgDepressionDelta)}",
              beneficial = lifter.avgDepressionDelta <= -0.05,
            )
            MiniLiftBadge(
              label = "Pleasure ${InsightsCalculator.formatSignedAverage(lifter.avgPleasureDelta)}",
              beneficial = lifter.avgPleasureDelta >= 0.05,
            )
            MiniLiftBadge(
              label = "Achiev. ${InsightsCalculator.formatSignedAverage(lifter.avgAchievementDelta)}",
              beneficial = lifter.avgAchievementDelta >= 0.05,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MiniLiftBadge(
  label: String,
  beneficial: Boolean,
) {
  val semantic = LocalActShiftColors.current
  Surface(
    shape = RoundedCornerShape(999.dp),
    color = if (beneficial) semantic.positiveShiftContainer else semantic.neutralShiftContainer,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (beneficial) semantic.onPositiveShiftContainer else semantic.onNeutralShiftContainer,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
private fun TherapistExportCard(onExportClick: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Share with Your Therapist",
        style = MaterialTheme.typography.titleLarge,
      )
      Text(
        text = "Export a CCI-style PDF worksheet or raw CSV spreadsheet of your completed activities and reflections.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(
        onClick = onExportClick,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Export PDF / CSV")
      }
    }
  }
}

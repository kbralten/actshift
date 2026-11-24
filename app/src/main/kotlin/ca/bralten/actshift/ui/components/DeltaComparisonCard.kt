package ca.bralten.actshift.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ca.bralten.actshift.domain.model.ActivationLog
import ca.bralten.actshift.ui.theme.LocalActShiftColors

/**
 * Worksheet-style Before vs. After comparison block showing directional deltas.
 */
@Composable
fun DeltaComparisonCard(
  log: ActivationLog,
  modifier: Modifier = Modifier,
) {
  val postDep = log.postDepression ?: return
  val postPle = log.postPleasure ?: return
  val postAch = log.postAchievement ?: return

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Indicator",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1.2f),
        )
        Text(
          text = "Before → After",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Start,
          modifier = Modifier.weight(1.1f),
        )
        Box(
          modifier = Modifier.width(68.dp),
          contentAlignment = Alignment.CenterEnd,
        ) {
          Text(
            text = "Shift",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      DeltaIndicatorRow(
        label = "Depression",
        before = log.preDepression,
        after = postDep,
        lowerIsPositive = true,
      )
      DeltaIndicatorRow(
        label = "Pleasure",
        before = log.prePleasure,
        after = postPle,
        lowerIsPositive = false,
      )
      DeltaIndicatorRow(
        label = "Achievement",
        before = log.preAchievement,
        after = postAch,
        lowerIsPositive = false,
      )
    }
  }
}

@Composable
private fun DeltaIndicatorRow(
  label: String,
  before: Int,
  after: Int,
  lowerIsPositive: Boolean,
) {
  val delta = after - before
  val isBeneficial = if (lowerIsPositive) delta < 0 else delta > 0
  val semantic = LocalActShiftColors.current
  val deltaBadgeContainer =
    if (isBeneficial) semantic.positiveShiftContainer else semantic.neutralShiftContainer
  val deltaBadgeContent =
    if (isBeneficial) semantic.onPositiveShiftContainer else semantic.onNeutralShiftContainer

  val signedDelta =
    when {
      delta > 0 -> "+$delta"
      else -> "$delta"
    }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1.2f),
    )
    Text(
      text = "$before → $after",
      style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Start,
      modifier = Modifier.weight(1.1f),
    )
    Box(
      modifier = Modifier.width(68.dp),
      contentAlignment = Alignment.CenterEnd,
    ) {
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = deltaBadgeContainer,
      ) {
        Text(
          text = "($signedDelta)",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = deltaBadgeContent,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }
  }
}

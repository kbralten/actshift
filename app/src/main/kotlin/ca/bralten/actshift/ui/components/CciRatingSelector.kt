package ca.bralten.actshift.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.bralten.actshift.domain.model.CciScaleLevel

/**
 * Thumb-friendly 0–8 CCI scale selector supporting both single-tap and horizontal drag
 * with haptic tick feedback and exact official CCI anchor labels.
 */
@Composable
fun CciRatingSelector(
  title: String,
  subtitle: String,
  selectedScore: Int?,
  onScoreSelected: (Int) -> Unit,
  modifier: Modifier = Modifier,
  testTagPrefix: String = "cci_selector",
) {
  val haptic = LocalHapticFeedback.current
  val currentSelected by rememberUpdatedState(selectedScore)
  var rowWidthPx by remember { mutableIntStateOf(0) }

  val selectedLevel = selectedScore?.let { CciScaleLevel.fromScore(it) }

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (selectedLevel != null) {
            MaterialTheme.colorScheme.primaryContainer
          } else {
            MaterialTheme.colorScheme.surfaceVariant
          },
          modifier = Modifier.testTag("${testTagPrefix}_badge"),
        ) {
          Text(
            text = if (selectedLevel != null) {
              "${selectedLevel.score} · ${selectedLevel.label}"
            } else {
              "Tap 0–8"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            color = if (selectedLevel != null) {
              MaterialTheme.colorScheme.onPrimaryContainer
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          )
        }
      }

      if (subtitle.isNotBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          modifier = Modifier.padding(top = 2.dp),
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .onSizeChanged { rowWidthPx = it.width }
          .pointerInput(Unit) {
            detectHorizontalDragGestures { change, _ ->
              if (rowWidthPx <= 0) return@detectHorizontalDragGestures
              val ratio = (change.position.x / rowWidthPx.toFloat()).coerceIn(0f, 0.9999f)
              val nextScore = (ratio * 9f).toInt().coerceIn(0, 8)
              if (nextScore != currentSelected) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onScoreSelected(nextScore)
              }
            }
          },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        for (score in 0..8) {
          val isSelected = selectedScore == score
          val level = CciScaleLevel.fromScore(score)
          val shape = RoundedCornerShape(10.dp)
          Box(
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .clip(shape)
              .background(
                if (isSelected) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                },
              )
              .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.outlineVariant
                },
                shape = shape,
              )
              .semantics {
                selected = isSelected
                contentDescription = "$title score $score ${level.label}"
              }
              .clickable(role = Role.RadioButton) {
                if (score != currentSelected) {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onScoreSelected(score)
              }
              .testTag("${testTagPrefix}_$score"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = score.toString(),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
              } else {
                MaterialTheme.colorScheme.onSurface
              },
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        listOf("0 None", "2 Slight", "4 Moderate", "6 Higher", "8 Extreme").forEach { anchor ->
          Text(
            text = anchor,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
        }
      }
    }
  }
}

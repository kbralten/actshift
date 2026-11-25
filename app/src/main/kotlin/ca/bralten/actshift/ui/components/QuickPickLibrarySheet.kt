package ca.bralten.actshift.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.bralten.actshift.domain.ActivitySuggestion
import ca.bralten.actshift.domain.ActivitySuggestionLibrary
import ca.bralten.actshift.domain.EnergyLevel
import ca.bralten.actshift.domain.model.ActivityCategory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickPickLibrarySheet(
  onDismiss: () -> Unit,
  onSelectSuggestion: (ActivitySuggestion) -> Unit,
  onStartNow: ((ActivitySuggestion) -> Unit)? = null,
) {
  val session = remember { ActivitySuggestionLibrary.createRandomizedSession() }
  var selectedEnergy by remember { mutableStateOf(session.initialEnergyLevel) }
  var selectedCategory by remember { mutableStateOf(session.initialCategory) }
  var highlightedSuggestion by remember { mutableStateOf<ActivitySuggestion?>(null) }

  val filtered =
    remember(session, selectedEnergy, selectedCategory) {
      ActivitySuggestionLibrary.filter(
        energyLevel = selectedEnergy,
        category = selectedCategory,
        source = session.shuffledSuggestions,
      )
    }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Quick-Pick Ideas",
          style = MaterialTheme.typography.headlineSmall,
        )
        Text(
          text = "Low-barrier 5–15 minute activities matched to your current energy.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          EnergyLevel.entries.forEachIndexed { index, level ->
            SegmentedButton(
              selected = selectedEnergy == level,
              onClick = {
                selectedEnergy = level
                highlightedSuggestion = null
              },
              shape = SegmentedButtonDefaults.itemShape(index, EnergyLevel.entries.size),
              label = { Text(level.label, maxLines = 1) },
            )
          }
        }
        Text(
          text = selectedEnergy.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        FilterChip(
          selected = selectedCategory == null,
          onClick = {
            selectedCategory = null
            highlightedSuggestion = null
          },
          label = { Text("All") },
        )
        ActivityCategory.entries.forEach { category ->
          val label =
            when (category) {
              ActivityCategory.PLEASURE -> "Fun"
              ActivityCategory.ACHIEVEMENT -> "Achievement"
              ActivityCategory.BOTH -> "Both"
            }
          FilterChip(
            selected = selectedCategory == category,
            onClick = {
              selectedCategory = category
              highlightedSuggestion = null
            },
            label = { Text(label) },
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val noun = if (filtered.size == 1) "idea" else "ideas"
        Text(
          text = "${filtered.size} $noun",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
          onClick = {
            highlightedSuggestion =
              ActivitySuggestionLibrary.pickRandom(
                energyLevel = selectedEnergy,
                category = selectedCategory,
                excludeId = highlightedSuggestion?.id,
              )
          },
        ) {
          Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = null,
            modifier = Modifier.padding(end = 6.dp),
          )
          Text("Pick one for me")
        }
      }

      highlightedSuggestion?.let { picked ->
        SuggestionCard(
          suggestion = picked,
          highlighted = true,
          onSelectSuggestion = onSelectSuggestion,
          onStartNow = onStartNow,
        )
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(filtered, key = { it.id }) { suggestion ->
          SuggestionCard(
            suggestion = suggestion,
            highlighted = false,
            onSelectSuggestion = onSelectSuggestion,
            onStartNow = onStartNow,
          )
        }
      }
    }
  }
}

@Composable
private fun SuggestionCard(
  suggestion: ActivitySuggestion,
  highlighted: Boolean,
  onSelectSuggestion: (ActivitySuggestion) -> Unit,
  onStartNow: ((ActivitySuggestion) -> Unit)?,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color =
      if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer
      } else {
        MaterialTheme.colorScheme.surface
      },
    tonalElevation = if (highlighted) 2.dp else 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (highlighted) {
        Text(
          text = "Suggested for right now",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
      Text(
        text = suggestion.title,
        style = MaterialTheme.typography.titleMedium,
        color =
          if (highlighted) {
            MaterialTheme.colorScheme.onPrimaryContainer
          } else {
            MaterialTheme.colorScheme.onSurface
          },
      )
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Text(
            text = "${suggestion.durationMinutes}m",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
        CategoryBadge(category = suggestion.category)
      }

      if (onStartNow != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = { onStartNow(suggestion) },
            modifier = Modifier.weight(1f),
          ) {
            Text("Start Now")
          }
          OutlinedButton(
            onClick = { onSelectSuggestion(suggestion) },
            modifier = Modifier.weight(1f),
          ) {
            Text("Schedule")
          }
        }
      } else {
        OutlinedButton(
          onClick = { onSelectSuggestion(suggestion) },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Use Idea")
        }
      }
    }
  }
}

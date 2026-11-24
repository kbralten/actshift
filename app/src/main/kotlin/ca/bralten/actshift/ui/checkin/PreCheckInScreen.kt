package ca.bralten.actshift.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.ui.components.CciRatingSelector

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PreCheckInScreen(
  viewModel: PreCheckInViewModel,
  onNavigateBack: () -> Unit,
  onStartWithTimer: () -> Unit,
  onStartWithoutTimer: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Before You Start") },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
      )
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp,
      ) {
        Button(
          onClick = {
            viewModel.submit { timerStarted ->
              if (timerStarted) onStartWithTimer() else onStartWithoutTimer()
            }
          },
          enabled = uiState.canSubmit,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("begin_activity_button"),
        ) {
          Text("Begin Activity")
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = uiState.activityTitle.ifEmpty { "Activity Check-In" },
        style = MaterialTheme.typography.headlineSmall,
      )
      Text(
        text = "Rate how you feel right now (${uiState.durationMinutes} min planned).",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActivityCategory.entries.forEach { category ->
          FilterChip(
            selected = uiState.category == category,
            onClick = { viewModel.selectCategory(category) },
            label = { Text(category.label) },
          )
        }
      }

      CciRatingSelector(
        title = "Depression / Low Mood",
        subtitle = "0 = Absolutely None · 8 = Extreme",
        selectedScore = uiState.depression,
        onScoreSelected = viewModel::setDepression,
        testTagPrefix = "pre_depression",
      )

      CciRatingSelector(
        title = "Pleasant Feelings",
        subtitle = "0 = Absolutely None · 8 = Extreme",
        selectedScore = uiState.pleasure,
        onScoreSelected = viewModel::setPleasure,
        testTagPrefix = "pre_pleasure",
      )

      CciRatingSelector(
        title = "Sense of Achievement",
        subtitle = "0 = Absolutely None · 8 = Extreme",
        selectedScore = uiState.achievement,
        onScoreSelected = viewModel::setAchievement,
        testTagPrefix = "pre_achievement",
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Start ${uiState.durationMinutes}m countdown timer",
          style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
          checked = uiState.startTimer,
          onCheckedChange = viewModel::setStartTimer,
        )
      }
    }
  }
}

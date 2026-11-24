package ca.bralten.actshift.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.bralten.actshift.ui.components.CciRatingSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCheckInScreen(
  viewModel: PostCheckInViewModel,
  onNavigateBack: () -> Unit,
  onSaved: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("After Check-In") },
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
          onClick = { viewModel.submit(onSaved) },
          enabled = uiState.canSubmit,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("save_see_shift_button"),
        ) {
          Text("Save & See Shift")
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
        text = uiState.activityTitle.ifEmpty { "After Activity" },
        style = MaterialTheme.typography.headlineSmall,
      )
      Text(
        text = "Rate how you feel right now, without looking back at your earlier numbers.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      CciRatingSelector(
        title = "Depression / Low Mood",
        subtitle = "0 = Absolutely None · 8 = Extreme",
        selectedScore = uiState.postDepression,
        onScoreSelected = viewModel::setPostDepression,
        testTagPrefix = "post_depression",
      )

      CciRatingSelector(
        title = "Pleasant Feelings",
        subtitle = "0 = Absolutely None · 8 = Extreme",
        selectedScore = uiState.postPleasure,
        onScoreSelected = viewModel::setPostPleasure,
        testTagPrefix = "post_pleasure",
      )

      CciRatingSelector(
        title = "Sense of Achievement",
        subtitle = "0 = Absolutely None · 8 = Extreme",
        selectedScore = uiState.postAchievement,
        onScoreSelected = viewModel::setPostAchievement,
        testTagPrefix = "post_achievement",
      )

      OutlinedTextField(
        value = uiState.reflectionNote,
        onValueChange = viewModel::updateReflectionNote,
        label = { Text("What did you notice about yourself?") },
        placeholder = { Text("Optional one-line note") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("reflection_input"),
      )
    }
  }
}

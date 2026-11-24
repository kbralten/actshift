package ca.bralten.actshift.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.bralten.actshift.ui.components.CategoryBadge
import ca.bralten.actshift.ui.components.TimeFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTimerScreen(
  viewModel: ActiveTimerViewModel,
  onNavigateBack: () -> Unit,
  onDone: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("In Progress") },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        CategoryBadge(category = uiState.category)
        Text(
          text = uiState.activityTitle,
          style = MaterialTheme.typography.headlineMedium,
        )
        Text(
          text =
            if (uiState.isFinished) {
              "Time's up — ready whenever you are."
            } else {
              "Stay anchored for just a few minutes."
            },
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Text(
          text = TimeFormatters.formatCountdown(uiState.remainingSeconds),
          style = MaterialTheme.typography.headlineMedium.copy(fontSize = 56.sp, lineHeight = 64.sp),
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp),
        )
      }

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedButton(
            onClick = viewModel::togglePause,
            modifier = Modifier.weight(1f),
          ) {
            Text(if (uiState.isPaused) "Resume" else "Pause")
          }
          OutlinedButton(
            onClick = viewModel::extendFiveMinutes,
            modifier = Modifier.weight(1f),
          ) {
            Text("+5m")
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Button(
          onClick = onDone,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("I'm Done")
        }
      }
    }
  }
}

package ca.bralten.actshift.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import ca.bralten.actshift.ui.components.CategoryBadge
import ca.bralten.actshift.ui.components.DeltaComparisonCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevealScreen(
  viewModel: RevealViewModel,
  onDone: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val item = uiState.item

  Scaffold(
    topBar = {
      TopAppBar(title = { Text("Before vs. After") })
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(20.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      if (item != null && item.log != null) {
        CategoryBadge(category = item.activity.category)
        Text(
          text = item.activity.title,
          style = MaterialTheme.typography.headlineSmall,
        )

        DeltaComparisonCard(log = item.log)

        if (uiState.feedbackCopy.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = uiState.feedbackCopy,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(16.dp),
            )
          }
        }

        val note = item.log.reflectionNote
        if (!note.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "What did you notice about yourself?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = note,
                style = MaterialTheme.typography.bodyLarge,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Button(
        onClick = onDone,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("reveal_done_button"),
      ) {
        Text("Done")
      }
    }
  }
}

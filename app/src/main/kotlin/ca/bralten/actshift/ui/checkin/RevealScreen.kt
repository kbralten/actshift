package ca.bralten.actshift.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.bralten.actshift.ui.components.CategoryBadge
import ca.bralten.actshift.ui.components.DeltaComparisonCard
import ca.bralten.actshift.ui.components.ReflectionNoteDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevealScreen(
  viewModel: RevealViewModel,
  onDone: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val item = uiState.item
  var showReflectionDialog by remember { mutableStateOf(false) }

  if (showReflectionDialog && item != null) {
    ReflectionNoteDialog(
      activityTitle = item.activity.title,
      initialNote = item.log?.reflectionNote.orEmpty(),
      onDismiss = { showReflectionDialog = false },
      onSave = { updatedNote ->
        viewModel.saveReflectionNote(updatedNote)
        showReflectionDialog = false
      },
    )
  }

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
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "What did you notice about yourself?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
              )
              TextButton(onClick = { showReflectionDialog = true }) {
                Text(if (note.isNullOrBlank()) "Add reflection" else "Edit")
              }
            }
            if (!note.isNullOrBlank()) {
              Text(
                text = note,
                style = MaterialTheme.typography.bodyLarge,
              )
            } else {
              Text(
                text = "Capture what stood out while it's fresh.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

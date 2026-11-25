package ca.bralten.actshift.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.bralten.actshift.data.export.TherapistExportManager
import ca.bralten.actshift.domain.model.ActivityWithLog
import ca.bralten.actshift.domain.model.InsightsTimeRange
import ca.bralten.actshift.ui.components.CategoryBadge
import ca.bralten.actshift.ui.components.DeltaComparisonCard
import ca.bralten.actshift.ui.components.ReflectionNoteDialog
import ca.bralten.actshift.ui.components.TherapistExportSheet
import ca.bralten.actshift.ui.components.TimeFormatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
  viewModel: HistoryViewModel,
  exportManager: TherapistExportManager,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  var editingItem by remember { mutableStateOf<ActivityWithLog?>(null) }
  var showExportSheet by remember { mutableStateOf(false) }

  editingItem?.let { target ->
    ReflectionNoteDialog(
      activityTitle = target.activity.title,
      initialNote = target.log?.reflectionNote.orEmpty(),
      onDismiss = { editingItem = null },
      onSave = { updatedNote ->
        viewModel.updateReflectionNote(target.activity.id, updatedNote)
        editingItem = null
      },
    )
  }

  if (showExportSheet) {
    TherapistExportSheet(
      allCompletedLogs = uiState.allLogs,
      initialRange = InsightsTimeRange.ALL_TIME,
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
        title = { Text("Activity Log") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
        actions = {
          if (uiState.allLogs.isNotEmpty()) {
            IconButton(onClick = { showExportSheet = true }) {
              Icon(Icons.Default.Share, contentDescription = "Export for therapist")
            }
          }
        },
      )
    },
  ) { innerPadding ->
    if (uiState.allLogs.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(24.dp),
        verticalArrangement = Arrangement.Center,
      ) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "Your worksheet log is empty",
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = "Completed Before & After check-ins will appear here chronologically, just like the CCI Fun & Achievement worksheet.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        item {
          FilterChip(
            selected = uiState.onlyWithReflections,
            onClick = viewModel::toggleOnlyWithReflections,
            label = { Text("With reflections only") },
          )
        }

        if (uiState.filteredLogs.isEmpty()) {
          item {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.surface,
              tonalElevation = 1.dp,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = "No reflection notes yet",
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = "Tap \"Add reflection note\" on any completed activity to record what you noticed about yourself.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          items(uiState.filteredLogs, key = { it.activity.id }) { item ->
            WorksheetHistoryEntry(
              item = item,
              onEditReflection = { editingItem = item },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WorksheetHistoryEntry(
  item: ActivityWithLog,
  onEditReflection: () -> Unit,
) {
  val log = item.log ?: return
  val displayTimestamp =
    item.activity.completedAtEpochMs ?: log.postLoggedAtEpochMs ?: item.activity.scheduledStartEpochMs

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
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = TimeFormatters.formatWorksheetDate(displayTimestamp),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CategoryBadge(category = item.activity.category)
      }

      Text(
        text = item.activity.title,
        style = MaterialTheme.typography.titleLarge,
      )

      DeltaComparisonCard(log = log)

      val note = log.reflectionNote
      if (!note.isNullOrBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "What did you notice?",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onEditReflection) {
              Text("Edit")
            }
          }
          Text(
            text = "\"$note\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      } else {
        TextButton(onClick = onEditReflection) {
          Text("Add reflection note")
        }
      }
    }
  }
}

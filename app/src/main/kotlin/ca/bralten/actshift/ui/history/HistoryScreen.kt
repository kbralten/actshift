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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.bralten.actshift.domain.model.ActivityWithLog
import ca.bralten.actshift.ui.components.CategoryBadge
import ca.bralten.actshift.ui.components.DeltaComparisonCard
import ca.bralten.actshift.ui.components.TimeFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
  viewModel: HistoryViewModel,
  modifier: Modifier = Modifier,
) {
  val logs by viewModel.completedLogs.collectAsStateWithLifecycle()

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text("Activity Log") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
  ) { innerPadding ->
    if (logs.isEmpty()) {
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
        items(logs, key = { it.activity.id }) { item ->
          WorksheetHistoryEntry(item = item)
        }
      }
    }
  }
}

@Composable
private fun WorksheetHistoryEntry(item: ActivityWithLog) {
  val log = item.log ?: return
  val displayTimestamp = item.activity.completedAtEpochMs ?: log.postLoggedAtEpochMs ?: item.activity.scheduledStartEpochMs

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
        Text(
          text = "\"$note\"",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

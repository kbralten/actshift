package ca.bralten.actshift.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ca.bralten.actshift.data.export.ExportFormat
import ca.bralten.actshift.data.export.TherapistExportManager
import ca.bralten.actshift.domain.InsightsCalculator
import ca.bralten.actshift.domain.model.ActivityWithLog
import ca.bralten.actshift.domain.model.InsightsTimeRange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TherapistExportSheet(
  allCompletedLogs: List<ActivityWithLog>,
  initialRange: InsightsTimeRange,
  countBothInFunAndAchievement: Boolean,
  exportManager: TherapistExportManager,
  onDismiss: () -> Unit,
  onMessage: (String) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var selectedRange by remember(initialRange) { mutableStateOf(initialRange) }
  var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
  var isExporting by remember { mutableStateOf(false) }

  val filteredCount =
    remember(allCompletedLogs, selectedRange) {
      InsightsCalculator.filterByTimeRange(allCompletedLogs, selectedRange).size
    }

  val savePdfLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      isExporting = true
      scope.launch {
        try {
          exportManager.exportToUri(
            targetUri = uri,
            allCompleted = allCompletedLogs,
            format = ExportFormat.PDF,
            range = selectedRange,
            countBothInFunAndAchievement = countBothInFunAndAchievement,
          )
          onMessage("Saved ${ExportFormat.PDF.label}.")
          onDismiss()
        } catch (e: Exception) {
          onMessage(e.message ?: "Could not save PDF worksheet.")
        } finally {
          isExporting = false
        }
      }
    }

  val saveCsvLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      isExporting = true
      scope.launch {
        try {
          exportManager.exportToUri(
            targetUri = uri,
            allCompleted = allCompletedLogs,
            format = ExportFormat.CSV,
            range = selectedRange,
            countBothInFunAndAchievement = countBothInFunAndAchievement,
          )
          onMessage("Saved ${ExportFormat.CSV.label}.")
          onDismiss()
        } catch (e: Exception) {
          onMessage(e.message ?: "Could not save CSV log.")
        } finally {
          isExporting = false
        }
      }
    }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Export for Therapist",
          style = MaterialTheme.typography.headlineSmall,
        )
        Text(
          text = "Share or save your Behavioural Activation log.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "Time Range",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          InsightsTimeRange.entries.forEachIndexed { index, range ->
            SegmentedButton(
              selected = selectedRange == range,
              onClick = { selectedRange = range },
              shape = SegmentedButtonDefaults.itemShape(index, InsightsTimeRange.entries.size),
              label = { Text(range.label) },
            )
          }
        }
        val noun = if (filteredCount == 1) "activity" else "activities"
        Text(
          text = "$filteredCount completed $noun in range",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Format",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ExportFormat.entries.forEach { format ->
          val isSelected = selectedFormat == format
          Surface(
            onClick = { selectedFormat = format },
            shape = RoundedCornerShape(14.dp),
            color =
              if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
              } else {
                MaterialTheme.colorScheme.surface
              },
            border =
              BorderStroke(
                width = if (isSelected) 1.5.dp else 1.dp,
                color =
                  if (isSelected) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.outlineVariant
                  },
              ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              RadioButton(
                selected = isSelected,
                onClick = { selectedFormat = format },
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = format.label,
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = format.subtitle,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = {
            val fileName = exportManager.buildFileName(selectedFormat, selectedRange)
            when (selectedFormat) {
              ExportFormat.PDF -> savePdfLauncher.launch(fileName)
              ExportFormat.CSV -> saveCsvLauncher.launch(fileName)
            }
          },
          enabled = filteredCount > 0 && !isExporting,
          modifier = Modifier.weight(1f),
        ) {
          Text("Save to Device")
        }

        Button(
          onClick = {
            isExporting = true
            scope.launch {
              try {
                val uri =
                  exportManager.exportToCacheForShare(
                    allCompleted = allCompletedLogs,
                    format = selectedFormat,
                    range = selectedRange,
                    countBothInFunAndAchievement = countBothInFunAndAchievement,
                  )
                context.startActivity(exportManager.createShareIntent(uri, selectedFormat))
                onDismiss()
              } catch (e: Exception) {
                onMessage(e.message ?: "Could not prepare export to share.")
              } finally {
                isExporting = false
              }
            }
          },
          enabled = filteredCount > 0 && !isExporting,
          modifier = Modifier.weight(1f),
        ) {
          Text("Share")
        }
      }
    }
  }
}

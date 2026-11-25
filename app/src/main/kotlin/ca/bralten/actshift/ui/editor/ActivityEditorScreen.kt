package ca.bralten.actshift.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.RecurrenceType
import ca.bralten.actshift.ui.components.QuickPickLibrarySheet
import ca.bralten.actshift.ui.components.TimeFormatters
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActivityEditorScreen(
  viewModel: ActivityEditorViewModel,
  onNavigateBack: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }
  var showQuickPick by remember { mutableStateOf(false) }

  if (showQuickPick) {
    QuickPickLibrarySheet(
      onDismiss = { showQuickPick = false },
      onSelectSuggestion = { suggestion ->
        viewModel.applySuggestion(suggestion)
        showQuickPick = false
      },
    )
  }

  if (showDatePicker) {
    val initialMillis =
      uiState.scheduledDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              val pickedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
              viewModel.updateDate(pickedDate)
            }
            showDatePicker = false
          },
        ) {
          Text("OK")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showTimePicker) {
    val timePickerState =
      rememberTimePickerState(
        initialHour = uiState.scheduledTime.hour,
        initialMinute = uiState.scheduledTime.minute,
      )
    AlertDialog(
      onDismissRequest = { showTimePicker = false },
      title = { Text("Select Start Time") },
      text = { TimePicker(state = timePickerState) },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.updateTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
            showTimePicker = false
          },
        ) {
          Text("OK")
        }
      },
      dismissButton = {
        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
      },
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(if (uiState.isEditing) "Edit Activity" else "Schedule Activity") },
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
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      OutlinedTextField(
        value = uiState.title,
        onValueChange = viewModel::updateTitle,
        label = { Text("What will you do?") },
        placeholder = { Text("e.g., Get the mail, sit in the sun") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      if (!uiState.isEditing) {
        OutlinedButton(
          onClick = { showQuickPick = true },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(
            imageVector = Icons.Default.Lightbulb,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
          )
          Text("Browse Quick-Pick Ideas")
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Duration", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(5, 10, 15).forEach { minutes ->
            FilterChip(
              selected = !uiState.isCustomDuration && uiState.durationMinutes == minutes,
              onClick = { viewModel.selectPresetDuration(minutes) },
              label = { Text("${minutes}m") },
            )
          }
          FilterChip(
            selected = uiState.isCustomDuration,
            onClick = { viewModel.enableCustomDuration() },
            label = { Text("Custom") },
          )
        }
        if (uiState.isCustomDuration) {
          OutlinedTextField(
            value = uiState.customDurationText,
            onValueChange = viewModel::updateCustomDurationText,
            label = { Text("Minutes (1–240)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Category (Fun & Achievement)", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          ActivityCategory.entries.forEachIndexed { index, category ->
            SegmentedButton(
              selected = uiState.category == category,
              onClick = { viewModel.selectCategory(category) },
              shape = SegmentedButtonDefaults.itemShape(index, ActivityCategory.entries.size),
              label = {
                Text(
                  text =
                    when (category) {
                      ActivityCategory.PLEASURE -> "Fun"
                      ActivityCategory.ACHIEVEMENT -> "Achievement"
                      ActivityCategory.BOTH -> "Both"
                    },
                  maxLines = 1,
                )
              },
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("When", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          uiState.quickPresets.forEach { preset ->
            val presetDate = preset.targetDateTime.toLocalDate()
            val presetTime = preset.targetDateTime.toLocalTime().withSecond(0).withNano(0)
            FilterChip(
              selected = uiState.scheduledDate == presetDate && uiState.scheduledTime == presetTime,
              onClick = { viewModel.applyQuickPreset(preset) },
              label = { Text(preset.label) },
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.weight(1f),
          ) {
            Text(TimeFormatters.formatDate(uiState.scheduledDate))
          }
          OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.weight(1f),
          ) {
            Text(TimeFormatters.formatTime(uiState.scheduledTime))
          }
        }
      }

      if (!uiState.isEditing) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Repeat", style = MaterialTheme.typography.titleMedium)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecurrenceType.entries.forEach { type ->
              FilterChip(
                selected = uiState.recurrenceType == type,
                onClick = { viewModel.selectRecurrence(type) },
                label = { Text(type.label) },
              )
            }
          }

          if (uiState.recurrenceType == RecurrenceType.WEEKLY) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              DayOfWeek.entries.forEach { day ->
                FilterChip(
                  selected = day in uiState.weeklyDays,
                  onClick = { viewModel.toggleWeeklyDay(day) },
                  label = {
                    Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                  },
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Button(
        onClick = { viewModel.save(onNavigateBack) },
        enabled = uiState.canSave,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(if (uiState.isEditing) "Update Activity" else "Save Activity")
      }
    }
  }
}

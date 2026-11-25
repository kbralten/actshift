package ca.bralten.actshift.ui.ideas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.bralten.actshift.domain.ActivitySuggestion
import ca.bralten.actshift.domain.EnergyLevel
import ca.bralten.actshift.domain.QuickTimePresets
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.ui.components.CategoryBadge

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickIdeasScreen(
  viewModel: QuickIdeasViewModel,
  onStartPreCheckIn: (Long) -> Unit,
  onScheduleSuggestion: (ActivitySuggestion) -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  var menuExpanded by remember { mutableStateOf(false) }
  var showAddDialog by remember { mutableStateOf(false) }
  var editingIdea by remember { mutableStateOf<ActivitySuggestion?>(null) }
  var deletingIdea by remember { mutableStateOf<ActivitySuggestion?>(null) }

  LaunchedEffect(uiState.snackbarMessage) {
    val msg = uiState.snackbarMessage ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(msg)
    viewModel.clearSnackbarMessage()
  }

  if (showAddDialog) {
    QuickIdeaEditorDialog(
      initialIdea = null,
      defaultEnergy = uiState.selectedEnergy,
      defaultCategory = uiState.selectedCategory ?: ActivityCategory.BOTH,
      onDismiss = { showAddDialog = false },
      onSave = { title, duration, category, energy ->
        viewModel.addIdea(title, duration, category, energy)
        showAddDialog = false
      },
    )
  }

  editingIdea?.let { target ->
    QuickIdeaEditorDialog(
      initialIdea = target,
      defaultEnergy = target.energyLevel,
      defaultCategory = target.category,
      onDismiss = { editingIdea = null },
      onSave = { title, duration, category, energy ->
        viewModel.updateIdea(target.dbId, title, duration, category, energy)
        editingIdea = null
      },
    )
  }

  deletingIdea?.let { target ->
    AlertDialog(
      onDismissRequest = { deletingIdea = null },
      title = { Text("Remove Quick Idea?") },
      text = {
        Text(
          "Remove \"${target.title}\" from your Quick Ideas list? You can restore starter ideas anytime from the menu.",
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteIdea(target.dbId)
            deletingIdea = null
          },
        ) {
          Text("Remove")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingIdea = null }) {
          Text("Cancel")
        }
      },
    )
  }

  Scaffold(
    modifier = modifier,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text("Quick Ideas") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
        actions = {
          Box {
            IconButton(onClick = { menuExpanded = true }) {
              Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(
              expanded = menuExpanded,
              onDismissRequest = { menuExpanded = false },
            ) {
              DropdownMenuItem(
                text = { Text("Restore starter ideas") },
                onClick = {
                  menuExpanded = false
                  viewModel.restoreStarterIdeas()
                },
              )
            }
          }
        },
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { showAddDialog = true },
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Add Idea") },
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            EnergyLevel.entries.forEachIndexed { index, level ->
              SegmentedButton(
                selected = uiState.selectedEnergy == level,
                onClick = { viewModel.selectEnergy(level) },
                shape = SegmentedButtonDefaults.itemShape(index, EnergyLevel.entries.size),
                label = { Text(level.label, maxLines = 1) },
              )
            }
          }
          Text(
            text = uiState.selectedEnergy.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      item {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          FilterChip(
            selected = uiState.selectedCategory == null,
            onClick = { viewModel.selectCategory(null) },
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
              selected = uiState.selectedCategory == category,
              onClick = { viewModel.selectCategory(category) },
              label = { Text(label) },
            )
          }
          FilterChip(
            selected = uiState.customOnly,
            onClick = viewModel::toggleCustomOnly,
            label = { Text("Custom only") },
          )
        }
      }

      if (uiState.filteredIdeas.isEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = "No matching ideas here yet",
                style = MaterialTheme.typography.titleLarge,
              )
              Text(
                text = "Add your own gentle 5–15 minute activity for ${uiState.selectedEnergy.label} energy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Button(onClick = { showAddDialog = true }) {
                Text("Add Idea")
              }
            }
          }
        }
      } else {
        items(uiState.filteredIdeas, key = { it.dbId }) { idea ->
          QuickIdeaCard(
            idea = idea,
            nextSlotLabel = QuickTimePresets.nextSlotButtonLabel(uiState.granularity),
            onEdit = { editingIdea = idea },
            onDelete = { deletingIdea = idea },
            onStartNow = { viewModel.startNow(idea, onStartPreCheckIn) },
            onScheduleNextSlot = { viewModel.scheduleNextSlot(idea) },
            onSchedule = { onScheduleSuggestion(idea) },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickIdeaCard(
  idea: ActivitySuggestion,
  nextSlotLabel: String,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onStartNow: () -> Unit,
  onScheduleNextSlot: () -> Unit,
  onSchedule: () -> Unit,
) {
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
        verticalAlignment = Alignment.Top,
      ) {
        Text(
          text = idea.title,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier
            .weight(1f)
            .padding(top = 4.dp),
        )
        Row {
          IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit idea")
          }
          IconButton(onClick = onDelete) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove idea")
          }
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Text(
            text = "${idea.durationMinutes}m",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
        CategoryBadge(category = idea.category)
        if (!idea.isStarter) {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Text(
              text = "Custom",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
      }

      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Button(onClick = onStartNow) {
          Text("Start Now")
        }
        FilledTonalButton(onClick = onScheduleNextSlot) {
          Text(nextSlotLabel)
        }
        OutlinedButton(onClick = onSchedule) {
          Text("Schedule")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun QuickIdeaEditorDialog(
  initialIdea: ActivitySuggestion?,
  defaultEnergy: EnergyLevel,
  defaultCategory: ActivityCategory,
  onDismiss: () -> Unit,
  onSave: (title: String, durationMinutes: Int, category: ActivityCategory, energyLevel: EnergyLevel) -> Unit,
) {
  var title by remember(initialIdea) { mutableStateOf(initialIdea?.title.orEmpty()) }
  var energyLevel by remember(initialIdea) { mutableStateOf(initialIdea?.energyLevel ?: defaultEnergy) }
  var category by remember(initialIdea) { mutableStateOf(initialIdea?.category ?: defaultCategory) }
  val initialDuration = initialIdea?.durationMinutes ?: 10
  var isCustomDuration by remember(initialIdea) { mutableStateOf(initialDuration !in listOf(5, 10, 15)) }
  var durationMinutes by remember(initialIdea) { mutableStateOf(initialDuration) }
  var customDurationText by remember(initialIdea) { mutableStateOf(initialDuration.toString()) }

  val effectiveDuration =
    if (isCustomDuration) {
      customDurationText.toIntOrNull() ?: 0
    } else {
      durationMinutes
    }
  val canSave = title.isNotBlank() && effectiveDuration in 1..240

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initialIdea == null) "New Quick Idea" else "Edit Quick Idea") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Activity idea") },
          placeholder = { Text("e.g., Step onto the porch for 5m") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Energy level", style = MaterialTheme.typography.labelLarge)
          SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            EnergyLevel.entries.forEachIndexed { index, level ->
              SegmentedButton(
                selected = energyLevel == level,
                onClick = { energyLevel = level },
                shape = SegmentedButtonDefaults.itemShape(index, EnergyLevel.entries.size),
                label = { Text(level.label, maxLines = 1) },
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Category", style = MaterialTheme.typography.labelLarge)
          SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ActivityCategory.entries.forEachIndexed { index, itemCategory ->
              SegmentedButton(
                selected = category == itemCategory,
                onClick = { category = itemCategory },
                shape = SegmentedButtonDefaults.itemShape(index, ActivityCategory.entries.size),
                label = {
                  Text(
                    text =
                      when (itemCategory) {
                        ActivityCategory.PLEASURE -> "Fun"
                        ActivityCategory.ACHIEVEMENT -> "Achiev."
                        ActivityCategory.BOTH -> "Both"
                      },
                    maxLines = 1,
                  )
                },
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Duration", style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 15).forEach { minutes ->
              FilterChip(
                selected = !isCustomDuration && durationMinutes == minutes,
                onClick = {
                  durationMinutes = minutes
                  isCustomDuration = false
                },
                label = { Text("${minutes}m") },
              )
            }
            FilterChip(
              selected = isCustomDuration,
              onClick = { isCustomDuration = true },
              label = { Text("Custom") },
            )
          }
          if (isCustomDuration) {
            OutlinedTextField(
              value = customDurationText,
              onValueChange = { text ->
                customDurationText = text.filter { it.isDigit() }.take(3)
                isCustomDuration = true
              },
              label = { Text("Minutes (1–240)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(title, effectiveDuration, category, energyLevel) },
        enabled = canSave,
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

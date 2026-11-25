package ca.bralten.actshift.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.bralten.actshift.domain.ActivitySuggestion
import ca.bralten.actshift.domain.model.ScheduledActivity
import ca.bralten.actshift.ui.components.CategoryBadge
import ca.bralten.actshift.ui.components.QuickPickLibrarySheet
import ca.bralten.actshift.ui.components.TimeFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
  viewModel: TodayViewModel,
  onAddActivity: () -> Unit,
  onEditActivity: (Long) -> Unit,
  onStartPreCheckIn: (Long) -> Unit,
  onOpenTimer: (Long) -> Unit,
  onOpenPostCheckIn: (Long) -> Unit,
  onScheduleSuggestion: (ActivitySuggestion) -> Unit = {},
  openQuickPickOnLaunch: Boolean = false,
  onQuickPickLaunchConsumed: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var showQuickPickSheet by remember { mutableStateOf(false) }

  LaunchedEffect(openQuickPickOnLaunch) {
    if (openQuickPickOnLaunch) {
      showQuickPickSheet = true
      onQuickPickLaunchConsumed()
    }
  }

  LaunchedEffect(Unit) { viewModel.refreshAndSync() }
  LifecycleResumeEffect(Unit) {
    viewModel.refreshAndSync()
    onPauseOrDispose {}
  }

  if (showQuickPickSheet) {
    QuickPickLibrarySheet(
      onDismiss = { showQuickPickSheet = false },
      onSelectSuggestion = { suggestion ->
        showQuickPickSheet = false
        onScheduleSuggestion(suggestion)
      },
      onStartNow = { suggestion ->
        showQuickPickSheet = false
        viewModel.startQuickPickNow(suggestion, onStartPreCheckIn)
      },
    )
  }

  uiState.abandonedPrompt?.let { abandoned ->
    AlertDialog(
      onDismissRequest = {},
      title = { Text("Did you end up doing ${abandoned.activity.title} yesterday?") },
      text = {
        Text(
          "You logged how you felt beforehand. If you completed it, you can record how you feel now — or discard this entry anytime.",
        )
      },
      confirmButton = {
        Button(onClick = { onOpenPostCheckIn(abandoned.activity.id) }) {
          Text("Log Now")
        }
      },
      dismissButton = {
        TextButton(onClick = { viewModel.discardAbandoned(abandoned.activity.id) }) {
          Text("Discard")
        }
      },
    )
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text("ActShift") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
        actions = {
          IconButton(onClick = { showQuickPickSheet = true }) {
            Icon(Icons.Default.Lightbulb, contentDescription = "Quick-Pick Ideas")
          }
        },
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onAddActivity,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Schedule Activity") },
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      if (uiState.inProgressActivities.isNotEmpty()) {
        item {
          Text(
            text = "In Progress",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        items(uiState.inProgressActivities, key = { "in_progress_${it.id}" }) { active ->
          InProgressCard(
            activity = active,
            onResumeTimer = { onOpenTimer(active.id) },
            onFinishNow = { onOpenPostCheckIn(active.id) },
          )
        }
      }

      if (uiState.nextActivity != null) {
        item {
          Text(
            text = "Next Up",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        item {
          NextActivityHeroCard(
            activity = uiState.nextActivity!!,
            onStart = { onStartPreCheckIn(uiState.nextActivity!!.id) },
            onSnooze30m = { viewModel.snooze30Minutes(uiState.nextActivity!!.id) },
            onMoveTomorrow = { viewModel.moveToTomorrow(uiState.nextActivity!!.id) },
            onEdit = { onEditActivity(uiState.nextActivity!!.id) },
            onDelete = { viewModel.deleteActivity(uiState.nextActivity!!.id) },
          )
        }
      } else if (uiState.inProgressActivities.isEmpty()) {
        item {
          EmptyTodayCard(
            onAddActivity = onAddActivity,
            onBrowseIdeas = { showQuickPickSheet = true },
          )
        }
      }

      if (uiState.todayRemaining.isNotEmpty()) {
        item {
          Text(
            text = "Later Today",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        items(uiState.todayRemaining, key = { "today_${it.id}" }) { activity ->
          ScheduledActivityRow(
            activity = activity,
            onStart = { onStartPreCheckIn(activity.id) },
            onSnooze30m = { viewModel.snooze30Minutes(activity.id) },
            onMoveTomorrow = { viewModel.moveToTomorrow(activity.id) },
            onEdit = { onEditActivity(activity.id) },
            onDelete = { viewModel.deleteActivity(activity.id) },
          )
        }
      }

      if (uiState.upcomingLater.isNotEmpty()) {
        item {
          Text(
            text = "Upcoming",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        items(uiState.upcomingLater, key = { "upcoming_${it.id}" }) { activity ->
          ScheduledActivityRow(
            activity = activity,
            onStart = { onStartPreCheckIn(activity.id) },
            onSnooze30m = { viewModel.snooze30Minutes(activity.id) },
            onMoveTomorrow = { viewModel.moveToTomorrow(activity.id) },
            onEdit = { onEditActivity(activity.id) },
            onDelete = { viewModel.deleteActivity(activity.id) },
          )
        }
      }

      if (uiState.uncompletedActivities.isNotEmpty()) {
        item {
          UncompletedTray(
            activities = uiState.uncompletedActivities,
            expanded = uiState.uncompletedExpanded,
            onToggleExpanded = { viewModel.toggleUncompletedExpanded() },
            onReschedule = { viewModel.rescheduleUncompletedToSoon(it) },
            onEdit = onEditActivity,
            onDismiss = { viewModel.dismissUncompleted(it) },
          )
        }
      }
    }
  }
}

@Composable
private fun InProgressCard(
  activity: ScheduledActivity,
  onResumeTimer: () -> Unit,
  onFinishNow: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = activity.title,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.weight(1f),
        )
        CategoryBadge(category = activity.category)
      }
      Text(
        text = "Pre-activity check-in saved (${activity.durationMinutes} min).",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (activity.timerEndEpochMs != null) {
          OutlinedButton(onClick = onResumeTimer) {
            Text("Open Timer")
          }
        }
        Button(onClick = onFinishNow) {
          Text("I'm Done")
        }
      }
    }
  }
}

@Composable
private fun NextActivityHeroCard(
  activity: ScheduledActivity,
  onStart: () -> Unit,
  onSnooze30m: () -> Unit,
  onMoveTomorrow: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "${TimeFormatters.formatActivityStart(activity.scheduledStartEpochMs)} · ${activity.durationMinutes}m",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = activity.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          CategoryBadge(category = activity.category)
        }

        Box {
          IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More actions")
          }
          ActivityOverflowMenu(
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            onSnooze30m = onSnooze30m,
            onMoveTomorrow = onMoveTomorrow,
            onEdit = onEdit,
            onDelete = onDelete,
          )
        }
      }

      Button(
        onClick = onStart,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Start Activity")
      }
    }
  }
}

@Composable
private fun ScheduledActivityRow(
  activity: ScheduledActivity,
  onStart: () -> Unit,
  onSnooze30m: () -> Unit,
  onMoveTomorrow: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Surface(
    onClick = onStart,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = activity.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${TimeFormatters.formatActivityStart(activity.scheduledStartEpochMs)} · ${activity.durationMinutes}m",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          CategoryBadge(category = activity.category)
        }
      }

      Box {
        IconButton(onClick = { menuExpanded = true }) {
          Icon(Icons.Default.MoreVert, contentDescription = "Activity options")
        }
        ActivityOverflowMenu(
          expanded = menuExpanded,
          onDismiss = { menuExpanded = false },
          onSnooze30m = onSnooze30m,
          onMoveTomorrow = onMoveTomorrow,
          onEdit = onEdit,
          onDelete = onDelete,
        )
      }
    }
  }
}

@Composable
private fun ActivityOverflowMenu(
  expanded: Boolean,
  onDismiss: () -> Unit,
  onSnooze30m: () -> Unit,
  onMoveTomorrow: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
    DropdownMenuItem(
      text = { Text("Snooze 30m") },
      onClick = {
        onDismiss()
        onSnooze30m()
      },
    )
    DropdownMenuItem(
      text = { Text("Move to Tomorrow") },
      onClick = {
        onDismiss()
        onMoveTomorrow()
      },
    )
    DropdownMenuItem(
      text = { Text("Edit") },
      onClick = {
        onDismiss()
        onEdit()
      },
    )
    DropdownMenuItem(
      text = { Text("Remove") },
      onClick = {
        onDismiss()
        onDelete()
      },
    )
  }
}

@Composable
private fun EmptyTodayCard(
  onAddActivity: () -> Unit,
  onBrowseIdeas: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Nothing scheduled yet",
        style = MaterialTheme.typography.titleLarge,
      )
      Text(
        text = "Pick one small 5–15 minute activity — something pleasant, or a tiny neglected task. Action comes before motivation.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onAddActivity) {
          Text("Schedule a 10m Activity")
        }
        OutlinedButton(onClick = onBrowseIdeas) {
          Text("Browse Ideas")
        }
      }
    }
  }
}

@Composable
private fun UncompletedTray(
  activities: List<ScheduledActivity>,
  expanded: Boolean,
  onToggleExpanded: () -> Unit,
  onReschedule: (Long) -> Unit,
  onEdit: (Long) -> Unit,
  onDismiss: (Long) -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Uncompleted / Reschedule (${activities.size})",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onToggleExpanded) {
          Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
          )
        }
      }

      AnimatedVisibility(visible = expanded) {
        Column(
          modifier = Modifier.padding(top = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          activities.forEach { item ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = item.title,
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = TimeFormatters.formatActivityStart(item.scheduledStartEpochMs),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  OutlinedButton(onClick = { onReschedule(item.id) }) {
                    Text("In 1 hour")
                  }
                  TextButton(onClick = { onEdit(item.id) }) {
                    Text("Pick time")
                  }
                  TextButton(onClick = { onDismiss(item.id) }) {
                    Text("Dismiss")
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

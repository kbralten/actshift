package ca.bralten.actshift.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.bralten.actshift.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  LifecycleResumeEffect(Unit) {
    viewModel.refreshPermissions()
    onPauseOrDispose {}
  }

  LaunchedEffect(uiState.syncMessage) {
    val msg = uiState.syncMessage ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(msg)
    viewModel.clearSyncMessage()
  }

  val calendarPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
      val granted =
        result[Manifest.permission.READ_CALENDAR] == true &&
          result[Manifest.permission.WRITE_CALENDAR] == true
      if (granted) {
        viewModel.connectCalendar()
      } else {
        viewModel.refreshPermissions()
      }
    }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
    ) {
      viewModel.refreshPermissions()
    }

  Scaffold(
    modifier = modifier,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text("Settings") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      SettingsCard(title = "Dedicated Calendar") {
        val connected =
          uiState.hasCalendarPermission &&
            uiState.preferences.dedicatedCalendarId != null &&
            uiState.preferences.calendarSyncEnabled

        Text(
          text =
            if (connected) {
              "Connected to local \"Behavioural Activation\" calendar (ID ${uiState.preferences.dedicatedCalendarId})."
            } else {
              "Disconnected. ActShift is running purely locally in its private database."
            },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (uiState.hasCalendarPermission && uiState.preferences.dedicatedCalendarId != null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Sync with dedicated calendar", style = MaterialTheme.typography.bodyLarge)
            Switch(
              checked = uiState.preferences.calendarSyncEnabled,
              onCheckedChange = viewModel::setCalendarSyncEnabled,
            )
          }
          OutlinedButton(
            onClick = viewModel::syncNow,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Sync Now")
          }
        } else {
          Button(
            onClick = {
              if (uiState.hasCalendarPermission) {
                viewModel.connectCalendar()
              } else {
                calendarPermissionLauncher.launch(
                  arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR,
                  ),
                )
              }
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Connect \"Behavioural Activation\" Calendar")
          }
        }
      }

      SettingsCard(title = "Reminders & Exact Alarms") {
        Text(
          text =
            "Notifications: ${if (uiState.hasNotificationPermission) "Allowed" else "Off"} · " +
              "Exact timing: ${if (uiState.canScheduleExactAlarms) "Allowed" else "Flexible"}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!uiState.hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          OutlinedButton(
            onClick = {
              notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Allow Notifications")
          }
        }

        if (!uiState.canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          OutlinedButton(
            onClick = {
              context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                  data = Uri.parse("package:${context.packageName}")
                },
              )
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Allow Exact Reminder Timing")
          }
        }
      }

      SettingsCard(title = "Appearance") {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          ThemeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
              selected = uiState.preferences.themeMode == mode,
              onClick = { viewModel.setThemeMode(mode) },
              shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
              label = { Text(mode.label) },
            )
          }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Use Dynamic Color", style = MaterialTheme.typography.bodyLarge)
              Text(
                text = "Match your device wallpaper palette instead of ActShift's sage theme",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = uiState.preferences.useDynamicColor,
              onCheckedChange = viewModel::setUseDynamicColor,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsCard(
  title: String,
  content: @Composable () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
      )
      content()
    }
  }
}

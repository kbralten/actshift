package ca.bralten.actshift.ui.onboarding

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.bralten.actshift.data.calendar.CalendarSyncEngine
import ca.bralten.actshift.notifications.AlarmScheduler
import ca.bralten.actshift.notifications.NotificationHelper

@Composable
fun OnboardingScreen(
  viewModel: OnboardingViewModel,
  calendarSyncEngine: CalendarSyncEngine,
  alarmScheduler: AlarmScheduler,
  notificationHelper: NotificationHelper,
  onFinished: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  fun syncStatus() {
    viewModel.updatePermissionStatus(
      calendarConnected = calendarSyncEngine.hasCalendarPermissions(),
      notificationsGranted = notificationHelper.hasNotificationPermission(),
      exactAlarmAllowed = alarmScheduler.canScheduleExactAlarms(),
    )
  }

  LaunchedEffect(Unit) { syncStatus() }
  LifecycleResumeEffect(Unit) {
    syncStatus()
    onPauseOrDispose {}
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
      }
      syncStatus()
    }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
    ) {
      syncStatus()
    }

  Scaffold { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 24.dp, vertical = 20.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
          text = "Step ${uiState.currentStep + 1} of 3",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
          progress = { (uiState.currentStep + 1) / 3f },
          modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (uiState.currentStep) {
          0 -> OnboardingWelcomeStep()
          1 ->
            OnboardingCalendarStep(
              calendarConnected = uiState.calendarConnected,
              onConnectClick = {
                if (calendarSyncEngine.hasCalendarPermissions()) {
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
            )
          else ->
            OnboardingRemindersStep(
              notificationsGranted = uiState.notificationsGranted,
              exactAlarmAllowed = uiState.exactAlarmAllowed,
              onRequestNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
              },
              onOpenExactAlarmSettings = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                  context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                      data = Uri.parse("package:${context.packageName}")
                    },
                  )
                }
              },
            )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        if (uiState.currentStep > 0) {
          TextButton(onClick = { viewModel.previousStep() }) {
            Text("Back")
          }
        } else {
          Spacer(modifier = Modifier.padding(1.dp))
        }

        when (uiState.currentStep) {
          0 -> {
            Button(onClick = { viewModel.nextStep() }) {
              Text("Continue")
            }
          }
          1 -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              TextButton(onClick = { viewModel.nextStep() }) {
                Text(if (uiState.calendarConnected) "Next" else "Skip for now")
              }
              if (uiState.calendarConnected) {
                Button(onClick = { viewModel.nextStep() }) {
                  Text("Continue")
                }
              }
            }
          }
          else -> {
            Button(
              onClick = { viewModel.completeOnboarding(onFinished) },
            ) {
              Text("Get Started")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OnboardingWelcomeStep() {
  Text(
    text = "Welcome to ActShift",
    style = MaterialTheme.typography.headlineMedium,
  )
  Text(
    text = "Small, intentional actions can gently shift low mood — even before motivation arrives.",
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )

  OnboardingPointCard(
    title = "5–15 minute micro-activities",
    body = "Keep steps tiny and doable. Balance pleasurable activities with small responsibilities.",
  )
  OnboardingPointCard(
    title = "Blind Before & After check-ins",
    body = "Rate Depression, Pleasure, and Achievement on the 0–8 clinical scale without anchoring on prior numbers.",
  )
  OnboardingPointCard(
    title = "Private & non-punitive",
    body = "All logs stay on your phone. No streaks, no guilt — missed items simply wait quietly to be rescheduled.",
  )
}

@Composable
private fun OnboardingCalendarStep(
  calendarConnected: Boolean,
  onConnectClick: () -> Unit,
) {
  Text(
    text = "Dedicated Calendar",
    style = MaterialTheme.typography.headlineMedium,
  )
  Text(
    text = "ActShift can create a separate local calendar named \"Behavioural Activation\" on your device.",
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  OnboardingPointCard(
    title = "Zero clutter",
    body = "Your activities appear in your phone's calendar app, while ActShift only reads that single calendar and ignores work or personal meetings.",
  )

  if (calendarConnected) {
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = "Connected: \"Behavioural Activation\" calendar is ready.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(16.dp),
      )
    }
  } else {
    Button(
      onClick = onConnectClick,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Create \"Behavioural Activation\" Calendar")
    }
  }
}

@Composable
private fun OnboardingRemindersStep(
  notificationsGranted: Boolean,
  exactAlarmAllowed: Boolean,
  onRequestNotifications: () -> Unit,
  onOpenExactAlarmSettings: () -> Unit,
) {
  Text(
    text = "Gentle Reminders",
    style = MaterialTheme.typography.headlineMedium,
  )
  Text(
    text = "Optional prompts remind you right at your scheduled time and let you snooze or move to tomorrow in one tap.",
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )

  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (notificationsGranted) {
      OnboardingPointCard(
        title = "Notifications enabled",
        body = "You'll receive calm start and post-activity check-in prompts.",
      )
    } else {
      Button(
        onClick = onRequestNotifications,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Allow Notifications")
      }
    }
  }

  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarmAllowed) {
    OutlinedButton(
      onClick = onOpenExactAlarmSettings,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Allow Exact Reminder Timing")
    }
  }
}

@Composable
private fun OnboardingPointCard(
  title: String,
  body: String,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

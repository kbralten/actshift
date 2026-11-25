package ca.bralten.actshift

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.bralten.actshift.data.prefs.UserPreferences
import ca.bralten.actshift.notifications.DeepLinkContract
import ca.bralten.actshift.ui.navigation.ActShiftNavHost
import ca.bralten.actshift.ui.navigation.DeepLinkTarget
import ca.bralten.actshift.ui.theme.ActShiftTheme

class MainActivity : ComponentActivity() {
  private var pendingDeepLink by mutableStateOf<DeepLinkTarget?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleIntent(intent)
    enableEdgeToEdge()

    val appContainer = (application as ActShiftApplication).container

    setContent {
      val prefs by appContainer.preferencesRepository.preferences.collectAsStateWithLifecycle(
        initialValue = null,
      )
      val resolvedPrefs = prefs ?: UserPreferences()

      ActShiftTheme(
        themeMode = resolvedPrefs.themeMode,
        useDynamicColor = resolvedPrefs.useDynamicColor,
      ) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background,
        ) {
          if (prefs == null) {
            Box(modifier = Modifier.fillMaxSize())
          } else {
            ActShiftNavHost(
              container = appContainer,
              onboardingCompleted = resolvedPrefs.onboardingCompleted,
              pendingDeepLink = pendingDeepLink,
              onDeepLinkConsumed = { pendingDeepLink = null },
            )
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    val destination = intent?.getStringExtra(DeepLinkContract.EXTRA_DESTINATION) ?: return
    val activityId = intent.getLongExtra(DeepLinkContract.EXTRA_ACTIVITY_ID, -1L)
    val needsActivityId =
      destination == DeepLinkContract.DEST_PRE_CHECKIN ||
        destination == DeepLinkContract.DEST_POST_CHECKIN ||
        destination == DeepLinkContract.DEST_ACTIVE_TIMER
    if (!needsActivityId || activityId > 0L) {
      pendingDeepLink = DeepLinkTarget(destination = destination, activityId = activityId)
    }
  }
}

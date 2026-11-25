package ca.bralten.actshift.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.bralten.actshift.ActShiftApplication
import ca.bralten.actshift.widget.NextActivationWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootRescheduleReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    val validActions =
      setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
      )
    if (action !in validActions) return

    val app = context.applicationContext as? ActShiftApplication ?: return
    val pendingResult = goAsync()

    CoroutineScope(Dispatchers.IO).launch {
      try {
        app.container.activationRepository.rescheduleAllAlarms()
        NextActivationWidgetProvider.requestUpdate(context)
      } finally {
        pendingResult.finish()
      }
    }
  }
}

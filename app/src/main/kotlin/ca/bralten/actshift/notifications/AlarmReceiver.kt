package ca.bralten.actshift.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.bralten.actshift.ActShiftApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val activityId = intent.getLongExtra(EXTRA_ACTIVITY_ID, -1L)
    if (activityId <= 0L) return

    val app = context.applicationContext as? ActShiftApplication ?: return
    val repo = app.container.activationRepository
    val pendingResult = goAsync()

    CoroutineScope(Dispatchers.IO).launch {
      try {
        when (intent.action) {
          ACTION_START_ALERT -> repo.handleStartAlarmFired(activityId)
          ACTION_COMPLETION_ALERT -> repo.handleCompletionAlarmFired(activityId)
          ACTION_SNOOZE_30M -> repo.snooze30Minutes(activityId)
          ACTION_MOVE_TOMORROW -> repo.moveToTomorrow(activityId)
          ACTION_EXTEND_TIMER_5M -> repo.extendTimer(activityId, extraMinutes = 5)
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    const val ACTION_START_ALERT = "ca.bralten.actshift.action.START_ALERT"
    const val ACTION_COMPLETION_ALERT = "ca.bralten.actshift.action.COMPLETION_ALERT"
    const val ACTION_SNOOZE_30M = "ca.bralten.actshift.action.SNOOZE_30M"
    const val ACTION_MOVE_TOMORROW = "ca.bralten.actshift.action.MOVE_TOMORROW"
    const val ACTION_EXTEND_TIMER_5M = "ca.bralten.actshift.action.EXTEND_TIMER_5M"
    const val EXTRA_ACTIVITY_ID = "extra_activity_id"
  }
}

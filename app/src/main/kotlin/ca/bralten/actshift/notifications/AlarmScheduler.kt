package ca.bralten.actshift.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import ca.bralten.actshift.data.local.entity.ScheduledActivityEntity

class AlarmScheduler(private val context: Context) {
  private val alarmManager: AlarmManager? =
    context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

  fun canScheduleExactAlarms(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      alarmManager?.canScheduleExactAlarms() == true
    } else {
      true
    }

  fun scheduleStartAlarm(activity: ScheduledActivityEntity, nowMs: Long = System.currentTimeMillis()) {
    val am = alarmManager ?: return
    if (activity.scheduledStartEpochMs < nowMs - 60_000L) return
    val triggerAt = maxOf(activity.scheduledStartEpochMs, nowMs + 1_000L)
    val intent =
      Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmReceiver.ACTION_START_ALERT
        putExtra(AlarmReceiver.EXTRA_ACTIVITY_ID, activity.id)
      }
    val pendingIntent =
      PendingIntent.getBroadcast(
        context,
        startRequestCode(activity.id),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    scheduleAlarmCompat(am, triggerAt, pendingIntent)
  }

  fun scheduleCompletionAlarm(
    activityId: Long,
    triggerAtEpochMs: Long,
    nowMs: Long = System.currentTimeMillis(),
  ) {
    val am = alarmManager ?: return
    val triggerAt = maxOf(triggerAtEpochMs, nowMs + 5_000L)
    val intent =
      Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmReceiver.ACTION_COMPLETION_ALERT
        putExtra(AlarmReceiver.EXTRA_ACTIVITY_ID, activityId)
      }
    val pendingIntent =
      PendingIntent.getBroadcast(
        context,
        completionRequestCode(activityId),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    scheduleAlarmCompat(am, triggerAt, pendingIntent)
  }

  fun cancelAlarmsForActivity(activityId: Long) {
    val am = alarmManager ?: return
    val startIntent =
      Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmReceiver.ACTION_START_ALERT
      }
    PendingIntent.getBroadcast(
      context,
      startRequestCode(activityId),
      startIntent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )?.let { am.cancel(it) }

    val completionIntent =
      Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmReceiver.ACTION_COMPLETION_ALERT
      }
    PendingIntent.getBroadcast(
      context,
      completionRequestCode(activityId),
      completionIntent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )?.let { am.cancel(it) }
  }

  private fun scheduleAlarmCompat(
    am: AlarmManager,
    triggerAtEpochMs: Long,
    pendingIntent: PendingIntent,
  ) {
    if (canScheduleExactAlarms()) {
      am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMs, pendingIntent)
    } else {
      am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMs, pendingIntent)
    }
  }

  private fun startRequestCode(activityId: Long): Int =
    (activityId * 2L).toInt()

  private fun completionRequestCode(activityId: Long): Int =
    (activityId * 2L + 1L).toInt()
}

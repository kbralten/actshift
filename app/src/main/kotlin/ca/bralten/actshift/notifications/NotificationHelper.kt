package ca.bralten.actshift.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ca.bralten.actshift.MainActivity
import ca.bralten.actshift.R
import ca.bralten.actshift.data.local.entity.ScheduledActivityEntity
import ca.bralten.actshift.ui.components.TimeFormatters

object DeepLinkContract {
  const val EXTRA_DESTINATION = "extra_destination"
  const val EXTRA_ACTIVITY_ID = "extra_activity_id"
  const val DEST_PRE_CHECKIN = "pre_checkin"
  const val DEST_POST_CHECKIN = "post_checkin"
  const val DEST_ACTIVE_TIMER = "active_timer"
}

class NotificationHelper(private val context: Context) {

  fun ensureChannels() {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    val startChannel =
      NotificationChannel(
        CHANNEL_START_REMINDERS,
        "Activity Reminders",
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = "Gentle start prompts for your scheduled Behavioural Activation activities"
      }
    val completionChannel =
      NotificationChannel(
        CHANNEL_COMPLETION_REMINDERS,
        "Check-in Prompts",
        NotificationManager.IMPORTANCE_DEFAULT,
      ).apply {
        description = "Post-activity check-in prompts to capture how your mood shifted"
      }
    val activeTimerChannel =
      NotificationChannel(
        CHANNEL_ACTIVE_TIMER,
        "Active Activity Timer",
        NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description = "Ongoing countdown timer while an activity is in progress"
        setShowBadge(false)
      }
    manager.createNotificationChannel(startChannel)
    manager.createNotificationChannel(completionChannel)
    manager.createNotificationChannel(activeTimerChannel)
  }

  fun hasNotificationPermission(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

  fun showStartNotification(activity: ScheduledActivityEntity) {
    if (!hasNotificationPermission()) return
    ensureChannels()

    val contentIntent =
      createMainActivityIntent(
        destination = DeepLinkContract.DEST_PRE_CHECKIN,
        activityId = activity.id,
        requestCode = startNotificationId(activity.id),
      )

    val snoozeIntent =
      PendingIntent.getBroadcast(
        context,
        startNotificationId(activity.id) + 10_000,
        Intent(context, AlarmReceiver::class.java).apply {
          action = AlarmReceiver.ACTION_SNOOZE_30M
          putExtra(AlarmReceiver.EXTRA_ACTIVITY_ID, activity.id)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val tomorrowIntent =
      PendingIntent.getBroadcast(
        context,
        startNotificationId(activity.id) + 20_000,
        Intent(context, AlarmReceiver::class.java).apply {
          action = AlarmReceiver.ACTION_MOVE_TOMORROW
          putExtra(AlarmReceiver.EXTRA_ACTIVITY_ID, activity.id)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(context, CHANNEL_START_REMINDERS)
        .setSmallIcon(R.drawable.ic_stat_actshift)
        .setContentTitle("Time for ${activity.durationMinutes}m: ${activity.title}")
        .setContentText("Tap to check in before you start.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(contentIntent)
        .addAction(0, "Snooze 30m", snoozeIntent)
        .addAction(0, "Move to Tomorrow", tomorrowIntent)
        .build()

    notifyIfPermitted(startNotificationId(activity.id), notification)
  }

  fun showActiveTimerNotification(
    activity: ScheduledActivityEntity,
    timerEndEpochMs: Long,
  ) {
    if (!hasNotificationPermission()) return
    ensureChannels()

    val notificationId = activeTimerNotificationId(activity.id)
    val contentIntent =
      createMainActivityIntent(
        destination = DeepLinkContract.DEST_ACTIVE_TIMER,
        activityId = activity.id,
        requestCode = notificationId,
      )

    val extendIntent =
      PendingIntent.getBroadcast(
        context,
        notificationId + 30_000,
        Intent(context, AlarmReceiver::class.java).apply {
          action = AlarmReceiver.ACTION_EXTEND_TIMER_5M
          putExtra(AlarmReceiver.EXTRA_ACTIVITY_ID, activity.id)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val doneIntent =
      createMainActivityIntent(
        destination = DeepLinkContract.DEST_POST_CHECKIN,
        activityId = activity.id,
        requestCode = notificationId + 40_000,
      )

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ACTIVE_TIMER)
        .setSmallIcon(R.drawable.ic_stat_actshift)
        .setContentTitle("In progress: ${activity.title}")
        .setContentText("Tap to open timer")
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
        .setWhen(timerEndEpochMs)
        .setShowWhen(true)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(contentIntent)
        .addAction(0, "+5m", extendIntent)
        .addAction(0, "I'm Done", doneIntent)
        .build()

    notifyIfPermitted(notificationId, notification)
  }

  fun showPausedTimerNotification(
    activity: ScheduledActivityEntity,
    remainingSeconds: Long,
  ) {
    if (!hasNotificationPermission()) return
    ensureChannels()

    val notificationId = activeTimerNotificationId(activity.id)
    val contentIntent =
      createMainActivityIntent(
        destination = DeepLinkContract.DEST_ACTIVE_TIMER,
        activityId = activity.id,
        requestCode = notificationId,
      )

    val doneIntent =
      createMainActivityIntent(
        destination = DeepLinkContract.DEST_POST_CHECKIN,
        activityId = activity.id,
        requestCode = notificationId + 40_000,
      )

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ACTIVE_TIMER)
        .setSmallIcon(R.drawable.ic_stat_actshift)
        .setContentTitle("Paused: ${activity.title}")
        .setContentText("${TimeFormatters.formatCountdown(remainingSeconds)} left · tap to resume")
        .setUsesChronometer(false)
        .setShowWhen(false)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(contentIntent)
        .addAction(0, "I'm Done", doneIntent)
        .build()

    notifyIfPermitted(notificationId, notification)
  }

  fun showCompletionNotification(activity: ScheduledActivityEntity) {
    if (!hasNotificationPermission()) return
    ensureChannels()

    val notification =
      NotificationCompat.Builder(context, CHANNEL_COMPLETION_REMINDERS)
        .setSmallIcon(R.drawable.ic_stat_actshift)
        .setContentTitle("Wrapped up ${activity.title}?")
        .setContentText("Log how you feel now.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(
          createMainActivityIntent(
            destination = DeepLinkContract.DEST_POST_CHECKIN,
            activityId = activity.id,
            requestCode = completionNotificationId(activity.id),
          ),
        )
        .build()

    notifyIfPermitted(completionNotificationId(activity.id), notification)
  }

  private fun notifyIfPermitted(notificationId: Int, notification: Notification) {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
    ) {
      return
    }
    try {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    } catch (_: SecurityException) {
      // Notification permission was revoked between check and notify.
    }
  }

  fun cancelActiveTimerNotification(activityId: Long) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    manager.cancel(activeTimerNotificationId(activityId))
  }

  fun cancelNotificationsForActivity(activityId: Long) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    manager.cancel(startNotificationId(activityId))
    manager.cancel(completionNotificationId(activityId))
    manager.cancel(activeTimerNotificationId(activityId))
  }

  private fun createMainActivityIntent(
    destination: String,
    activityId: Long,
    requestCode: Int,
  ): PendingIntent {
    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(DeepLinkContract.EXTRA_DESTINATION, destination)
        putExtra(DeepLinkContract.EXTRA_ACTIVITY_ID, activityId)
      }
    return PendingIntent.getActivity(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun startNotificationId(activityId: Long): Int =
    (activityId * 3L).toInt() + 1000

  private fun completionNotificationId(activityId: Long): Int =
    (activityId * 3L).toInt() + 1001

  private fun activeTimerNotificationId(activityId: Long): Int =
    (activityId * 3L).toInt() + 1002

  companion object {
    const val CHANNEL_START_REMINDERS = "actshift_start_reminders"
    const val CHANNEL_COMPLETION_REMINDERS = "actshift_completion_reminders"
    const val CHANNEL_ACTIVE_TIMER = "actshift_active_timer"
  }
}

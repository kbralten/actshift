package ca.bralten.actshift.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ca.bralten.actshift.ActShiftApplication
import ca.bralten.actshift.MainActivity
import ca.bralten.actshift.R
import ca.bralten.actshift.notifications.AlarmReceiver
import ca.bralten.actshift.notifications.DeepLinkContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NextActivationWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    val app = context.applicationContext as? ActShiftApplication ?: return
    val pendingResult = goAsync()

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val upcomingAndActive =
          app.container.database
            .scheduledActivityDao()
            .getUpcomingAndActive()
            .map { it.toDomain() }
        val snapshot = WidgetSnapshotBuilder.build(upcomingAndActive)

        for (widgetId in appWidgetIds) {
          val views = buildRemoteViews(context, widgetId, snapshot)
          appWidgetManager.updateAppWidget(widgetId, views)
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  private fun buildRemoteViews(
    context: Context,
    widgetId: Int,
    snapshot: WidgetSnapshot,
  ): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_next_activation)
    views.setTextViewText(R.id.widget_badge, snapshot.badgeText)
    views.setTextViewText(R.id.widget_title, snapshot.title)
    views.setTextViewText(R.id.widget_subtitle, snapshot.subtitle)
    views.setTextViewText(R.id.widget_primary_action, snapshot.primaryAction.label)
    views.setTextViewText(R.id.widget_secondary_action, snapshot.secondaryAction.label)

    views.setOnClickPendingIntent(
      R.id.widget_root,
      createActivityPendingIntent(
        context = context,
        requestCode = widgetId * 10 + 1,
        destination = null,
        activityId = -1L,
      ),
    )

    views.setOnClickPendingIntent(
      R.id.widget_primary_action,
      createActionPendingIntent(
        context = context,
        requestCode = widgetId * 10 + 2,
        action = snapshot.primaryAction,
      ),
    )

    views.setOnClickPendingIntent(
      R.id.widget_secondary_action,
      createActionPendingIntent(
        context = context,
        requestCode = widgetId * 10 + 3,
        action = snapshot.secondaryAction,
      ),
    )

    return views
  }

  private fun createActionPendingIntent(
    context: Context,
    requestCode: Int,
    action: WidgetAction,
  ): PendingIntent =
    when (action.type) {
      WidgetActionType.START_PRE_CHECKIN ->
        createActivityPendingIntent(
          context = context,
          requestCode = requestCode,
          destination = DeepLinkContract.DEST_PRE_CHECKIN,
          activityId = action.activityId,
        )
      WidgetActionType.POST_CHECKIN ->
        createActivityPendingIntent(
          context = context,
          requestCode = requestCode,
          destination = DeepLinkContract.DEST_POST_CHECKIN,
          activityId = action.activityId,
        )
      WidgetActionType.OPEN_TIMER ->
        createActivityPendingIntent(
          context = context,
          requestCode = requestCode,
          destination = DeepLinkContract.DEST_ACTIVE_TIMER,
          activityId = action.activityId,
        )
      WidgetActionType.OPEN_QUICK_PICK ->
        createActivityPendingIntent(
          context = context,
          requestCode = requestCode,
          destination = DeepLinkContract.DEST_QUICK_PICK,
          activityId = -1L,
        )
      WidgetActionType.OPEN_NEW_ACTIVITY ->
        createActivityPendingIntent(
          context = context,
          requestCode = requestCode,
          destination = DeepLinkContract.DEST_NEW_ACTIVITY,
          activityId = -1L,
        )
      WidgetActionType.OPEN_APP ->
        createActivityPendingIntent(
          context = context,
          requestCode = requestCode,
          destination = null,
          activityId = -1L,
        )
      WidgetActionType.SNOOZE_30M ->
        PendingIntent.getBroadcast(
          context,
          requestCode,
          Intent(context, AlarmReceiver::class.java).apply {
            this.action = AlarmReceiver.ACTION_SNOOZE_30M
            putExtra(AlarmReceiver.EXTRA_ACTIVITY_ID, action.activityId)
          },
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

  private fun createActivityPendingIntent(
    context: Context,
    requestCode: Int,
    destination: String?,
    activityId: Long,
  ): PendingIntent {
    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (destination != null) {
          putExtra(DeepLinkContract.EXTRA_DESTINATION, destination)
        }
        if (activityId > 0L) {
          putExtra(DeepLinkContract.EXTRA_ACTIVITY_ID, activityId)
        }
      }
    return PendingIntent.getActivity(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  companion object {
    fun requestUpdate(context: Context) {
      val appContext = context.applicationContext
      val manager = AppWidgetManager.getInstance(appContext)
      val component = ComponentName(appContext, NextActivationWidgetProvider::class.java)
      val ids = manager.getAppWidgetIds(component)
      if (ids.isEmpty()) return

      val intent =
        Intent(appContext, NextActivationWidgetProvider::class.java).apply {
          action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
          putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
      appContext.sendBroadcast(intent)
    }

    fun isPinWidgetSupported(context: Context): Boolean =
      AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    fun requestPinWidget(context: Context): Boolean {
      val manager = AppWidgetManager.getInstance(context)
      if (!manager.isRequestPinAppWidgetSupported) return false
      val component = ComponentName(context, NextActivationWidgetProvider::class.java)
      return manager.requestPinAppWidget(component, null, null)
    }
  }
}

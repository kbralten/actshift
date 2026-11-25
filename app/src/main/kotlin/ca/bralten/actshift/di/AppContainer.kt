package ca.bralten.actshift.di

import android.content.Context
import ca.bralten.actshift.data.calendar.AndroidCalendarGateway
import ca.bralten.actshift.data.calendar.CalendarSyncEngine
import ca.bralten.actshift.data.export.TherapistExportManager
import ca.bralten.actshift.data.local.ActShiftDatabase
import ca.bralten.actshift.data.prefs.UserPreferencesRepository
import ca.bralten.actshift.data.repository.ActivationRepository
import ca.bralten.actshift.notifications.AlarmScheduler
import ca.bralten.actshift.notifications.NotificationHelper

class AppContainer(context: Context) {
  private val appContext = context.applicationContext

  val database: ActShiftDatabase by lazy { ActShiftDatabase.create(appContext) }
  val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(appContext) }
  val therapistExportManager: TherapistExportManager by lazy { TherapistExportManager(appContext) }
  val calendarGateway: AndroidCalendarGateway by lazy { AndroidCalendarGateway(appContext) }
  val alarmScheduler: AlarmScheduler by lazy { AlarmScheduler(appContext) }
  val notificationHelper: NotificationHelper by lazy { NotificationHelper(appContext) }

  val calendarSyncEngine: CalendarSyncEngine by lazy {
    CalendarSyncEngine(
      gateway = calendarGateway,
      preferencesRepository = preferencesRepository,
      scheduledActivityDao = database.scheduledActivityDao(),
      templateDao = database.activityTemplateDao(),
    )
  }

  val activationRepository: ActivationRepository by lazy {
    ActivationRepository(
      templateDao = database.activityTemplateDao(),
      scheduledActivityDao = database.scheduledActivityDao(),
      activationLogDao = database.activationLogDao(),
      calendarSyncEngine = calendarSyncEngine,
      alarmScheduler = alarmScheduler,
      notificationHelper = notificationHelper,
    )
  }
}

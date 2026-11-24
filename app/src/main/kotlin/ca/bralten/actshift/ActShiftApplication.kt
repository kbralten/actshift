package ca.bralten.actshift

import android.app.Application
import ca.bralten.actshift.di.AppContainer
import ca.bralten.actshift.notifications.MaintenanceWorker

class ActShiftApplication : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
    container.notificationHelper.ensureChannels()
    MaintenanceWorker.schedulePeriodic(this)
  }
}

package ca.bralten.actshift.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ca.bralten.actshift.ActShiftApplication
import java.util.concurrent.TimeUnit

class MaintenanceWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val app = applicationContext as? ActShiftApplication ?: return Result.failure()
    app.container.activationRepository.runMaintenanceSweep()
    return Result.success()
  }

  companion object {
    private const val PERIODIC_WORK_NAME = "actshift_maintenance_periodic"

    fun schedulePeriodic(context: Context) {
      val request =
        PeriodicWorkRequestBuilder<MaintenanceWorker>(6, TimeUnit.HOURS).build()
      WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        PERIODIC_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        request,
      )
    }
  }
}

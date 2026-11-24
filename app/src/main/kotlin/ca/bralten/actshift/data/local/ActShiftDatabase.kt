package ca.bralten.actshift.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ca.bralten.actshift.data.local.dao.ActivationLogDao
import ca.bralten.actshift.data.local.dao.ActivityTemplateDao
import ca.bralten.actshift.data.local.dao.ScheduledActivityDao
import ca.bralten.actshift.data.local.entity.ActivationLogEntity
import ca.bralten.actshift.data.local.entity.ActivityTemplateEntity
import ca.bralten.actshift.data.local.entity.ScheduledActivityEntity

@Database(
  entities = [
    ActivityTemplateEntity::class,
    ScheduledActivityEntity::class,
    ActivationLogEntity::class,
  ],
  version = 1,
  exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ActShiftDatabase : RoomDatabase() {
  abstract fun activityTemplateDao(): ActivityTemplateDao

  abstract fun scheduledActivityDao(): ScheduledActivityDao

  abstract fun activationLogDao(): ActivationLogDao

  companion object {
    fun create(context: Context): ActShiftDatabase =
      Room.databaseBuilder(
        context.applicationContext,
        ActShiftDatabase::class.java,
        "actshift.db",
      ).build()
  }
}

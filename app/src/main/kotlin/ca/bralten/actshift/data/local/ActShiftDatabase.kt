package ca.bralten.actshift.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ca.bralten.actshift.data.local.dao.ActivationLogDao
import ca.bralten.actshift.data.local.dao.ActivityTemplateDao
import ca.bralten.actshift.data.local.dao.QuickIdeaDao
import ca.bralten.actshift.data.local.dao.ScheduledActivityDao
import ca.bralten.actshift.data.local.entity.ActivationLogEntity
import ca.bralten.actshift.data.local.entity.ActivityTemplateEntity
import ca.bralten.actshift.data.local.entity.QuickIdeaEntity
import ca.bralten.actshift.data.local.entity.ScheduledActivityEntity

@Database(
  entities = [
    ActivityTemplateEntity::class,
    ScheduledActivityEntity::class,
    ActivationLogEntity::class,
    QuickIdeaEntity::class,
  ],
  version = 2,
  exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ActShiftDatabase : RoomDatabase() {
  abstract fun activityTemplateDao(): ActivityTemplateDao

  abstract fun scheduledActivityDao(): ScheduledActivityDao

  abstract fun activationLogDao(): ActivationLogDao

  abstract fun quickIdeaDao(): QuickIdeaDao

  companion object {
    val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `quick_ideas` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `title` TEXT NOT NULL,
              `durationMinutes` INTEGER NOT NULL,
              `category` TEXT NOT NULL,
              `energyLevel` TEXT NOT NULL,
              `isStarter` INTEGER NOT NULL,
              `starterKey` TEXT
            )
            """.trimIndent(),
          )
        }
      }

    fun create(context: Context): ActShiftDatabase =
      Room.databaseBuilder(
        context.applicationContext,
        ActShiftDatabase::class.java,
        "actshift.db",
      )
        .addMigrations(MIGRATION_1_2)
        .build()
  }
}

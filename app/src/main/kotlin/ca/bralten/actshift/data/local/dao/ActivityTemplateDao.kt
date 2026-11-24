package ca.bralten.actshift.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ca.bralten.actshift.data.local.entity.ActivityTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityTemplateDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(template: ActivityTemplateEntity): Long

  @Update
  suspend fun update(template: ActivityTemplateEntity)

  @Query("SELECT * FROM activity_templates WHERE isActive = 1 ORDER BY createdAtEpochMs DESC")
  fun observeActiveTemplates(): Flow<List<ActivityTemplateEntity>>

  @Query("SELECT * FROM activity_templates WHERE isActive = 1")
  suspend fun getActiveTemplates(): List<ActivityTemplateEntity>

  @Query("SELECT * FROM activity_templates WHERE id = :id")
  suspend fun getById(id: Long): ActivityTemplateEntity?

  @Query("UPDATE activity_templates SET isActive = 0 WHERE id = :id")
  suspend fun deactivate(id: Long)

  @Query("UPDATE activity_templates SET calendarEventId = :calendarEventId WHERE id = :id")
  suspend fun updateCalendarEventId(id: Long, calendarEventId: Long?)
}

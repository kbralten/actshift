package ca.bralten.actshift.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ca.bralten.actshift.data.local.entity.ScheduledActivityEntity
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.ActivityStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledActivityDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(activity: ScheduledActivityEntity): Long

  @Update
  suspend fun update(activity: ScheduledActivityEntity)

  @Query("SELECT * FROM scheduled_activities WHERE id = :id")
  suspend fun getById(id: Long): ScheduledActivityEntity?

  @Query("SELECT * FROM scheduled_activities WHERE id = :id")
  fun observeById(id: Long): Flow<ScheduledActivityEntity?>

  @Query(
    """
    SELECT * FROM scheduled_activities
    WHERE status IN ('SCHEDULED', 'IN_PROGRESS')
    ORDER BY scheduledStartEpochMs ASC
    """,
  )
  fun observeUpcomingAndActive(): Flow<List<ScheduledActivityEntity>>

  @Query(
    """
    SELECT * FROM scheduled_activities
    WHERE status = 'UNCOMPLETED'
    ORDER BY scheduledStartEpochMs DESC
    """,
  )
  fun observeUncompleted(): Flow<List<ScheduledActivityEntity>>

  @Query(
    """
    SELECT * FROM scheduled_activities
    WHERE status IN ('SCHEDULED', 'IN_PROGRESS')
    ORDER BY scheduledStartEpochMs ASC
    """,
  )
  suspend fun getUpcomingAndActive(): List<ScheduledActivityEntity>

  @Query("SELECT * FROM scheduled_activities WHERE status = 'SCHEDULED'")
  suspend fun getAllScheduled(): List<ScheduledActivityEntity>

  @Query("SELECT * FROM scheduled_activities WHERE templateId = :templateId AND scheduledStartEpochMs = :startMs LIMIT 1")
  suspend fun findByTemplateAndStart(templateId: Long, startMs: Long): ScheduledActivityEntity?

  @Query(
    """
    SELECT * FROM scheduled_activities
    WHERE calendarEventId = :eventId AND calendarInstanceBeginMs = :instanceBeginMs
    LIMIT 1
    """,
  )
  suspend fun findByCalendarInstance(eventId: Long, instanceBeginMs: Long): ScheduledActivityEntity?

  @Query(
    """
    SELECT * FROM scheduled_activities
    WHERE calendarEventId = :eventId AND status = 'SCHEDULED'
    """,
  )
  suspend fun findScheduledByCalendarEventId(eventId: Long): List<ScheduledActivityEntity>

  @Query(
    """
    UPDATE scheduled_activities
    SET status = :status
    WHERE id = :id
    """,
  )
  suspend fun updateStatus(id: Long, status: ActivityStatus)

  @Query(
    """
    UPDATE scheduled_activities
    SET title = :title,
        category = :category,
        scheduledStartEpochMs = :scheduledStartEpochMs,
        durationMinutes = :durationMinutes
    WHERE id = :id
    """,
  )
  suspend fun updateDetails(
    id: Long,
    title: String,
    category: ActivityCategory,
    scheduledStartEpochMs: Long,
    durationMinutes: Int,
  )

  @Query(
    """
    UPDATE scheduled_activities
    SET scheduledStartEpochMs = :newStartMs,
        snoozeCount = snoozeCount + 1,
        status = 'SCHEDULED'
    WHERE id = :id
    """,
  )
  suspend fun reschedule(id: Long, newStartMs: Long)

  @Query("UPDATE scheduled_activities SET calendarEventId = :eventId, calendarInstanceBeginMs = :instanceBeginMs WHERE id = :id")
  suspend fun updateCalendarLink(id: Long, eventId: Long?, instanceBeginMs: Long?)

  @Query(
    """
    UPDATE scheduled_activities
    SET status = 'IN_PROGRESS',
        startedAtEpochMs = :startedAtMs,
        timerEndEpochMs = :timerEndMs
    WHERE id = :id
    """,
  )
  suspend fun markInProgress(id: Long, startedAtMs: Long, timerEndMs: Long?)

  @Query("UPDATE scheduled_activities SET timerEndEpochMs = :timerEndMs WHERE id = :id")
  suspend fun updateTimerEnd(id: Long, timerEndMs: Long?)

  @Query(
    """
    UPDATE scheduled_activities
    SET status = 'COMPLETED',
        completedAtEpochMs = :completedAtMs,
        timerEndEpochMs = NULL
    WHERE id = :id
    """,
  )
  suspend fun markCompleted(id: Long, completedAtMs: Long)

  @Query("DELETE FROM scheduled_activities WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM scheduled_activities WHERE templateId = :templateId AND status = 'SCHEDULED'")
  suspend fun deleteFutureForTemplate(templateId: Long)
}

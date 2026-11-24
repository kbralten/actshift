package ca.bralten.actshift.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ca.bralten.actshift.data.local.entity.ActivationLogEntity
import ca.bralten.actshift.data.local.entity.ActivityWithLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivationLogDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(log: ActivationLogEntity): Long

  @Query("SELECT * FROM activation_logs WHERE activityId = :activityId LIMIT 1")
  suspend fun getByActivityId(activityId: Long): ActivationLogEntity?

  @Query(
    """
    UPDATE activation_logs
    SET postDepression = :postDepression,
        postPleasure = :postPleasure,
        postAchievement = :postAchievement,
        postLoggedAtEpochMs = :postLoggedAtEpochMs,
        reflectionNote = :reflectionNote
    WHERE activityId = :activityId
    """,
  )
  suspend fun updatePostLog(
    activityId: Long,
    postDepression: Int,
    postPleasure: Int,
    postAchievement: Int,
    postLoggedAtEpochMs: Long,
    reflectionNote: String?,
  )

  @Transaction
  @Query("SELECT * FROM scheduled_activities WHERE id = :activityId LIMIT 1")
  fun observeActivityWithLog(activityId: Long): Flow<ActivityWithLogEntity?>

  @Transaction
  @Query("SELECT * FROM scheduled_activities WHERE id = :activityId LIMIT 1")
  suspend fun getActivityWithLog(activityId: Long): ActivityWithLogEntity?

  @Transaction
  @Query(
    """
    SELECT s.* FROM scheduled_activities s
    INNER JOIN activation_logs l ON s.id = l.activityId
    WHERE s.status = 'COMPLETED' AND l.postLoggedAtEpochMs IS NOT NULL
    ORDER BY COALESCE(s.completedAtEpochMs, l.postLoggedAtEpochMs) DESC
    """,
  )
  fun observeCompletedHistory(): Flow<List<ActivityWithLogEntity>>

  @Transaction
  @Query(
    """
    SELECT s.* FROM scheduled_activities s
    INNER JOIN activation_logs l ON s.id = l.activityId
    WHERE s.status = 'IN_PROGRESS'
      AND l.postLoggedAtEpochMs IS NULL
      AND l.preLoggedAtEpochMs <= :cutoffEpochMs
    ORDER BY l.preLoggedAtEpochMs ASC
    """,
  )
  fun observeAbandonedActivities(cutoffEpochMs: Long): Flow<List<ActivityWithLogEntity>>

  @Transaction
  @Query(
    """
    SELECT s.* FROM scheduled_activities s
    INNER JOIN activation_logs l ON s.id = l.activityId
    WHERE s.status = 'IN_PROGRESS'
      AND l.postLoggedAtEpochMs IS NULL
      AND l.preLoggedAtEpochMs <= :cutoffEpochMs
    ORDER BY l.preLoggedAtEpochMs ASC
    """,
  )
  suspend fun getAbandonedActivities(cutoffEpochMs: Long): List<ActivityWithLogEntity>
}

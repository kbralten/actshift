package ca.bralten.actshift.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import ca.bralten.actshift.domain.model.ActivationLog
import ca.bralten.actshift.domain.model.ActivityWithLog

@Entity(
  tableName = "activation_logs",
  foreignKeys = [
    ForeignKey(
      entity = ScheduledActivityEntity::class,
      parentColumns = ["id"],
      childColumns = ["activityId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index(value = ["activityId"], unique = true)],
)
data class ActivationLogEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val activityId: Long,
  val preDepression: Int,
  val prePleasure: Int,
  val preAchievement: Int,
  val preLoggedAtEpochMs: Long,
  val postDepression: Int? = null,
  val postPleasure: Int? = null,
  val postAchievement: Int? = null,
  val postLoggedAtEpochMs: Long? = null,
  val reflectionNote: String? = null,
) {
  fun toDomain(): ActivationLog =
    ActivationLog(
      id = id,
      activityId = activityId,
      preDepression = preDepression,
      prePleasure = prePleasure,
      preAchievement = preAchievement,
      preLoggedAtEpochMs = preLoggedAtEpochMs,
      postDepression = postDepression,
      postPleasure = postPleasure,
      postAchievement = postAchievement,
      postLoggedAtEpochMs = postLoggedAtEpochMs,
      reflectionNote = reflectionNote,
    )
}

data class ActivityWithLogEntity(
  @Embedded val activity: ScheduledActivityEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "activityId",
  )
  val log: ActivationLogEntity?,
) {
  fun toDomain(): ActivityWithLog =
    ActivityWithLog(
      activity = activity.toDomain(),
      log = log?.toDomain(),
    )
}

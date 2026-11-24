package ca.bralten.actshift.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.ActivityStatus
import ca.bralten.actshift.domain.model.ScheduledActivity

@Entity(
  tableName = "scheduled_activities",
  indices = [
    Index(value = ["calendarEventId"]),
    Index(value = ["scheduledStartEpochMs"]),
    Index(value = ["status"]),
  ],
)
data class ScheduledActivityEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val templateId: Long? = null,
  val title: String,
  val category: ActivityCategory,
  val scheduledStartEpochMs: Long,
  val durationMinutes: Int,
  val status: ActivityStatus = ActivityStatus.SCHEDULED,
  val calendarEventId: Long? = null,
  val calendarInstanceBeginMs: Long? = null,
  val startedAtEpochMs: Long? = null,
  val timerEndEpochMs: Long? = null,
  val completedAtEpochMs: Long? = null,
  val snoozeCount: Int = 0,
) {
  fun toDomain(): ScheduledActivity =
    ScheduledActivity(
      id = id,
      templateId = templateId,
      title = title,
      category = category,
      scheduledStartEpochMs = scheduledStartEpochMs,
      durationMinutes = durationMinutes,
      status = status,
      calendarEventId = calendarEventId,
      calendarInstanceBeginMs = calendarInstanceBeginMs,
      startedAtEpochMs = startedAtEpochMs,
      timerEndEpochMs = timerEndEpochMs,
      completedAtEpochMs = completedAtEpochMs,
      snoozeCount = snoozeCount,
    )
}

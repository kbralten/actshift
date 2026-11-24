package ca.bralten.actshift.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ca.bralten.actshift.domain.model.ActivityCategory

@Entity(tableName = "activity_templates")
data class ActivityTemplateEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val title: String,
  val category: ActivityCategory,
  val durationMinutes: Int,
  val recurrenceRule: String?,
  val defaultStartTimeMinutes: Int,
  val isActive: Boolean = true,
  val calendarEventId: Long? = null,
  val createdAtEpochMs: Long,
)

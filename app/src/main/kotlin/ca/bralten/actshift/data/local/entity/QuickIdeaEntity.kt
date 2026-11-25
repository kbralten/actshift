package ca.bralten.actshift.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ca.bralten.actshift.domain.ActivitySuggestion
import ca.bralten.actshift.domain.EnergyLevel
import ca.bralten.actshift.domain.model.ActivityCategory

@Entity(tableName = "quick_ideas")
data class QuickIdeaEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val title: String,
  val durationMinutes: Int,
  val category: ActivityCategory,
  val energyLevel: EnergyLevel,
  val isStarter: Boolean = false,
  val starterKey: String? = null,
) {
  fun toDomain(): ActivitySuggestion =
    ActivitySuggestion(
      id = starterKey ?: "custom_$id",
      title = title,
      durationMinutes = durationMinutes,
      category = category,
      energyLevel = energyLevel,
      dbId = id,
      isStarter = isStarter,
    )
}

package ca.bralten.actshift.data.local

import androidx.room.TypeConverter
import ca.bralten.actshift.domain.EnergyLevel
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.ActivityStatus

class Converters {
  @TypeConverter
  fun fromActivityCategory(category: ActivityCategory): String = category.name

  @TypeConverter
  fun toActivityCategory(value: String): ActivityCategory =
    ActivityCategory.entries.firstOrNull { it.name == value } ?: ActivityCategory.BOTH

  @TypeConverter
  fun fromActivityStatus(status: ActivityStatus): String = status.name

  @TypeConverter
  fun toActivityStatus(value: String): ActivityStatus =
    ActivityStatus.entries.firstOrNull { it.name == value } ?: ActivityStatus.SCHEDULED

  @TypeConverter
  fun fromEnergyLevel(level: EnergyLevel): String = level.name

  @TypeConverter
  fun toEnergyLevel(value: String): EnergyLevel =
    when (value) {
      "VERY_LOW" -> EnergyLevel.LOW
      "MODERATE" -> EnergyLevel.HIGH
      else -> EnergyLevel.entries.firstOrNull { it.name == value } ?: EnergyLevel.LOW
    }
}

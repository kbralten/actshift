package ca.bralten.actshift.data.local

import androidx.room.TypeConverter
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
}

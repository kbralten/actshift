package ca.bralten.actshift.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ca.bralten.actshift.data.local.entity.QuickIdeaEntity
import ca.bralten.actshift.domain.EnergyLevel
import ca.bralten.actshift.domain.model.ActivityCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickIdeaDao {
  @Query("SELECT * FROM quick_ideas ORDER BY isStarter ASC, id DESC")
  fun observeAll(): Flow<List<QuickIdeaEntity>>

  @Query("SELECT COUNT(*) FROM quick_ideas")
  suspend fun countAll(): Int

  @Query("SELECT starterKey FROM quick_ideas WHERE starterKey IS NOT NULL")
  suspend fun getExistingStarterKeys(): List<String>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(idea: QuickIdeaEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(ideas: List<QuickIdeaEntity>)

  @Query(
    """
    UPDATE quick_ideas
    SET title = :title,
        durationMinutes = :durationMinutes,
        category = :category,
        energyLevel = :energyLevel
    WHERE id = :id
    """,
  )
  suspend fun update(
    id: Long,
    title: String,
    durationMinutes: Int,
    category: ActivityCategory,
    energyLevel: EnergyLevel,
  )

  @Query("DELETE FROM quick_ideas WHERE id = :id")
  suspend fun deleteById(id: Long)
}

package ca.bralten.actshift.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.bralten.actshift.data.local.entity.ActivationLogEntity
import ca.bralten.actshift.data.local.entity.ScheduledActivityEntity
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.domain.model.ActivityStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActShiftDatabaseTest {

  private lateinit var db: ActShiftDatabase

  @Before
  fun createDb() {
    db =
      Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        ActShiftDatabase::class.java,
      ).allowMainThreadQueries().build()
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun insertPrePostLogAndObserveCompletedHistory() = runTest {
    val activityDao = db.scheduledActivityDao()
    val logDao = db.activationLogDao()

    val activityId =
      activityDao.insert(
        ScheduledActivityEntity(
          title = "Get the mail",
          category = ActivityCategory.ACHIEVEMENT,
          scheduledStartEpochMs = 10_000L,
          durationMinutes = 10,
          status = ActivityStatus.IN_PROGRESS,
        ),
      )

    logDao.upsert(
      ActivationLogEntity(
        activityId = activityId,
        preDepression = 6,
        prePleasure = 0,
        preAchievement = 0,
        preLoggedAtEpochMs = 10_500L,
      ),
    )

    logDao.updatePostLog(
      activityId = activityId,
      postDepression = 3,
      postPleasure = 0,
      postAchievement = 1,
      postLoggedAtEpochMs = 11_500L,
      reflectionNote = "Fresh air helped",
    )
    activityDao.markCompleted(activityId, 11_500L)

    val history = logDao.observeCompletedHistory().first()
    assertEquals(1, history.size)
    assertEquals("Get the mail", history.first().activity.title)
    assertEquals(3, history.first().log?.postDepression)
    assertEquals("Fresh air helped", history.first().log?.reflectionNote)
  }

  @Test
  fun cascadeDeleteRemovesActivationLogAndAbandonedQueryFiltersCutoff() = runTest {
    val activityDao = db.scheduledActivityDao()
    val logDao = db.activationLogDao()

    val oldId =
      activityDao.insert(
        ScheduledActivityEntity(
          title = "Old abandoned",
          category = ActivityCategory.PLEASURE,
          scheduledStartEpochMs = 1_000L,
          durationMinutes = 5,
          status = ActivityStatus.IN_PROGRESS,
        ),
      )
    logDao.upsert(
      ActivationLogEntity(
        activityId = oldId,
        preDepression = 5,
        prePleasure = 1,
        preAchievement = 1,
        preLoggedAtEpochMs = 1_000L,
      ),
    )

    val abandoned = logDao.getAbandonedActivities(cutoffEpochMs = 2_000L)
    assertEquals(1, abandoned.size)
    assertEquals(oldId, abandoned.first().activity.id)

    activityDao.deleteById(oldId)
    assertNull(logDao.getByActivityId(oldId))
  }
}

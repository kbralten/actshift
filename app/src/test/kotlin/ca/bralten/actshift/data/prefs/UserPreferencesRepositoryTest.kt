package ca.bralten.actshift.data.prefs

import ca.bralten.actshift.domain.model.ScheduleTimeGranularity
import ca.bralten.actshift.fakes.InMemoryPreferencesDataStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPreferencesRepositoryTest {

  @Test
  fun countBothInFunAndAchievementDefaultsTrueAndPersistsToggle() = runTest {
    val repo = UserPreferencesRepository(InMemoryPreferencesDataStore())
    assertTrue(repo.getCurrent().countBothInFunAndAchievement)

    repo.setCountBothInFunAndAchievement(false)
    assertFalse(repo.getCurrent().countBothInFunAndAchievement)

    repo.setCountBothInFunAndAchievement(true)
    assertTrue(repo.getCurrent().countBothInFunAndAchievement)
  }

  @Test
  fun scheduleTimeGranularityDefaultsHourAndPersistsHalfHour() = runTest {
    val repo = UserPreferencesRepository(InMemoryPreferencesDataStore())
    assertEquals(ScheduleTimeGranularity.HOUR, repo.getCurrent().scheduleTimeGranularity)

    repo.setScheduleTimeGranularity(ScheduleTimeGranularity.HALF_HOUR)
    assertEquals(ScheduleTimeGranularity.HALF_HOUR, repo.getCurrent().scheduleTimeGranularity)

    repo.setScheduleTimeGranularity(ScheduleTimeGranularity.HOUR)
    assertEquals(ScheduleTimeGranularity.HOUR, repo.getCurrent().scheduleTimeGranularity)
  }
}

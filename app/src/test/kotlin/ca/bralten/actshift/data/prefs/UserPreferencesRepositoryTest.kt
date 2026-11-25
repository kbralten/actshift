package ca.bralten.actshift.data.prefs

import ca.bralten.actshift.fakes.InMemoryPreferencesDataStore
import kotlinx.coroutines.test.runTest
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
}

package ca.bralten.actshift.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ca.bralten.actshift.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.actShiftDataStore: DataStore<Preferences> by preferencesDataStore(name = "actshift_prefs")

data class UserPreferences(
  val onboardingCompleted: Boolean = false,
  val dedicatedCalendarId: Long? = null,
  val calendarSyncEnabled: Boolean = false,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val useDynamicColor: Boolean = false,
  val countBothInFunAndAchievement: Boolean = true,
)

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
  constructor(context: Context) : this(context.applicationContext.actShiftDataStore)

  private object Keys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val DEDICATED_CALENDAR_ID = longPreferencesKey("dedicated_calendar_id")
    val CALENDAR_SYNC_ENABLED = booleanPreferencesKey("calendar_sync_enabled")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
    val COUNT_BOTH_IN_FUN_AND_ACHIEVEMENT = booleanPreferencesKey("count_both_in_fun_and_achievement")
  }

  val preferences: Flow<UserPreferences> =
    dataStore.data.map { prefs ->
      UserPreferences(
        onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
        dedicatedCalendarId = prefs[Keys.DEDICATED_CALENDAR_ID],
        calendarSyncEnabled = prefs[Keys.CALENDAR_SYNC_ENABLED] ?: false,
        themeMode = prefs[Keys.THEME_MODE]?.let { raw ->
          ThemeMode.entries.firstOrNull { it.name == raw }
        } ?: ThemeMode.SYSTEM,
        useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: false,
        countBothInFunAndAchievement = prefs[Keys.COUNT_BOTH_IN_FUN_AND_ACHIEVEMENT] ?: true,
      )
    }

  suspend fun getCurrent(): UserPreferences = preferences.first()

  suspend fun setOnboardingCompleted(completed: Boolean) {
    dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
  }

  suspend fun setDedicatedCalendar(calendarId: Long?, syncEnabled: Boolean) {
    dataStore.edit { prefs ->
      if (calendarId != null) {
        prefs[Keys.DEDICATED_CALENDAR_ID] = calendarId
      } else {
        prefs.remove(Keys.DEDICATED_CALENDAR_ID)
      }
      prefs[Keys.CALENDAR_SYNC_ENABLED] = syncEnabled
    }
  }

  suspend fun setCalendarSyncEnabled(enabled: Boolean) {
    dataStore.edit { it[Keys.CALENDAR_SYNC_ENABLED] = enabled }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    dataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setUseDynamicColor(enabled: Boolean) {
    dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
  }

  suspend fun setCountBothInFunAndAchievement(enabled: Boolean) {
    dataStore.edit { it[Keys.COUNT_BOTH_IN_FUN_AND_ACHIEVEMENT] = enabled }
  }
}

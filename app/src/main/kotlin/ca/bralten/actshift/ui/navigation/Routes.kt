package ca.bralten.actshift.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute

@Serializable data object TodayRoute

@Serializable data object InsightsRoute

@Serializable data object HistoryRoute

@Serializable data object SettingsRoute

@Serializable data class ActivityEditorRoute(val activityId: Long? = null)

@Serializable data class PreCheckInRoute(val activityId: Long)

@Serializable data class ActiveTimerRoute(val activityId: Long)

@Serializable data class PostCheckInRoute(val activityId: Long)

@Serializable data class RevealRoute(val activityId: Long)

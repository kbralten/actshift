package ca.bralten.actshift.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ca.bralten.actshift.di.AppContainer
import ca.bralten.actshift.domain.model.ActivityCategory
import ca.bralten.actshift.notifications.DeepLinkContract
import ca.bralten.actshift.ui.checkin.ActiveTimerScreen
import ca.bralten.actshift.ui.checkin.ActiveTimerViewModel
import ca.bralten.actshift.ui.checkin.PostCheckInScreen
import ca.bralten.actshift.ui.checkin.PostCheckInViewModel
import ca.bralten.actshift.ui.checkin.PreCheckInScreen
import ca.bralten.actshift.ui.checkin.PreCheckInViewModel
import ca.bralten.actshift.ui.checkin.RevealScreen
import ca.bralten.actshift.ui.checkin.RevealViewModel
import ca.bralten.actshift.ui.editor.ActivityEditorScreen
import ca.bralten.actshift.ui.editor.ActivityEditorViewModel
import ca.bralten.actshift.ui.history.HistoryScreen
import ca.bralten.actshift.ui.history.HistoryViewModel
import ca.bralten.actshift.ui.insights.InsightsScreen
import ca.bralten.actshift.ui.insights.InsightsViewModel
import ca.bralten.actshift.ui.onboarding.OnboardingScreen
import ca.bralten.actshift.ui.onboarding.OnboardingViewModel
import ca.bralten.actshift.ui.settings.SettingsScreen
import ca.bralten.actshift.ui.settings.SettingsViewModel
import ca.bralten.actshift.ui.today.TodayScreen
import ca.bralten.actshift.ui.today.TodayViewModel

data class DeepLinkTarget(
  val destination: String,
  val activityId: Long = -1L,
)

@Composable
fun ActShiftNavHost(
  container: AppContainer,
  onboardingCompleted: Boolean,
  pendingDeepLink: DeepLinkTarget?,
  onDeepLinkConsumed: () -> Unit,
) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination
  var openQuickPickOnToday by remember { mutableStateOf(false) }

  val showBottomBar =
    currentDestination?.hasRoute<TodayRoute>() == true ||
      currentDestination?.hasRoute<InsightsRoute>() == true ||
      currentDestination?.hasRoute<HistoryRoute>() == true ||
      currentDestination?.hasRoute<SettingsRoute>() == true

  LaunchedEffect(pendingDeepLink, onboardingCompleted) {
    val target = pendingDeepLink ?: return@LaunchedEffect
    if (!onboardingCompleted) return@LaunchedEffect
    when (target.destination) {
      DeepLinkContract.DEST_PRE_CHECKIN -> navController.navigate(PreCheckInRoute(target.activityId))
      DeepLinkContract.DEST_POST_CHECKIN -> navController.navigate(PostCheckInRoute(target.activityId))
      DeepLinkContract.DEST_ACTIVE_TIMER -> navController.navigate(ActiveTimerRoute(target.activityId))
      DeepLinkContract.DEST_NEW_ACTIVITY -> navController.navigate(ActivityEditorRoute())
      DeepLinkContract.DEST_QUICK_PICK -> {
        openQuickPickOnToday = true
        navController.navigate(TodayRoute) {
          popUpTo(navController.graph.findStartDestination().id) { saveState = true }
          launchSingleTop = true
          restoreState = true
        }
      }
    }
    onDeepLinkConsumed()
  }

  Scaffold(
    bottomBar = {
      if (showBottomBar) {
        NavigationBar {
          NavigationBarItem(
            selected = currentDestination.hasRoute<TodayRoute>(),
            onClick = {
              navController.navigate(TodayRoute) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            },
            icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
            label = { Text("Today") },
          )
          NavigationBarItem(
            selected = currentDestination.hasRoute<InsightsRoute>(),
            onClick = {
              navController.navigate(InsightsRoute) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            },
            icon = { Icon(Icons.Default.Insights, contentDescription = null) },
            label = { Text("Insights") },
          )
          NavigationBarItem(
            selected = currentDestination.hasRoute<HistoryRoute>(),
            onClick = {
              navController.navigate(HistoryRoute) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            },
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text("History") },
          )
          NavigationBarItem(
            selected = currentDestination.hasRoute<SettingsRoute>(),
            onClick = {
              navController.navigate(SettingsRoute) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
          )
        }
      }
    },
  ) { innerPadding ->
    val startDestination: Any = if (onboardingCompleted) TodayRoute else OnboardingRoute

    NavHost(
      navController = navController,
      startDestination = startDestination,
      modifier = Modifier.padding(innerPadding),
    ) {
      composable<OnboardingRoute> {
        val vm: OnboardingViewModel =
          viewModel(
            factory = OnboardingViewModel.factory(
              container.preferencesRepository,
              container.activationRepository,
            ),
          )
        OnboardingScreen(
          viewModel = vm,
          calendarSyncEngine = container.calendarSyncEngine,
          alarmScheduler = container.alarmScheduler,
          notificationHelper = container.notificationHelper,
          onFinished = {
            navController.navigate(TodayRoute) {
              popUpTo<OnboardingRoute> { inclusive = true }
            }
          },
        )
      }

      composable<TodayRoute> {
        val vm: TodayViewModel =
          viewModel(factory = TodayViewModel.factory(container.activationRepository))
        TodayScreen(
          viewModel = vm,
          onAddActivity = { navController.navigate(ActivityEditorRoute()) },
          onEditActivity = { id -> navController.navigate(ActivityEditorRoute(activityId = id)) },
          onStartPreCheckIn = { id -> navController.navigate(PreCheckInRoute(id)) },
          onOpenTimer = { id -> navController.navigate(ActiveTimerRoute(id)) },
          onOpenPostCheckIn = { id -> navController.navigate(PostCheckInRoute(id)) },
          onScheduleSuggestion = { suggestion ->
            navController.navigate(
              ActivityEditorRoute(
                prefillTitle = suggestion.title,
                prefillDurationMinutes = suggestion.durationMinutes,
                prefillCategoryName = suggestion.category.name,
              ),
            )
          },
          openQuickPickOnLaunch = openQuickPickOnToday,
          onQuickPickLaunchConsumed = { openQuickPickOnToday = false },
        )
      }

      composable<InsightsRoute> {
        val vm: InsightsViewModel =
          viewModel(
            factory = InsightsViewModel.factory(
              repository = container.activationRepository,
              preferencesRepository = container.preferencesRepository,
            ),
          )
        InsightsScreen(
          viewModel = vm,
          exportManager = container.therapistExportManager,
          onOpenSettings = {
            navController.navigate(SettingsRoute) {
              popUpTo(navController.graph.findStartDestination().id) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
        )
      }

      composable<HistoryRoute> {
        val vm: HistoryViewModel =
          viewModel(
            factory = HistoryViewModel.factory(
              repository = container.activationRepository,
              preferencesRepository = container.preferencesRepository,
            ),
          )
        HistoryScreen(
          viewModel = vm,
          exportManager = container.therapistExportManager,
        )
      }

      composable<SettingsRoute> {
        val vm: SettingsViewModel =
          viewModel(
            factory = SettingsViewModel.factory(
              preferencesRepository = container.preferencesRepository,
              repository = container.activationRepository,
              calendarSyncEngine = container.calendarSyncEngine,
              alarmScheduler = container.alarmScheduler,
              notificationHelper = container.notificationHelper,
            ),
          )
        SettingsScreen(viewModel = vm)
      }

      composable<ActivityEditorRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ActivityEditorRoute>()
        val prefillCategory =
          route.prefillCategoryName?.let { raw ->
            ActivityCategory.entries.firstOrNull { it.name == raw }
          }
        val vm: ActivityEditorViewModel =
          viewModel(
            factory = ActivityEditorViewModel.factory(
              activityId = route.activityId,
              repository = container.activationRepository,
              prefillTitle = route.prefillTitle,
              prefillDurationMinutes = route.prefillDurationMinutes,
              prefillCategory = prefillCategory,
            ),
          )
        ActivityEditorScreen(
          viewModel = vm,
          onNavigateBack = { navController.popBackStack() },
        )
      }

      composable<PreCheckInRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PreCheckInRoute>()
        val vm: PreCheckInViewModel =
          viewModel(
            factory = PreCheckInViewModel.factory(
              activityId = route.activityId,
              repository = container.activationRepository,
            ),
          )
        PreCheckInScreen(
          viewModel = vm,
          onNavigateBack = { navController.popBackStack() },
          onStartWithTimer = {
            navController.navigate(ActiveTimerRoute(route.activityId)) {
              popUpTo<PreCheckInRoute> { inclusive = true }
            }
          },
          onStartWithoutTimer = {
            navController.popBackStack(TodayRoute, inclusive = false)
          },
        )
      }

      composable<ActiveTimerRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ActiveTimerRoute>()
        val vm: ActiveTimerViewModel =
          viewModel(
            factory = ActiveTimerViewModel.factory(
              activityId = route.activityId,
              repository = container.activationRepository,
            ),
          )
        ActiveTimerScreen(
          viewModel = vm,
          onNavigateBack = { navController.popBackStack() },
          onDone = {
            navController.navigate(PostCheckInRoute(route.activityId)) {
              popUpTo<ActiveTimerRoute> { inclusive = true }
            }
          },
        )
      }

      composable<PostCheckInRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PostCheckInRoute>()
        val vm: PostCheckInViewModel =
          viewModel(
            factory = PostCheckInViewModel.factory(
              activityId = route.activityId,
              repository = container.activationRepository,
            ),
          )
        PostCheckInScreen(
          viewModel = vm,
          onNavigateBack = { navController.popBackStack() },
          onSaved = {
            navController.navigate(RevealRoute(route.activityId)) {
              popUpTo<PostCheckInRoute> { inclusive = true }
            }
          },
        )
      }

      composable<RevealRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<RevealRoute>()
        val vm: RevealViewModel =
          viewModel(
            factory = RevealViewModel.factory(
              activityId = route.activityId,
              repository = container.activationRepository,
            ),
          )
        RevealScreen(
          viewModel = vm,
          onDone = {
            navController.navigate(TodayRoute) {
              popUpTo<TodayRoute> { inclusive = true }
            }
          },
        )
      }
    }
  }
}

package ca.bralten.actshift.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import ca.bralten.actshift.domain.model.ThemeMode

private val LightColorScheme = lightColorScheme(
  primary = SagePrimaryLight,
  onPrimary = OnSagePrimaryLight,
  primaryContainer = SagePrimaryContainerLight,
  onPrimaryContainer = OnSagePrimaryContainerLight,
  secondary = SlateSecondaryLight,
  onSecondary = OnSlateSecondaryLight,
  secondaryContainer = SlateSecondaryContainerLight,
  onSecondaryContainer = OnSlateSecondaryContainerLight,
  tertiary = WarmAmberTertiaryLight,
  onTertiary = OnWarmAmberTertiaryLight,
  tertiaryContainer = WarmAmberTertiaryContainerLight,
  onTertiaryContainer = OnWarmAmberTertiaryContainerLight,
  background = SandBackgroundLight,
  onBackground = OnSandBackgroundLight,
  surface = SandSurfaceLight,
  onSurface = OnSandBackgroundLight,
  surfaceVariant = SandSurfaceVariantLight,
  onSurfaceVariant = OnSandSurfaceVariantLight,
  outline = OutlineLight,
  outlineVariant = OutlineVariantLight,
  error = SoftErrorLight,
  onError = OnSoftErrorLight,
  errorContainer = SoftErrorContainerLight,
  onErrorContainer = OnSoftErrorContainerLight,
)

private val DarkColorScheme = darkColorScheme(
  primary = SagePrimaryDark,
  onPrimary = OnSagePrimaryDark,
  primaryContainer = SagePrimaryContainerDark,
  onPrimaryContainer = OnSagePrimaryContainerDark,
  secondary = SlateSecondaryDark,
  onSecondary = OnSlateSecondaryDark,
  secondaryContainer = SlateSecondaryContainerDark,
  onSecondaryContainer = OnSlateSecondaryContainerDark,
  tertiary = WarmAmberTertiaryDark,
  onTertiary = OnWarmAmberTertiaryDark,
  tertiaryContainer = WarmAmberTertiaryContainerDark,
  onTertiaryContainer = OnWarmAmberTertiaryContainerDark,
  background = CharcoalBackgroundDark,
  onBackground = OnCharcoalBackgroundDark,
  surface = CharcoalSurfaceDark,
  onSurface = OnCharcoalBackgroundDark,
  surfaceVariant = CharcoalSurfaceVariantDark,
  onSurfaceVariant = OnCharcoalSurfaceVariantDark,
  outline = OutlineDark,
  outlineVariant = OutlineVariantDark,
  error = SoftErrorDark,
  onError = OnSoftErrorDark,
  errorContainer = SoftErrorContainerDark,
  onErrorContainer = OnSoftErrorContainerDark,
)

@Immutable
data class ActShiftSemanticColors(
  val pleasureContainer: Color,
  val onPleasureContainer: Color,
  val achievementContainer: Color,
  val onAchievementContainer: Color,
  val bothContainer: Color,
  val onBothContainer: Color,
  val positiveShiftContainer: Color,
  val onPositiveShiftContainer: Color,
  val neutralShiftContainer: Color,
  val onNeutralShiftContainer: Color,
)

private val LightSemanticColors = ActShiftSemanticColors(
  pleasureContainer = WarmAmberTertiaryContainerLight,
  onPleasureContainer = OnWarmAmberTertiaryContainerLight,
  achievementContainer = SlateSecondaryContainerLight,
  onAchievementContainer = OnSlateSecondaryContainerLight,
  bothContainer = SagePrimaryContainerLight,
  onBothContainer = OnSagePrimaryContainerLight,
  positiveShiftContainer = SagePrimaryContainerLight,
  onPositiveShiftContainer = OnSagePrimaryContainerLight,
  neutralShiftContainer = SandSurfaceVariantLight,
  onNeutralShiftContainer = OnSandSurfaceVariantLight,
)

private val DarkSemanticColors = ActShiftSemanticColors(
  pleasureContainer = WarmAmberTertiaryContainerDark,
  onPleasureContainer = OnWarmAmberTertiaryContainerDark,
  achievementContainer = SlateSecondaryContainerDark,
  onAchievementContainer = OnSlateSecondaryContainerDark,
  bothContainer = SagePrimaryContainerDark,
  onBothContainer = OnSagePrimaryContainerDark,
  positiveShiftContainer = SagePrimaryContainerDark,
  onPositiveShiftContainer = OnSagePrimaryContainerDark,
  neutralShiftContainer = CharcoalSurfaceVariantDark,
  onNeutralShiftContainer = OnCharcoalSurfaceVariantDark,
)

val LocalActShiftColors = staticCompositionLocalOf { LightSemanticColors }

@Composable
fun ActShiftTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  useDynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val darkTheme =
    when (themeMode) {
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
    }

  val colorScheme =
    if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
      if (darkTheme) DarkColorScheme else LightColorScheme
    }

  val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

  CompositionLocalProvider(LocalActShiftColors provides semanticColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = ActShiftTypography,
      content = content,
    )
  }
}

package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.localization.AppLanguage

private val PatrolColorScheme = darkColorScheme(
    primary = PatrolCobalt,
    onPrimary = TextPrimary,
    primaryContainer = ElevatedPanel,
    onPrimaryContainer = PatrolCobaltLight,
    secondary = PoliceGold,
    onSecondary = ObsidianNavy,
    tertiary = SirenRed,
    onTertiary = TextPrimary,
    background = ObsidianNavy,
    onBackground = TextPrimary,
    surface = TacticalSlate,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedPanel,
    onSurfaceVariant = TextSecondary,
    outline = PanelBorder,
    error = SirenRed,
    onError = TextPrimary
)

@Composable
fun PatrolCityTheme(
    language: AppLanguage = AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    val layoutDirection = if (language == AppLanguage.URDU) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = PatrolColorScheme,
            typography = buildPatrolTypography(language),
            content = content
        )
    }
}

package com.onetill.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Surface,
    onSurfaceVariant = TextSecondary,
    background = Background,
    onBackground = TextPrimary,
    outline = Border,
    outlineVariant = BorderSubtle,
    error = Error,
    onError = TextPrimary,
    scrim = Scrim,
)

@Immutable
data class OneTillColors(
    val background: Color = Background,
    val backgroundGradientStart: Color = BackgroundGradientStart,
    val drawer: Color = Drawer,
    val surface: Color = Surface,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textTertiary: Color = TextTertiary,
    val accent: Color = Accent,
    val accentLight: Color = AccentLight,
    val accentMuted: Color = AccentMuted,
    val border: Color = Border,
    val borderSubtle: Color = BorderSubtle,
    val success: Color = Success,
    val warning: Color = Warning,
    val error: Color = Error,
    val successContainer: Color = SuccessContainer,
    val warningContainer: Color = WarningContainer,
    val errorContainer: Color = ErrorContainer,
    val scrim: Color = Scrim,
)

val LocalOneTillColors = staticCompositionLocalOf { OneTillColors() }
val LocalOneTillDimens = staticCompositionLocalOf { OneTillDimens() }
val LocalOneTillExtraTypography = staticCompositionLocalOf { OneTillExtraTypography() }

@Composable
fun OneTillTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalOneTillColors provides OneTillColors(),
        LocalOneTillDimens provides OneTillDimens(),
        LocalOneTillExtraTypography provides OneTillExtraTypography(),
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = OneTillTypography,
            content = content,
        )
    }
}

object OneTillTheme {
    val colors: OneTillColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOneTillColors.current

    val dimens: OneTillDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalOneTillDimens.current

    val extraTypography: OneTillExtraTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalOneTillExtraTypography.current
}

/** 135-degree diagonal gradient used as the screen background. */
fun screenGradient(widthPx: Float, heightPx: Float): Brush = Brush.linearGradient(
    colors = listOf(BackgroundGradientStart, Background),
    start = Offset(0f, 0f),
    end = Offset(widthPx, heightPx),
)

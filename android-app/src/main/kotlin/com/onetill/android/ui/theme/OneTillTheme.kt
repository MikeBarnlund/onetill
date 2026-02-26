package com.onetill.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Error,
    onError = OnSemantic,
    scrim = Scrim,
)

@Immutable
data class OneTillExtendedColors(
    val success: androidx.compose.ui.graphics.Color = Success,
    val warning: androidx.compose.ui.graphics.Color = Warning,
    val error: androidx.compose.ui.graphics.Color = Error,
    val onSemantic: androidx.compose.ui.graphics.Color = OnSemantic,
    val disabled: androidx.compose.ui.graphics.Color = Disabled,
    val disabledContainer: androidx.compose.ui.graphics.Color = DisabledContainer,
    val highlight: androidx.compose.ui.graphics.Color = Highlight,
    val scrim: androidx.compose.ui.graphics.Color = Scrim,
)

val LocalOneTillColors = staticCompositionLocalOf { OneTillExtendedColors() }
val LocalOneTillDimens = staticCompositionLocalOf { OneTillDimens() }

@Composable
fun OneTillTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalOneTillColors provides OneTillExtendedColors(),
        LocalOneTillDimens provides OneTillDimens(),
    ) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = OneTillTypography,
            content = content,
        )
    }
}

object OneTillTheme {
    val colors: OneTillExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOneTillColors.current

    val dimens: OneTillDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalOneTillDimens.current
}

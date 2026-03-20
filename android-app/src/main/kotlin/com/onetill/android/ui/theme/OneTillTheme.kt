package com.onetill.android.ui.theme

import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = TextOnAccent,
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
    val accentDark: Color = AccentDark,
    val accentMuted: Color = AccentMuted,
    val textOnAccent: Color = TextOnAccent,
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

/**
 * Diagonal gradient background rendered via an RGB_565 intermediate bitmap.
 *
 * The dark gradient (#1A1A18 → #000000) has only ~26 distinct brightness
 * levels in 8-bit colour, producing visible banding. Android's Paint.isDither
 * is a no-op on modern 32-bit (ARGB_8888) surfaces but **does** produce
 * ordered dithering on 16-bit (RGB_565) bitmaps. By rendering the gradient
 * into RGB_565 first, the dithering scatters pixel values across band
 * boundaries, breaking up the visible steps.
 *
 * The bitmap is cached via drawWithCache and only rebuilt when size changes.
 */
fun Modifier.screenGradientBackground(): Modifier = drawWithCache {
    val w = size.width.toInt().coerceAtLeast(1)
    val h = size.height.toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
    android.graphics.Canvas(bitmap).drawRect(
        0f, 0f, w.toFloat(), h.toFloat(),
        android.graphics.Paint().apply {
            isDither = true
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                BackgroundGradientStart.toArgb(),
                Background.toArgb(),
                Shader.TileMode.CLAMP,
            )
        },
    )
    val imageBitmap = bitmap.asImageBitmap()

    onDrawBehind {
        drawImage(imageBitmap)
    }
}

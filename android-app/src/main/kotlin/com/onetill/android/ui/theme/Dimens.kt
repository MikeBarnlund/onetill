package com.onetill.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class OneTillDimens(
    // Spacing scale
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,

    // Component sizes
    val statusBarHeight: Dp = 40.dp,
    val screenHeaderHeight: Dp = 56.dp,
    val bottomActionBarHeight: Dp = 72.dp,
    val searchBarHeight: Dp = 56.dp,

    // Button heights
    val buttonHeightPrimary: Dp = 56.dp,
    val buttonHeightSecondary: Dp = 48.dp,

    // Touch targets
    val touchTargetPrimary: Dp = 52.dp,
    val touchTargetSecondary: Dp = 48.dp,

    // Radii
    val cardRadius: Dp = 12.dp,
    val buttonRadius: Dp = 12.dp,
    val inputRadius: Dp = 8.dp,
    val productImageRadius: Dp = 8.dp,
)

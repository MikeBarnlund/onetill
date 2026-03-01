package com.onetill.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class OneTillDimens(
    // Spacing scale (4dp base grid)
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,

    // Component heights
    val statusBarHeight: Dp = 28.dp,
    val screenHeaderHeight: Dp = 52.dp,
    val bottomActionBarHeight: Dp = 72.dp,
    val searchBarHeight: Dp = 48.dp,

    // Header action button
    val headerActionSize: Dp = 40.dp,
    val headerIconSize: Dp = 20.dp,

    // Button heights
    val buttonHeightPrimary: Dp = 52.dp,
    val buttonHeightSecondary: Dp = 48.dp,

    // Touch targets
    val touchTargetPrimary: Dp = 52.dp,
    val touchTargetSecondary: Dp = 48.dp,

    // Radii
    val cardRadius: Dp = 10.dp,
    val buttonRadiusPrimary: Dp = 26.dp,   // pill
    val buttonRadiusSecondary: Dp = 24.dp, // pill
    val inputRadius: Dp = 12.dp,
    val chipRadius: Dp = 10.dp,
    val productImageRadius: Dp = 10.dp,

    // Quantity stepper
    val stepperWidth: Dp = 36.dp,
    val stepperHeight: Dp = 32.dp,
    val stepperRadius: Dp = 8.dp,
    val stepperIconSize: Dp = 16.dp,

    // Status bar
    val connectivityDotSize: Dp = 6.dp,

    // Drawer
    val drawerWidth: Dp = 240.dp,

    // Input field
    val inputFieldHeight: Dp = 48.dp,
)

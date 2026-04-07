package com.chelovecheck.presentation.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Top inset from [ScanTopBar] to first content block (QR hint, manual form, URL, photo). */
    val scanContentTop = 16.dp
}

object AppMotion {
    const val durationShort = 200
    const val durationMedium = 350
    const val durationLong = 600
    val easingStandard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

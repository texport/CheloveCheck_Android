package com.chelovecheck.presentation.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chelovecheck.R
import com.chelovecheck.domain.model.AccentColor
import com.chelovecheck.domain.model.ColorSource
import com.google.android.material.color.utilities.DynamicColor
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeTonalSpot

private val Sora = FontFamily(
    Font(R.font.sora_regular, FontWeight.Normal),
    Font(R.font.sora_medium, FontWeight.Medium),
    Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_bold, FontWeight.Bold),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

@Composable
fun CheloveCheckTheme(
    darkTheme: Boolean,
    colorSource: ColorSource,
    accentColor: AccentColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDynamic = colorSource == ColorSource.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        isDynamic -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> seedColorScheme(seed = accentColor.seedArgb, isDark = darkTheme)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

private fun seedColorScheme(seed: Int, isDark: Boolean): androidx.compose.material3.ColorScheme {
    val scheme = SchemeTonalSpot(Hct.fromInt(seed), isDark, 0.0)
    val colors = MaterialDynamicColors()
    fun colorOf(dynamic: DynamicColor): Color = Color(dynamic.getArgb(scheme))
    return androidx.compose.material3.ColorScheme(
        primary = colorOf(colors.primary()),
        onPrimary = colorOf(colors.onPrimary()),
        primaryContainer = colorOf(colors.primaryContainer()),
        onPrimaryContainer = colorOf(colors.onPrimaryContainer()),
        inversePrimary = colorOf(colors.inversePrimary()),
        secondary = colorOf(colors.secondary()),
        onSecondary = colorOf(colors.onSecondary()),
        secondaryContainer = colorOf(colors.secondaryContainer()),
        onSecondaryContainer = colorOf(colors.onSecondaryContainer()),
        tertiary = colorOf(colors.tertiary()),
        onTertiary = colorOf(colors.onTertiary()),
        tertiaryContainer = colorOf(colors.tertiaryContainer()),
        onTertiaryContainer = colorOf(colors.onTertiaryContainer()),
        background = colorOf(colors.background()),
        onBackground = colorOf(colors.onBackground()),
        surface = colorOf(colors.surface()),
        onSurface = colorOf(colors.onSurface()),
        surfaceVariant = colorOf(colors.surfaceVariant()),
        onSurfaceVariant = colorOf(colors.onSurfaceVariant()),
        surfaceTint = colorOf(colors.surfaceTint()),
        inverseSurface = colorOf(colors.inverseSurface()),
        inverseOnSurface = colorOf(colors.inverseOnSurface()),
        error = colorOf(colors.error()),
        onError = colorOf(colors.onError()),
        errorContainer = colorOf(colors.errorContainer()),
        onErrorContainer = colorOf(colors.onErrorContainer()),
        outline = colorOf(colors.outline()),
        outlineVariant = colorOf(colors.outlineVariant()),
        scrim = colorOf(colors.scrim()),
        surfaceBright = colorOf(colors.surfaceBright()),
        surfaceDim = colorOf(colors.surfaceDim()),
        surfaceContainer = colorOf(colors.surfaceContainer()),
        surfaceContainerHigh = colorOf(colors.surfaceContainerHigh()),
        surfaceContainerHighest = colorOf(colors.surfaceContainerHighest()),
        surfaceContainerLow = colorOf(colors.surfaceContainerLow()),
        surfaceContainerLowest = colorOf(colors.surfaceContainerLowest()),
        primaryFixed = colorOf(colors.primaryFixed()),
        primaryFixedDim = colorOf(colors.primaryFixedDim()),
        onPrimaryFixed = colorOf(colors.onPrimaryFixed()),
        onPrimaryFixedVariant = colorOf(colors.onPrimaryFixedVariant()),
        secondaryFixed = colorOf(colors.secondaryFixed()),
        secondaryFixedDim = colorOf(colors.secondaryFixedDim()),
        onSecondaryFixed = colorOf(colors.onSecondaryFixed()),
        onSecondaryFixedVariant = colorOf(colors.onSecondaryFixedVariant()),
        tertiaryFixed = colorOf(colors.tertiaryFixed()),
        tertiaryFixedDim = colorOf(colors.tertiaryFixedDim()),
        onTertiaryFixed = colorOf(colors.onTertiaryFixed()),
        onTertiaryFixedVariant = colorOf(colors.onTertiaryFixedVariant()),
    )
}

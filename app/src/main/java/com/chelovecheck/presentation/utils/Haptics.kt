package com.chelovecheck.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

val LocalHapticsEnabled = staticCompositionLocalOf { true }

@Composable
fun HapticsProvider(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalHapticsEnabled provides enabled, content = content)
}

@Composable
fun rememberHapticPerformer(): (HapticFeedbackType) -> Unit {
    val haptics: HapticFeedback = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return remember(haptics, enabled) {
        { type ->
            if (enabled) {
                haptics.performHapticFeedback(type)
            }
        }
    }
}

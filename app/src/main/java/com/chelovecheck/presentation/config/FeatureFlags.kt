package com.chelovecheck.presentation.config

/**
 * Runtime-safe switches for progressive rollout and rollback.
 * Keep defaults conservative and enable gradually.
 */
object FeatureFlags {
    const val adaptiveNavigationRailEnabled: Boolean = true
    const val enhancedScanValidationEnabled: Boolean = true
}

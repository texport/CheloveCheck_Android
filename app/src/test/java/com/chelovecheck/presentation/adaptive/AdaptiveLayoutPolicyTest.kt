package com.chelovecheck.presentation.adaptive

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutPolicyTest {

    @Test
    fun expandedFlat_enablesRailAndTwoPane() {
        val policy = AdaptiveLayoutPolicy.from(
            WindowAdaptiveInfo(
                widthSizeClass = WindowWidthSizeClass.Expanded,
                posture = DevicePosture.Flat,
                hingeBounds = null,
            ),
        )

        assertTrue(policy.useNavigationRail)
        assertTrue(policy.preferTwoPane)
    }

    @Test
    fun expandedTableTop_disablesTwoPane() {
        val policy = AdaptiveLayoutPolicy.from(
            WindowAdaptiveInfo(
                widthSizeClass = WindowWidthSizeClass.Expanded,
                posture = DevicePosture.TableTop,
                hingeBounds = null,
            ),
        )

        assertTrue(policy.useNavigationRail)
        assertFalse(policy.preferTwoPane)
    }

    @Test
    fun compact_disablesRailAndTwoPane() {
        val policy = AdaptiveLayoutPolicy.from(
            WindowAdaptiveInfo(
                widthSizeClass = WindowWidthSizeClass.Compact,
                posture = DevicePosture.Flat,
                hingeBounds = null,
            ),
        )

        assertFalse(policy.useNavigationRail)
        assertFalse(policy.preferTwoPane)
    }
}

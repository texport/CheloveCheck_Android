package com.chelovecheck.presentation.adaptive

import android.app.Activity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class DevicePosture {
    Flat,
    Book,
    TableTop,
}

data class WindowAdaptiveInfo(
    val widthSizeClass: WindowWidthSizeClass,
    val posture: DevicePosture,
    val hingeBounds: Rect?,
)

data class AdaptiveLayoutPolicy(
    val useNavigationRail: Boolean,
    val preferTwoPane: Boolean,
    val hingeBounds: Rect?,
) {
    companion object {
        fun from(info: WindowAdaptiveInfo): AdaptiveLayoutPolicy {
            val expanded = info.widthSizeClass == WindowWidthSizeClass.Expanded
            val useRail = expanded
            val twoPane = expanded && info.posture != DevicePosture.TableTop
            return AdaptiveLayoutPolicy(
                useNavigationRail = useRail,
                preferTwoPane = twoPane,
                hingeBounds = info.hingeBounds,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
fun rememberAdaptiveLayoutPolicy(): AdaptiveLayoutPolicy {
    val context = LocalContext.current
    val activity = context as Activity
    val windowSizeClass = calculateWindowSizeClass(activity)
    val tracker = remember(activity) { WindowInfoTracker.getOrCreate(activity) }
    val postureInfo by remember(activity, tracker) {
        tracker.windowLayoutInfo(activity).map { layoutInfo ->
            val feature = layoutInfo.displayFeatures
                .filterIsInstance<FoldingFeature>()
                .firstOrNull()
            val posture = when (feature?.state) {
                FoldingFeature.State.HALF_OPENED -> {
                    if (feature.orientation == FoldingFeature.Orientation.HORIZONTAL) {
                        DevicePosture.TableTop
                    } else {
                        DevicePosture.Book
                    }
                }
                else -> DevicePosture.Flat
            }
            val hinge = feature?.bounds?.let {
                Rect(
                    left = it.left.toFloat(),
                    top = it.top.toFloat(),
                    right = it.right.toFloat(),
                    bottom = it.bottom.toFloat(),
                )
            }
            WindowAdaptiveInfo(windowSizeClass.widthSizeClass, posture, hinge)
        }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(
        initialValue = WindowAdaptiveInfo(
            widthSizeClass = windowSizeClass.widthSizeClass,
            posture = DevicePosture.Flat,
            hingeBounds = null,
        ),
    )
    return remember(postureInfo) { AdaptiveLayoutPolicy.from(postureInfo) }
}

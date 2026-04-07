package com.chelovecheck.presentation.components

import android.util.Size
import android.view.MotionEvent
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.PreviewView
import java.util.concurrent.TimeUnit

/**
 * CameraX tuning for small QR codes: higher analysis resolution, light zoom, tap-to-focus.
 * Keeps [QrScannerView] composable thin (KISS).
 */
internal object QrCameraBinder {

    private val analysisTargetSize = Size(1920, 1080)

    fun buildImageAnalysis(targetRotation: Int): ImageAnalysis {
        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    analysisTargetSize,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                ),
            )
            .build()
        return ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(targetRotation)
            .build()
    }

    /**
     * Slight zoom helps tiny QR occupy more sensor pixels; clamped to device range.
     */
    fun applyBarcodeScanZoom(camera: Camera) {
        val control = camera.cameraControl
        val zoomState = camera.cameraInfo.zoomState.value ?: return
        val minZ = zoomState.minZoomRatio
        val maxZ = zoomState.maxZoomRatio
        val target = (minZ + maxZ) * 0.38f
        val ratio = target.coerceIn(minZ, maxZ)
        runCatching { control.setZoomRatio(ratio) }
    }

    fun attachTapToFocus(
        previewView: PreviewView,
        camera: Camera,
        onTap: (x: Float, y: Float) -> Unit = { _, _ -> },
    ) {
        previewView.setOnTouchListener { _, event ->
            if (event.action != MotionEvent.ACTION_UP) {
                return@setOnTouchListener false
            }
            onTap(event.x, event.y)
            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()
            runCatching { camera.cameraControl.startFocusAndMetering(action) }
            true
        }
    }

    fun clearTapToFocus(previewView: PreviewView) {
        previewView.setOnTouchListener(null)
    }
}

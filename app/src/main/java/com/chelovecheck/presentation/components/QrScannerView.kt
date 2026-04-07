package com.chelovecheck.presentation.components

import android.annotation.SuppressLint
import android.util.Rational
import android.view.Surface
import android.view.View
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun QrScannerView(
    enabled: Boolean,
    torchEnabled: Boolean,
    onQrScanned: (String) -> Unit,
    onError: (Throwable) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val analysisExecutor = remember { Dispatchers.Default.limitedParallelism(1).asExecutor() }
    val uiScope = rememberCoroutineScope()
    val cameraState: MutableState<Camera?> = remember { mutableStateOf(null) }
    val enabledState: MutableState<Boolean> = remember { mutableStateOf(enabled) }
    val cameraProviderState: MutableState<ProcessCameraProvider?> = remember { mutableStateOf(null) }
    val scanLock = remember { AtomicBoolean(false) }
    val previewSizeState: MutableState<IntSize> = remember { mutableStateOf(IntSize.Zero) }
    val lastBoundSize: MutableState<IntSize> = remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(enabled) {
        enabledState.value = enabled
        if (enabled) {
            scanLock.set(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderState.value?.unbindAll()
        }
    }

    DisposableEffect(previewView) {
        val listener = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            val width = view.width
            val height = view.height
            if (width > 0 && height > 0) {
                previewSizeState.value = IntSize(width, height)
            }
        }
        previewView.addOnLayoutChangeListener(listener)
        onDispose { previewView.removeOnLayoutChangeListener(listener) }
    }

    LaunchedEffect(Unit) {
        val cameraProvider = Dispatchers.Main.asExecutor().let { executorMain ->
            val future = ProcessCameraProvider.getInstance(context)
            suspendCancellableCoroutine<ProcessCameraProvider> { continuation ->
                future.addListener(
                    { continuation.resume(future.get()) },
                    executorMain,
                )
            }
        }
        cameraProviderState.value = cameraProvider
        cameraState.value?.cameraControl?.enableTorch(torchEnabled)
    }

    LaunchedEffect(cameraProviderState.value, previewSizeState.value) {
        val provider = cameraProviderState.value ?: return@LaunchedEffect
        val size = previewSizeState.value
        if (size == IntSize.Zero || size == lastBoundSize.value) return@LaunchedEffect
        val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
        val viewPort = ViewPort.Builder(Rational(size.width, size.height), rotation)
            .setScaleType(ViewPort.FILL_CENTER)
            .build()
        bindCamera(
            cameraProvider = provider,
            previewView = previewView,
            lifecycleOwner = lifecycleOwner,
            executor = analysisExecutor,
            enabledState = enabledState,
            uiScope = uiScope,
            scanLock = scanLock,
            onQrScanned = onQrScanned,
            onError = onError,
            cameraState = cameraState,
            viewPort = viewPort,
            targetRotation = rotation,
        )
        lastBoundSize.value = size
    }

    LaunchedEffect(torchEnabled) {
        cameraState.value?.cameraControl?.enableTorch(torchEnabled)
    }

    var focusIndicator by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(focusIndicator) {
        if (focusIndicator != null) {
            delay(1000)
            focusIndicator = null
        }
    }

    LaunchedEffect(cameraState.value) {
        val cam = cameraState.value
        if (cam != null) {
            delay(80)
            QrCameraBinder.attachTapToFocus(previewView, cam) { x, y ->
                focusIndicator = Offset(x, y)
            }
        } else {
            QrCameraBinder.clearTapToFocus(previewView)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = focusIndicator
            if (center != null) {
                drawCircle(
                    color = Color(0xFF90CAF9).copy(alpha = 0.95f),
                    radius = 32.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
    }
}

private fun bindCamera(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    executor: Executor,
    enabledState: MutableState<Boolean>,
    uiScope: kotlinx.coroutines.CoroutineScope,
    scanLock: AtomicBoolean,
    onQrScanned: (String) -> Unit,
    onError: (Throwable) -> Unit,
    cameraState: MutableState<Camera?>,
    viewPort: ViewPort,
    targetRotation: Int,
) {
    val preview = Preview.Builder()
        .setTargetRotation(targetRotation)
        .build()
        .apply {
        setSurfaceProvider(previewView.surfaceProvider)
    }

    val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    val analysis = QrCameraBinder.buildImageAnalysis(targetRotation)

    analysis.setAnalyzer(executor) { imageProxy ->
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return@setAnalyzer
        }

        if (!enabledState.value) {
            imageProxy.close()
            return@setAnalyzer
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val barcode = barcodes.firstOrNull()
                val value = barcode?.rawValue
                if (!value.isNullOrBlank() && scanLock.compareAndSet(false, true)) {
                    uiScope.launch { onQrScanned(value) }
                }
            }
            .addOnFailureListener { error ->
                uiScope.launch {
                    onError(error)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    val useCaseGroup = UseCaseGroup.Builder()
        .setViewPort(viewPort)
        .addUseCase(preview)
        .addUseCase(analysis)
        .build()

    cameraProvider.unbindAll()
    val camera = cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        useCaseGroup,
    )

    cameraState.value = camera
}

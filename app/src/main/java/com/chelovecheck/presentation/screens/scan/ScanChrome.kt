package com.chelovecheck.presentation.screens.scan

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chelovecheck.R
import com.chelovecheck.presentation.model.ScanMode
import com.chelovecheck.presentation.theme.AppMotion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScanTopBar(
    scanMode: ScanMode,
    torchEnabled: Boolean,
    onClose: () -> Unit,
    onToggleTorch: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.scan_title)) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
            }
        },
        actions = {
            if (scanMode == ScanMode.QR) {
                IconButton(onClick = onToggleTorch) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                        contentDescription = stringResource(R.string.scan_flashlight),
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ScanModeToolbar(
    current: ScanMode,
    onSelected: (ScanMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        Triple(ScanMode.QR, Icons.Outlined.QrCodeScanner, stringResource(R.string.scan_mode_qr)),
        Triple(ScanMode.MANUAL, Icons.Outlined.TextFields, stringResource(R.string.scan_mode_manual)),
        Triple(ScanMode.PHOTO, Icons.Outlined.Image, stringResource(R.string.scan_mode_photo)),
        Triple(ScanMode.URL, Icons.Outlined.Link, stringResource(R.string.scan_mode_url)),
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { (mode, icon, label) ->
                if (current == mode) {
                    FilledTonalIconButton(onClick = { onSelected(mode) }) {
                        Icon(icon, contentDescription = label)
                    }
                } else {
                    OutlinedIconButton(onClick = { onSelected(mode) }) {
                        Icon(icon, contentDescription = label)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ScanOverlay() {
    val accent = MaterialTheme.colorScheme.primary
    val scrim = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.scrim.copy(alpha = 0f),
            MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
        ),
    )
    val density = LocalDensity.current
    val infinite = rememberInfiniteTransition(label = "scan-overlay")
    val pulse by infinite.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AppMotion.durationMedium,
                easing = AppMotion.easingStandard,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan-pulse",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrim),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = with(density) { 2.dp.toPx() }
            val frameSize = minOf(size.width, size.height) * 0.62f
            val left = (size.width - frameSize) / 2f
            val top = (size.height - frameSize) / 2f
            val width = frameSize
            val height = frameSize
            if (width > 0f && height > 0f) {
                val corner = minOf(width, height) * 0.18f
                val thick = strokeWidth * 1.6f
                val right = left + width
                val bottom = top + height
                val frameColor = accent.copy(alpha = pulse)
                drawLine(frameColor, Offset(left, top), Offset(left + corner, top), thick)
                drawLine(frameColor, Offset(left, top), Offset(left, top + corner), thick)
                drawLine(frameColor, Offset(right, top), Offset(right - corner, top), thick)
                drawLine(frameColor, Offset(right, top), Offset(right, top + corner), thick)
                drawLine(frameColor, Offset(left, bottom), Offset(left + corner, bottom), thick)
                drawLine(frameColor, Offset(left, bottom), Offset(left, bottom - corner), thick)
                drawLine(frameColor, Offset(right, bottom), Offset(right - corner, bottom), thick)
                drawLine(frameColor, Offset(right, bottom), Offset(right, bottom - corner), thick)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ScanLoadingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.QrCodeScanner,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.size(12.dp))
            androidx.compose.material3.LoadingIndicator(modifier = Modifier.size(48.dp))
        }
    }
}

package com.chelovecheck.presentation.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelovecheck.domain.model.AfterScanAction
import com.chelovecheck.presentation.components.QrScannerView
import com.chelovecheck.presentation.model.ScanMode
import com.chelovecheck.presentation.permissions.PermissionManager
import com.chelovecheck.presentation.screens.scan.InstructionCard
import com.chelovecheck.presentation.screens.scan.ManualEntryContent
import com.chelovecheck.presentation.screens.scan.PermissionCard
import com.chelovecheck.presentation.screens.scan.PhotoScanContent
import com.chelovecheck.presentation.navigation.OfdCaptchaNav
import com.chelovecheck.presentation.screens.scan.ScanErrorDialog
import com.chelovecheck.presentation.screens.scan.ScanLoadingOverlay
import com.chelovecheck.presentation.screens.scan.ScanModeToolbar
import com.chelovecheck.presentation.screens.scan.ScanOverlay
import com.chelovecheck.presentation.screens.scan.ScanSavedDialog
import com.chelovecheck.presentation.screens.scan.ScanTopBar
import com.chelovecheck.presentation.screens.scan.UrlEntryContent
import com.chelovecheck.presentation.screens.scan.createPhotoUri
import com.chelovecheck.presentation.screens.scan.isCameraPermissionPermanentlyDenied
import com.chelovecheck.presentation.screens.scan.openAppSettings
import com.chelovecheck.presentation.screens.scan.openReceiptInBrowser
import com.chelovecheck.presentation.screens.scan.openSupport
import com.chelovecheck.presentation.utils.rememberHapticPerformer
import com.chelovecheck.presentation.theme.AppMotion
import com.chelovecheck.presentation.theme.AppSpacing
import com.chelovecheck.presentation.viewmodel.ScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScanScreen(
    onReceiptSaved: (String) -> Unit,
    onClose: () -> Unit,
    onNavigateToOfdCaptcha: (String) -> Unit = {},
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val afterScanAction by viewModel.afterScanAction.collectAsStateWithLifecycle()
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraPermissionRequested by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity
    val haptics = rememberHapticPerformer()
    val permissionManager = remember(context) { PermissionManager(context.applicationContext) }
    var savedDialogFiscalSign by remember { mutableStateOf<String?>(null) }

    var hasPermission by remember { mutableStateOf(permissionManager.hasCameraPermission()) }
    val isPermanentlyDenied = remember(hasPermission, cameraPermissionRequested) {
        isCameraPermissionPermanentlyDenied(
            activity = activity,
            permission = permissionManager.cameraPermission,
            hasPermission = hasPermission,
            wasRequested = cameraPermissionRequested,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted },
    )

    var latestPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = latestPhotoUri
        if (success && uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes != null) {
                    viewModel.onPhotoSelected(bytes)
                } else {
                    viewModel.onPhotoReadFailed()
                }
            }
        } else if (!success) {
            viewModel.onPhotoReadFailed()
        }
    }

    LaunchedEffect(state.scanMode) {
        if (state.scanMode == ScanMode.QR) {
            hasPermission = permissionManager.hasCameraPermission()
            if (!hasPermission) {
                cameraPermissionRequested = true
                permissionLauncher.launch(permissionManager.cameraPermission)
            }
        }
    }

    LaunchedEffect(state.savedFiscalSign, afterScanAction) {
        val fiscalSign = state.savedFiscalSign
        if (fiscalSign != null) {
            haptics(HapticFeedbackType.TextHandleMove)
            if (afterScanAction == AfterScanAction.OPEN_RECEIPT) {
                onReceiptSaved(fiscalSign)
            } else {
                savedDialogFiscalSign = fiscalSign
            }
            viewModel.consumeSavedReceipt()
        }
    }

    val error = state.error
    LaunchedEffect(error) {
        if (error != null) {
            haptics(HapticFeedbackType.LongPress)
        }
    }
    if (error != null) {
        ScanErrorDialog(
            error = error,
            onDismiss = viewModel::consumeError,
            onOpenReceipt = onReceiptSaved,
            onOpenSupport = { openSupport(context) },
            onVerifyOfdInApp = { receiptUrl ->
                onNavigateToOfdCaptcha(OfdCaptchaNav.encodeUrlForNav(receiptUrl))
            },
            onOpenReceiptUrlInBrowser = { url ->
                openReceiptInBrowser(context, url)
            },
        )
    }

    if (savedDialogFiscalSign != null) {
        val fiscalSign = savedDialogFiscalSign.orEmpty()
        ScanSavedDialog(
            fiscalSign = fiscalSign,
            onDismiss = { savedDialogFiscalSign = null },
            onOpenReceipt = {
                savedDialogFiscalSign = null
                onReceiptSaved(it)
            },
        )
    }

    Scaffold(
        topBar = {
            ScanTopBar(
                scanMode = state.scanMode,
                torchEnabled = torchEnabled,
                onClose = onClose,
                onToggleTorch = { torchEnabled = !torchEnabled },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            AnimatedContent(
                targetState = state.scanMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(AppMotion.durationShort)) togetherWith
                        fadeOut(animationSpec = tween(AppMotion.durationShort))
                },
                label = "scan_mode",
            ) { mode ->
                when (mode) {
                    ScanMode.QR -> {
                        if (hasPermission) {
                            QrScannerView(
                                enabled = !state.isProcessing && state.error == null,
                                torchEnabled = torchEnabled,
                                onQrScanned = viewModel::onQrScanned,
                                onError = viewModel::onCameraError,
                            )
                        }
                        ScanOverlay()
                        if (!hasPermission) {
                            PermissionCard(
                                modifier = Modifier.align(Alignment.Center),
                                isPermanentlyDenied = isPermanentlyDenied,
                                onRequest = {
                                    cameraPermissionRequested = true
                                    permissionLauncher.launch(permissionManager.cameraPermission)
                                },
                                onOpenSettings = { openAppSettings(context) },
                            )
                        } else {
                            InstructionCard(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = AppSpacing.scanContentTop),
                            )
                        }
                    }
                    ScanMode.MANUAL -> {
                        ManualEntryContent(
                            modifier = Modifier.align(Alignment.TopCenter),
                            state = state,
                            onTChanged = viewModel::updateManualT,
                            onIChanged = viewModel::updateManualI,
                            onFChanged = viewModel::updateManualF,
                            onSChanged = viewModel::updateManualS,
                            onSubmit = viewModel::submitManual,
                        )
                    }
                    ScanMode.PHOTO -> {
                        PhotoScanContent(
                            modifier = Modifier.align(Alignment.TopCenter),
                            onPickPhoto = {
                                val uri = createPhotoUri(context)
                                latestPhotoUri = uri
                                cameraLauncher.launch(uri)
                            },
                        )
                    }
                    ScanMode.URL -> {
                        UrlEntryContent(
                            modifier = Modifier.align(Alignment.TopCenter),
                            url = state.urlInput,
                            onUrlChanged = viewModel::updateUrlInput,
                            onSubmit = viewModel::submitUrl,
                        )
                    }
                }
            }

            if (state.isProcessing) {
                ScanLoadingOverlay()
            }

            ScanModeToolbar(
                current = state.scanMode,
                onSelected = { mode ->
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    viewModel.setScanMode(mode)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            )
        }
    }
}

package com.chelovecheck.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelovecheck.domain.model.AfterScanAction
import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.usecase.FetchReceiptByUrlUseCase
import com.chelovecheck.domain.usecase.FetchReceiptFromManualUseCase
import com.chelovecheck.domain.usecase.FetchReceiptFromPhotoUseCase
import com.chelovecheck.domain.usecase.ObserveAfterScanActionUseCase
import com.chelovecheck.domain.usecase.SaveReceiptUseCase
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.presentation.model.ScanMode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val fetchReceiptByUrlUseCase: FetchReceiptByUrlUseCase,
    private val fetchReceiptFromManualUseCase: FetchReceiptFromManualUseCase,
    private val fetchReceiptFromPhotoUseCase: FetchReceiptFromPhotoUseCase,
    private val saveReceiptUseCase: SaveReceiptUseCase,
    observeAfterScanActionUseCase: ObserveAfterScanActionUseCase,
    private val logger: AppLogger,
) : ViewModel() {
    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()
    val afterScanAction: StateFlow<AfterScanAction> = observeAfterScanActionUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AfterScanAction.OPEN_RECEIPT)

    fun onQrScanned(qr: String) {
        if (_state.value.isProcessing) return
        logger.debug("ScanViewModel", "QR scanned: $qr")

        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            delay(QR_LOCK_DELAY_MS)
            executeScanAction(tag = "QR fetch failed") {
                val receipt = fetchReceiptByUrlUseCase(qr)
                saveReceiptUseCase(receipt)
                receipt.fiscalSign
            }
        }
    }

    fun onPhotoSelected(bytes: ByteArray) {
        if (_state.value.isProcessing) return
        viewModelScope.launch {
            executeScanAction(tag = "Photo fetch failed") {
                val receipt = fetchReceiptFromPhotoUseCase(bytes)
                saveReceiptUseCase(receipt)
                receipt.fiscalSign
            }
        }
    }

    fun onPhotoReadFailed() {
        _state.update { it.copy(error = AppError.PhotoNotRecognized) }
    }

    fun submitManual() {
        if (_state.value.isProcessing) return
        val input = _state.value.manualInput

        viewModelScope.launch {
            executeScanAction(tag = "Manual fetch failed") {
                val receipt = fetchReceiptFromManualUseCase(
                    t = input.t,
                    i = input.i,
                    f = input.f,
                    s = input.s,
                )
                saveReceiptUseCase(receipt)
                receipt.fiscalSign
            }
        }
    }

    fun setScanMode(mode: ScanMode) {
        _state.update { it.copy(scanMode = mode, error = null) }
    }

    fun updateManualT(value: String) {
        _state.update { it.copy(manualInput = it.manualInput.copy(t = value)) }
    }

    fun updateManualI(value: String) {
        _state.update { it.copy(manualInput = it.manualInput.copy(i = value)) }
    }

    fun updateManualF(value: String) {
        _state.update { it.copy(manualInput = it.manualInput.copy(f = value)) }
    }

    fun updateManualS(value: String) {
        _state.update { it.copy(manualInput = it.manualInput.copy(s = value)) }
    }

    fun updateUrlInput(value: String) {
        _state.update { it.copy(urlInput = value) }
    }

    fun submitUrl() {
        if (_state.value.isProcessing) return
        val url = _state.value.urlInput
        viewModelScope.launch {
            executeScanAction(tag = "URL fetch failed") {
                val receipt = fetchReceiptByUrlUseCase(url)
                saveReceiptUseCase(receipt)
                receipt.fiscalSign
            }
        }
    }

    fun consumeSavedReceipt() {
        _state.update { it.copy(savedFiscalSign = null) }
    }

    fun consumeError() {
        _state.update { it.copy(error = null) }
    }

    fun onCameraError(error: Throwable) {
        _state.update { it.copy(error = AppError.CameraError(error)) }
    }

    private fun toAppError(error: Throwable): AppError {
        if (error is AppError.ReceiptAlreadyExists) {
            logger.debug("ScanViewModel", "Receipt already exists: ${error.fiscalSign}")
            return error
        }
        logger.debug("ScanViewModel", "Mapping error to AppError: ${error::class.simpleName}")
        if (error is AppError) return error
        if (error.hasCause<SSLHandshakeException>() || error.hasCause<CertPathValidatorException>() || error.hasCause<CertificateException>()) {
            return AppError.SslError(error)
        }
        if (error.hasCause<IOException>()) {
            return AppError.NetworkError(error)
        }
        return AppError.Unknown(error)
    }

    private fun logFailure(message: String, error: Throwable) {
        when (error) {
            is AppError.ReceiptAlreadyExists -> {
                logger.debug("ScanViewModel", "$message: receipt exists (${error.fiscalSign})")
            }
            is AppError -> {
                logger.error("ScanViewModel", message, error)
            }
            else -> {
                logger.error("ScanViewModel", message, error)
            }
        }
    }

    private suspend fun executeScanAction(
        tag: String,
        action: suspend () -> String,
    ) {
        _state.update { it.copy(isProcessing = true, error = null) }
        runCatching { action() }
            .onSuccess { fiscalSign ->
                _state.update { it.copy(isProcessing = false, savedFiscalSign = fiscalSign) }
            }
            .onFailure { error ->
                logFailure(tag, error)
                _state.update { it.copy(isProcessing = false, error = toAppError(error)) }
            }
    }

    private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is T) return true
            current = current.cause
        }
        return false
    }
}

private const val QR_LOCK_DELAY_MS = 1800L

data class ScanUiState(
    val isProcessing: Boolean = false,
    val savedFiscalSign: String? = null,
    val error: AppError? = null,
    val scanMode: ScanMode = ScanMode.QR,
    val manualInput: ManualReceiptInput = ManualReceiptInput(),
    val urlInput: String = "",
)

data class ManualReceiptInput(
    val t: String = "",
    val i: String = "",
    val f: String = "",
    val s: String = "",
)

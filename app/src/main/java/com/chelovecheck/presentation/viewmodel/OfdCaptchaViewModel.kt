package com.chelovecheck.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelovecheck.domain.model.AppError
import com.chelovecheck.domain.usecase.FetchReceiptWithCaptchaTokenUseCase
import com.chelovecheck.domain.usecase.SaveReceiptUseCase
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.presentation.navigation.OfdCaptchaNav
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OfdCaptchaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchReceiptWithCaptchaToken: FetchReceiptWithCaptchaTokenUseCase,
    private val saveReceiptUseCase: SaveReceiptUseCase,
    private val logger: AppLogger,
) : ViewModel() {
    private val encodedUrl: String = savedStateHandle.get<String>("encodedUrl").orEmpty()
    val receiptUrl: String = runCatching {
        OfdCaptchaNav.decodeUrlFromNav(encodedUrl)
    }.getOrElse { "" }

    private val _uiState = MutableStateFlow(OfdCaptchaUiState())
    val uiState: StateFlow<OfdCaptchaUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OfdCaptchaEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<OfdCaptchaEvent> = _events.asSharedFlow()

    fun onCaptchaToken(token: String) {
        if (token.isBlank() || receiptUrl.isBlank()) return
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val receipt = fetchReceiptWithCaptchaToken(receiptUrl, token)
                saveReceiptUseCase(receipt)
                receipt.fiscalSign
            }.onSuccess { fiscalSign ->
                _uiState.update { it.copy(isLoading = false) }
                _events.tryEmit(OfdCaptchaEvent.Saved(fiscalSign))
            }.onFailure { error ->
                logFailure(error)
                _uiState.update {
                    it.copy(isLoading = false, error = toAppError(error))
                }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun toAppError(error: Throwable): AppError {
        if (error is AppError.ReceiptAlreadyExists) return error
        if (error is AppError) return error
        if (error.hasCause<SSLHandshakeException>() || error.hasCause<CertPathValidatorException>() || error.hasCause<CertificateException>()) {
            return AppError.SslError(error)
        }
        if (error.hasCause<IOException>()) {
            return AppError.NetworkError(error)
        }
        return AppError.Unknown(error)
    }

    private fun logFailure(error: Throwable) {
        when (error) {
            is AppError.ReceiptAlreadyExists -> logger.debug("OfdCaptcha", "Receipt exists: ${error.fiscalSign}")
            is AppError -> logger.error("OfdCaptcha", "captcha flow failed", error)
            else -> logger.error("OfdCaptcha", "captcha flow failed", error)
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

data class OfdCaptchaUiState(
    val isLoading: Boolean = false,
    val error: AppError? = null,
)

sealed interface OfdCaptchaEvent {
    data class Saved(val fiscalSign: String) : OfdCaptchaEvent
}

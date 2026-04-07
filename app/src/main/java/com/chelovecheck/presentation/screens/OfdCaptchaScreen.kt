package com.chelovecheck.presentation.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelovecheck.R
import com.chelovecheck.domain.model.AppError
import com.chelovecheck.presentation.strings.appErrorMessage
import com.chelovecheck.presentation.viewmodel.OfdCaptchaEvent
import com.chelovecheck.presentation.viewmodel.OfdCaptchaViewModel
import kotlinx.coroutines.flow.collectLatest

private const val INJECT_HOOK = """
(function() {
  function bridge(token) {
    try { OfdCaptchaBridge.onCaptchaToken(String(token)); } catch(e) {}
  }
  var orig = window.ticketCaptchaVerified;
  window.ticketCaptchaVerified = function(token) {
    bridge(token);
    if (typeof orig === 'function') orig(token);
  };
})();
"""

private fun isAllowedTranstelecomHost(url: String): Boolean {
    val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return false
    return host == "ofd1.kz" ||
        host.endsWith(".ofd1.kz") ||
        host == "87.255.215.96"
}

private class OfdCaptchaJsBridge(
    private val onToken: (String) -> Unit,
) {
    @JavascriptInterface
    fun onCaptchaToken(token: String) {
        if (token.isBlank()) return
        Handler(Looper.getMainLooper()).post {
            onToken(token)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OfdCaptchaScreen(
    onClose: () -> Unit,
    onReceiptSaved: (String) -> Unit,
    viewModel: OfdCaptchaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val receiptUrl = viewModel.receiptUrl
    val invalidUrl = receiptUrl.isBlank() || !isAllowedTranstelecomHost(receiptUrl)

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is OfdCaptchaEvent.Saved -> onReceiptSaved(event.fiscalSign)
            }
        }
    }

    if (invalidUrl) {
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(stringResource(R.string.error_invalid_qr)) },
            confirmButton = {
                TextButton(onClick = onClose) { Text(stringResource(R.string.action_ok)) }
            },
        )
        return
    }

    val duplicate = state.error as? AppError.ReceiptAlreadyExists

    if (duplicate != null) {
        AlertDialog(
            onDismissRequest = viewModel::consumeError,
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(appErrorMessage(duplicate)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.consumeError()
                    onReceiptSaved(duplicate.fiscalSign)
                }) {
                    Text(stringResource(R.string.action_view_receipt))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::consumeError) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
    } else if (state.error != null) {
        val err = state.error!!
        AlertDialog(
            onDismissRequest = viewModel::consumeError,
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(appErrorMessage(err)) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeError) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ofd_captcha_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.evaluateJavascript(INJECT_HOOK, null)
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        addJavascriptInterface(
                            OfdCaptchaJsBridge { viewModel.onCaptchaToken(it) },
                            "OfdCaptchaBridge",
                        )
                        loadUrl(receiptUrl)
                    }
                },
                onRelease = { view ->
                    view.destroy()
                },
            )
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

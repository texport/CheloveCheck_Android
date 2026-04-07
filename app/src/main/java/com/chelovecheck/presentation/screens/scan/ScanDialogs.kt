package com.chelovecheck.presentation.screens.scan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.chelovecheck.R
import com.chelovecheck.domain.model.AppError
import com.chelovecheck.presentation.strings.appErrorMessage

@Composable
internal fun ScanErrorDialog(
    error: AppError,
    onDismiss: () -> Unit,
    onOpenReceipt: (String) -> Unit,
    onOpenSupport: () -> Unit,
    onVerifyOfdInApp: (String) -> Unit = {},
    onOpenReceiptUrlInBrowser: (String) -> Unit = {},
) {
    when (error) {
        is AppError.ReceiptAlreadyExists -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenReceipt(error.fiscalSign)
                        },
                    ) {
                        Text(stringResource(R.string.action_view_receipt))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
                },
                title = { Text(stringResource(R.string.error_title)) },
                text = { Text(appErrorMessage(error)) },
            )
        }
        is AppError.ReceiptRequiresOfdVerification -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    Column(Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = {
                                onVerifyOfdInApp(error.receiptUrl)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.action_verify_in_app))
                        }
                        TextButton(
                            onClick = {
                                onOpenReceiptUrlInBrowser(error.receiptUrl)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.action_open_in_browser))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                },
                title = { Text(stringResource(R.string.error_title)) },
                text = { Text(appErrorMessage(error)) },
            )
        }
        else -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
                },
                dismissButton = when {
                    shouldShowSupport(error) -> {
                        { TextButton(onClick = onOpenSupport) { Text(stringResource(R.string.action_support)) } }
                    }
                    else -> null
                },
                title = { Text(stringResource(R.string.error_title)) },
                text = { Text(appErrorMessage(error)) },
            )
        }
    }
}

@Composable
internal fun ScanSavedDialog(
    fiscalSign: String,
    onDismiss: () -> Unit,
    onOpenReceipt: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scan_saved_message)) },
        confirmButton = {
            TextButton(onClick = { onOpenReceipt(fiscalSign) }) {
                Text(stringResource(R.string.action_view_receipt))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        },
    )
}

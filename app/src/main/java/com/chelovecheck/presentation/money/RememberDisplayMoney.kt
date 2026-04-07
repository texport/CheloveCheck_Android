package com.chelovecheck.presentation.money

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelovecheck.presentation.strings.formatMoney
import com.chelovecheck.presentation.viewmodel.ChecksViewModel
import com.chelovecheck.presentation.viewmodel.ProductViewModel
import com.chelovecheck.presentation.viewmodel.ReceiptViewModel

@Composable
fun rememberChecksDisplayMoney(
    kztAmount: Double,
    atEpochMillis: Long,
    viewModel: ChecksViewModel,
): String {
    val currency by viewModel.displayCurrency.collectAsStateWithLifecycle()
    var text by remember(kztAmount, currency) { mutableStateOf<String?>(null) }
    LaunchedEffect(kztAmount, atEpochMillis, currency) {
        text = viewModel.formatKztForDisplay(kztAmount, atEpochMillis)
    }
    return text ?: formatMoney(kztAmount)
}

@Composable
fun rememberReceiptDisplayMoney(kztAmount: Double, viewModel: ReceiptViewModel): String {
    val currency by viewModel.displayCurrency.collectAsStateWithLifecycle()
    var text by remember(kztAmount, currency) { mutableStateOf<String?>(null) }
    LaunchedEffect(kztAmount, currency) {
        text = viewModel.formatKztForDisplay(kztAmount)
    }
    return text ?: formatMoney(kztAmount)
}

@Composable
fun rememberProductDisplayMoney(kztAmount: Double, viewModel: ProductViewModel): String {
    val currency by viewModel.displayCurrency.collectAsStateWithLifecycle()
    var text by remember(kztAmount, currency) { mutableStateOf<String?>(null) }
    LaunchedEffect(kztAmount, currency) {
        text = viewModel.formatKztForDisplay(kztAmount)
    }
    return text ?: formatMoney(kztAmount)
}

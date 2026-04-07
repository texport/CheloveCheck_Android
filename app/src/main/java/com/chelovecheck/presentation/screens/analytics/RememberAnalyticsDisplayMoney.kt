package com.chelovecheck.presentation.screens.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelovecheck.presentation.strings.formatMoney
import com.chelovecheck.presentation.viewmodel.AnalyticsViewModel

@Composable
internal fun rememberAnalyticsDisplayMoney(kztAmount: Double, viewModel: AnalyticsViewModel): String {
    val currency by viewModel.displayCurrency.collectAsStateWithLifecycle()
    var text by remember(kztAmount, currency) { mutableStateOf<String?>(null) }
    LaunchedEffect(kztAmount, currency) {
        text = viewModel.formatKztForDisplay(kztAmount)
    }
    return text ?: formatMoney(kztAmount)
}

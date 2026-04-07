package com.chelovecheck.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelovecheck.domain.model.DisplayCurrency
import com.chelovecheck.domain.model.ItemPurchaseRow
import com.chelovecheck.domain.usecase.ConvertAmountUseCase
import com.chelovecheck.domain.usecase.GetItemPurchaseHistoryUseCase
import com.chelovecheck.domain.usecase.ObserveDisplayCurrencyUseCase
import com.chelovecheck.presentation.money.DisplayMoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val getItemPurchaseHistory: GetItemPurchaseHistoryUseCase,
    observeDisplayCurrencyUseCase: ObserveDisplayCurrencyUseCase,
    private val convertAmountUseCase: ConvertAmountUseCase,
) : ViewModel() {
    private val _rows = MutableStateFlow<List<ItemPurchaseRow>>(emptyList())
    val rows: StateFlow<List<ItemPurchaseRow>> = _rows.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val displayCurrency: StateFlow<DisplayCurrency> = observeDisplayCurrencyUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, DisplayCurrency.KZT)

    fun load(normalizedItemKey: String) {
        viewModelScope.launch {
            _loading.value = true
            _rows.value = getItemPurchaseHistory(normalizedItemKey)
            _loading.value = false
        }
    }

    suspend fun formatKztForDisplay(kztAmount: Double): String {
        val c = displayCurrency.value
        val v = convertAmountUseCase(kztAmount, c)
        return DisplayMoneyFormatter.format(v, c)
    }
}

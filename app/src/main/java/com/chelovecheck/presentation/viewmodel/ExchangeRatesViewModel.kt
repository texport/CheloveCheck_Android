package com.chelovecheck.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelovecheck.domain.model.ExchangeRatesSnapshot
import com.chelovecheck.domain.usecase.RefreshExchangeRatesUseCase
import com.chelovecheck.domain.repository.ExchangeRateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ExchangeRatesViewModel @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val refreshExchangeRatesUseCase: RefreshExchangeRatesUseCase,
) : ViewModel() {
    val snapshot: StateFlow<ExchangeRatesSnapshot> =
        exchangeRateRepository.observeExchangeRatesSnapshot()
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                ExchangeRatesSnapshot(null, emptyMap()),
            )

    fun refresh() {
        viewModelScope.launch {
            refreshExchangeRatesUseCase()
        }
    }
}

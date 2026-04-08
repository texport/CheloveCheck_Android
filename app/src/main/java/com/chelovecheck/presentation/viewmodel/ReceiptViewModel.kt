package com.chelovecheck.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelovecheck.domain.model.DisplayCurrency
import com.chelovecheck.domain.model.Item
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.usecase.ConvertAmountUseCase
import com.chelovecheck.domain.usecase.DeleteReceiptUseCase
import com.chelovecheck.domain.usecase.GetReceiptUseCase
import com.chelovecheck.domain.usecase.ObserveDisplayCurrencyUseCase
import com.chelovecheck.domain.usecase.RefetchReceiptFromUrlUseCase
import com.chelovecheck.domain.usecase.TranslateReceiptItemsUseCase
import com.chelovecheck.domain.usecase.UpdateReceiptAddressUseCase
import com.chelovecheck.domain.usecase.UpdateReceiptItemsUseCase
import com.chelovecheck.presentation.money.DisplayMoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val getReceiptUseCase: GetReceiptUseCase,
    private val refetchReceiptFromUrlUseCase: RefetchReceiptFromUrlUseCase,
    private val translateReceiptItemsUseCase: TranslateReceiptItemsUseCase,
    private val updateReceiptAddressUseCase: UpdateReceiptAddressUseCase,
    private val updateReceiptItemsUseCase: UpdateReceiptItemsUseCase,
    private val deleteReceiptUseCase: DeleteReceiptUseCase,
    observeDisplayCurrencyUseCase: ObserveDisplayCurrencyUseCase,
    private val convertAmountUseCase: ConvertAmountUseCase,
    private val logger: AppLogger,
) : ViewModel() {
    private val _state = MutableStateFlow(ReceiptUiState())
    val state: StateFlow<ReceiptUiState> = _state.asStateFlow()

    val displayCurrency: StateFlow<DisplayCurrency> = observeDisplayCurrencyUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, DisplayCurrency.KZT)

    fun refetchFromOfd(fiscalSign: String) {
        viewModelScope.launch {
            _state.update { it.copy(isRefetching = true, refetchError = null) }
            runCatching { refetchReceiptFromUrlUseCase(fiscalSign) }
                .onSuccess {
                    _state.update { it.copy(isRefetching = false) }
                    loadReceipt(fiscalSign)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isRefetching = false, refetchError = e.message)
                    }
                }
        }
    }

    fun dismissRefetchError() {
        _state.update { it.copy(refetchError = null) }
    }

    fun loadReceipt(fiscalSign: String) {
        if (fiscalSign.isBlank()) return
        _state.update { it.copy(isLoading = true, loadingStage = ReceiptLoadingStage.LOADING_RECEIPT) }

        viewModelScope.launch {
            val sourceReceipt = getReceiptUseCase(fiscalSign)
            val receipt = maybeRetranslateAndPersist(sourceReceipt)
            val items = receipt?.items?.mapIndexed { index, item ->
                val stableId = if (item.id != 0L) item.id else -(index + 1L)
                ReceiptItemUi(id = stableId, item = item)
            }.orEmpty()
            _state.update {
                it.copy(
                    isLoading = false,
                    loadingStage = null,
                    receipt = receipt,
                    items = items,
                    selectedIds = emptySet(),
                    isSelectionMode = false,
                    isDeleted = false,
                )
            }
        }
    }

    private suspend fun maybeRetranslateAndPersist(receipt: Receipt?): Receipt? {
        if (receipt == null) return null
        _state.update { it.copy(loadingStage = ReceiptLoadingStage.TRANSLATING_ITEMS) }
        val translated = translateReceiptItemsUseCase(receipt)
        val changedCount = receipt.items.zip(translated.items).count { (before, after) ->
            before.name != after.name
        }
        if (changedCount == 0) {
            logger.debug("ReceiptViewModel", "Retranslate skipped: no changes for ${receipt.fiscalSign}")
            return receipt
        }
        logger.debug(
            "ReceiptViewModel",
            "Retranslate applied for ${receipt.fiscalSign}: changedItems=$changedCount/${receipt.items.size}",
        )
        updateReceiptItemsUseCase(receipt.fiscalSign, translated.items)
        return translated
    }

    fun toggleSelection(id: Long) {
        _state.update { state ->
            val selected = state.selectedIds.toMutableSet()
            if (!selected.add(id)) selected.remove(id)
            state.copy(
                selectedIds = selected,
                isSelectionMode = selected.isNotEmpty(),
            )
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun deleteSelectedItems() {
        val receipt = _state.value.receipt ?: return
        val remaining = _state.value.items.filterNot { _state.value.selectedIds.contains(it.id) }
        if (remaining.isEmpty()) {
            viewModelScope.launch {
                deleteReceiptUseCase(receipt.fiscalSign)
                _state.update { it.copy(receipt = null, items = emptyList(), isDeleted = true) }
            }
            return
        }

        val updatedTotal = remaining.sumOf { it.item.sum }
        _state.update {
            it.copy(
                items = remaining,
                selectedIds = emptySet(),
                isSelectionMode = false,
                receipt = it.receipt?.copy(totalSum = updatedTotal),
            )
        }
        persistItems()
    }

    fun deleteItem(id: Long) {
        val receipt = _state.value.receipt ?: return
        val remaining = _state.value.items.filterNot { it.id == id }
        if (remaining.isEmpty()) {
            viewModelScope.launch {
                deleteReceiptUseCase(receipt.fiscalSign)
                _state.update { it.copy(receipt = null, items = emptyList(), isDeleted = true) }
            }
            return
        }

        val updatedTotal = remaining.sumOf { it.item.sum }
        _state.update { state ->
            val selected = state.selectedIds - id
            state.copy(
                items = remaining,
                selectedIds = selected,
                isSelectionMode = selected.isNotEmpty(),
                receipt = state.receipt?.copy(totalSum = updatedTotal),
            )
        }
        persistItems()
    }

    fun updateAddress(address: String) {
        val receipt = _state.value.receipt ?: return
        viewModelScope.launch {
            updateReceiptAddressUseCase(receipt.fiscalSign, address)
            _state.update { state ->
                state.copy(receipt = state.receipt?.copy(companyAddress = address))
            }
        }
    }

    private fun persistItems() {
        val receipt = _state.value.receipt ?: return
        val items = _state.value.items.map { it.item }
        viewModelScope.launch {
            updateReceiptItemsUseCase(receipt.fiscalSign, items)
        }
    }

    suspend fun formatKztForDisplay(kztAmount: Double): String {
        val c = displayCurrency.value
        val v = convertAmountUseCase(kztAmount, c)
        return DisplayMoneyFormatter.format(v, c)
    }
}

data class ReceiptUiState(
    val receipt: Receipt? = null,
    val isLoading: Boolean = false,
    val isRefetching: Boolean = false,
    val refetchError: String? = null,
    val items: List<ReceiptItemUi> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isDeleted: Boolean = false,
    val loadingStage: ReceiptLoadingStage? = null,
)

enum class ReceiptLoadingStage {
    LOADING_RECEIPT,
    TRANSLATING_ITEMS,
}

data class ReceiptItemUi(
    val id: Long,
    val item: Item,
)

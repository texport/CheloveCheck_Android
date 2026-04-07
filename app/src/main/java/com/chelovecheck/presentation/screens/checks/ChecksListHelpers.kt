package com.chelovecheck.presentation.screens.checks

import androidx.compose.foundation.lazy.LazyListState
import com.chelovecheck.domain.model.ReceiptFilter
import com.chelovecheck.presentation.viewmodel.ChecksUiState

internal fun hasActiveQuery(state: ChecksUiState): Boolean {
    if (state.searchQuery.isNotBlank()) return true
    return state.activeFilter != ReceiptFilter.All
}

internal fun shouldLoadMore(listState: LazyListState, totalItems: Int): Boolean {
    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    return lastVisible >= totalItems - 5
}

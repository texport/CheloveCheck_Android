package com.chelovecheck.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.model.DisplayCurrency
import com.chelovecheck.domain.model.MapProvider
import com.chelovecheck.domain.model.ReceiptFilter
import com.chelovecheck.domain.model.ReceiptListCursor
import com.chelovecheck.domain.model.ReceiptListItem
import com.chelovecheck.domain.model.ReceiptListSortOrder
import com.chelovecheck.domain.model.ReceiptGroupMode
import com.chelovecheck.domain.model.ReceiptOwnershipFilter
import com.chelovecheck.domain.repository.SettingsRepository
import com.chelovecheck.domain.usecase.ConvertAmountUseCase
import com.chelovecheck.domain.usecase.DeleteReceiptUseCase
import com.chelovecheck.domain.usecase.GetReceiptListPageUseCase
import com.chelovecheck.domain.usecase.ObserveDisplayCurrencyUseCase
import com.chelovecheck.domain.usecase.ObserveMapProviderUseCase
import com.chelovecheck.domain.usecase.ToggleFavoriteReceiptUseCase
import com.chelovecheck.domain.usecase.TogglePinnedReceiptUseCase
import com.chelovecheck.presentation.money.DisplayMoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class ChecksViewModel @Inject constructor(
    private val getReceiptListPageUseCase: GetReceiptListPageUseCase,
    private val deleteReceiptUseCase: DeleteReceiptUseCase,
    private val toggleFavoriteReceiptUseCase: ToggleFavoriteReceiptUseCase,
    private val togglePinnedReceiptUseCase: TogglePinnedReceiptUseCase,
    observeMapProviderUseCase: ObserveMapProviderUseCase,
    observeDisplayCurrencyUseCase: ObserveDisplayCurrencyUseCase,
    private val convertAmountUseCase: ConvertAmountUseCase,
    private val settingsRepository: SettingsRepository,
    private val logger: AppLogger,
) : ViewModel() {
    private val _state = MutableStateFlow(ChecksUiState(isLoading = true))
    val state: StateFlow<ChecksUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<ChecksUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ChecksUiEvent> = _events.asSharedFlow()
    val mapProvider: StateFlow<MapProvider> =
        observeMapProviderUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, MapProvider.GOOGLE)

    val displayCurrency: StateFlow<DisplayCurrency> = observeDisplayCurrencyUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, DisplayCurrency.KZT)

    val sortOrder: StateFlow<ReceiptListSortOrder> =
        settingsRepository.receiptListSortOrder
            .stateIn(viewModelScope, SharingStarted.Eagerly, ReceiptListSortOrder.DEFAULT)

    val groupMode: StateFlow<ReceiptGroupMode> =
        settingsRepository.receiptGroupMode
            .stateIn(viewModelScope, SharingStarted.Eagerly, ReceiptGroupMode.NONE)

    private val limit = 50
    private var hasMore = true
    private var searchJob: Job? = null
    private var listFetchJob: Job? = null
    private val fetchMutex = Mutex()
    private var skipNextResumeRefresh = true

    /** Max rows kept in memory to avoid OOM when many pages are loaded. */
    private val maxRowsInMemory = 500

    init {
        refresh(RefreshMode.Full)
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            refresh(RefreshMode.Full)
        }
    }

    fun onFilterSelected(filter: ReceiptFilter) {
        _state.update { it.copy(activeFilter = filter) }
        refresh(RefreshMode.Full)
    }

    fun onOwnershipFilterSelected(ownership: ReceiptOwnershipFilter) {
        _state.update { it.copy(ownershipFilter = ownership) }
        refresh(RefreshMode.Full)
    }

    fun setSortOrder(order: ReceiptListSortOrder) {
        if (order == sortOrder.value) return
        viewModelScope.launch {
            settingsRepository.setReceiptListSortOrder(order)
            refresh(RefreshMode.Full)
        }
    }

    fun setGroupMode(mode: ReceiptGroupMode) {
        if (mode == groupMode.value) return
        viewModelScope.launch {
            settingsRepository.setReceiptGroupMode(mode)
        }
    }

    fun toggleFavorite(fiscalSign: String, favorite: Boolean) {
        viewModelScope.launch {
            runCatching { toggleFavoriteReceiptUseCase(fiscalSign, favorite) }
                .onSuccess {
                    _state.update { st ->
                        st.copy(
                            receipts = st.receipts.map { row ->
                                if (row.summary.fiscalSign != fiscalSign) row
                                else row.copy(
                                    summary = row.summary.copy(isFavorite = favorite),
                                )
                            },
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(errorMessage = e.message) } }
        }
    }

    fun togglePinned(fiscalSign: String, pinned: Boolean) {
        viewModelScope.launch {
            runCatching { togglePinnedReceiptUseCase(fiscalSign, pinned) }
                .onSuccess {
                    refresh(RefreshMode.Full)
                    _events.tryEmit(ChecksUiEvent.ScrollToTop)
                }
                .onFailure { e -> _state.update { it.copy(errorMessage = e.message) } }
        }
    }

    /**
     * Skips the first [androidx.lifecycle.Lifecycle.Event.ON_RESUME] after creation to avoid double-fetch with [init].
     */
    fun onScreenResumed() {
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false
            return
        }
        refresh(RefreshMode.SoftResume)
    }

    fun loadNextPage() {
        if (!hasMore) return
        val s = _state.value
        if (s.isLoading || s.isPaging || s.receipts.isEmpty()) return
        viewModelScope.launch {
            fetchMutex.withLock {
                if (!hasMore) return@withLock
                val cur = _state.value
                if (cur.isLoading || cur.isPaging || cur.receipts.isEmpty()) return@withLock
                _state.update { it.copy(isPaging = true, errorMessage = null) }
                val last = cur.receipts.last()
                val cursor = ReceiptListCursor(
                    dateTimeEpochMillis = last.summary.dateTimeEpochMillis,
                    fiscalSign = last.summary.fiscalSign,
                    isPinned = last.summary.isPinned,
                    isFavorite = last.summary.isFavorite,
                    totalSum = last.summary.totalSum,
                    companyName = last.summary.companyName,
                )
                val result = runCatching {
                    getReceiptListPageUseCase(
                        filter = cur.activeFilter,
                        searchQuery = cur.searchQuery.takeIf { it.isNotBlank() },
                        cursor = cursor,
                        limit = limit,
                        ownership = cur.ownershipFilter,
                        sortOrder = sortOrder.value,
                    )
                }
                result.onSuccess { newItems ->
                    hasMore = newItems.size == limit
                    _state.update { state ->
                        val merged = (state.receipts + newItems).distinctBy { it.summary.fiscalSign }
                        val capped =
                            if (merged.size <= maxRowsInMemory) merged else merged.takeLast(maxRowsInMemory)
                        state.copy(
                            receipts = capped,
                            isPaging = false,
                            hasMore = hasMore,
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isPaging = false,
                            errorMessage = error.message,
                        )
                    }
                }
            }
        }
    }

    fun refresh(mode: RefreshMode = RefreshMode.Full) {
        listFetchJob?.cancel()
        hasMore = true
        listFetchJob = viewModelScope.launch {
            fetchMutex.withLock {
                when (mode) {
                    RefreshMode.Full -> {
                        _state.update {
                            it.copy(
                                receipts = emptyList(),
                                isLoading = true,
                                isRefreshing = false,
                                errorMessage = null,
                                hasMore = true,
                            )
                        }
                        fetchFirstPageLocked()
                    }
                    RefreshMode.SoftResume -> {
                        val hadReceipts = _state.value.receipts.isNotEmpty()
                        _state.update {
                            it.copy(
                                isRefreshing = hadReceipts,
                                isLoading = !hadReceipts,
                                errorMessage = null,
                            )
                        }
                        fetchFirstPageLocked(keepStaleOnFailure = hadReceipts)
                    }
                }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private suspend fun fetchFirstPageLocked(keepStaleOnFailure: Boolean = false) {
        val snapshot = _state.value
        val result = runCatching {
            getReceiptListPageUseCase(
                filter = snapshot.activeFilter,
                searchQuery = snapshot.searchQuery.takeIf { it.isNotBlank() },
                cursor = null,
                limit = limit,
                ownership = snapshot.ownershipFilter,
                sortOrder = sortOrder.value,
            )
        }
        result.onSuccess { items ->
            hasMore = items.size == limit
            _state.update { state ->
                state.copy(
                    receipts = items,
                    isLoading = false,
                    isRefreshing = false,
                    hasMore = hasMore,
                    errorMessage = null,
                )
            }
        }.onFailure { error ->
            _state.update { s ->
                s.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = error.message,
                    receipts = if (keepStaleOnFailure) s.receipts else emptyList(),
                )
            }
        }
    }

    fun deleteReceipt(fiscalSign: String) {
        viewModelScope.launch {
            val result = runCatching { deleteReceiptUseCase(fiscalSign) }
            result.onSuccess {
                _state.update { state ->
                    state.copy(
                        receipts = state.receipts.filterNot { it.summary.fiscalSign == fiscalSign },
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    fun logMapOpen(provider: MapProvider, source: String, query: String) {
        logger.debug(
            tag = "MapIntent",
            message = "open map: provider=$provider source=$source query=$query",
        )
    }

    fun logMapOpenFailed(provider: MapProvider, source: String, query: String) {
        logger.debug(
            tag = "MapIntent",
            message = "open map failed: provider=$provider source=$source query=$query",
        )
    }

    fun logMapOpenResult(
        provider: MapProvider,
        source: String,
        query: String,
        primaryUri: String,
        fallbackUri: String,
        success: Boolean,
        usedFallback: Boolean,
        failureReason: String?,
        usedPackage: String?,
    ) {
        logger.debug(
            tag = "MapIntent",
            message = "result: provider=$provider source=$source query=$query " +
                "primary=$primaryUri fallback=$fallbackUri success=$success usedFallback=$usedFallback " +
                "reason=${failureReason ?: "none"} package=${usedPackage ?: "none"}",
        )
    }

    fun logSwipe(message: String) {
        logger.debug(tag = "SwipeCard", message = message)
    }

    suspend fun formatKztForDisplay(kztAmount: Double, atEpochMillis: Long? = null): String {
        val c = displayCurrency.value
        val v = convertAmountUseCase(kztAmount, c, atEpochMillis)
        return DisplayMoneyFormatter.format(v, c)
    }
}

enum class RefreshMode {
    Full,
    SoftResume,
}

data class ChecksUiState(
    val receipts: List<ReceiptListItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isPaging: Boolean = false,
    val hasMore: Boolean = true,
    val activeFilter: ReceiptFilter = ReceiptFilter.All,
    val ownershipFilter: ReceiptOwnershipFilter = ReceiptOwnershipFilter.All,
    val searchQuery: String = "",
    val errorMessage: String? = null,
)

sealed interface ChecksUiEvent {
    data object ScrollToTop : ChecksUiEvent
}

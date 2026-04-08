package com.chelovecheck.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelovecheck.domain.model.AnalyticsLoadStage
import com.chelovecheck.domain.model.AnalyticsSummary
import com.chelovecheck.domain.model.AppLanguage
import com.chelovecheck.domain.model.CoicopCategory
import com.chelovecheck.domain.model.DisplayCurrency
import com.chelovecheck.domain.model.ItemTranslationLanguage
import com.chelovecheck.domain.model.PendingCategoryItem
import com.chelovecheck.domain.repository.ReceiptsChangeTracker
import com.chelovecheck.data.analytics.AnalyticsProgressStore
import com.chelovecheck.data.analytics.AnalyticsCacheStore
import com.chelovecheck.domain.analytics.RetailDisplayGroupResolver
import com.chelovecheck.domain.repository.CategoryRepository
import com.chelovecheck.domain.repository.RetailDisplayGroupsRepository
import com.chelovecheck.domain.usecase.ObserveLanguageUseCase
import com.chelovecheck.domain.usecase.ObserveItemTranslationLanguageUseCase
import com.chelovecheck.domain.usecase.ObserveAnalyticsPendingPromptUseCase
import com.chelovecheck.domain.usecase.ObserveDisplayCurrencyUseCase
import com.chelovecheck.domain.usecase.ConvertAmountUseCase
import com.chelovecheck.domain.usecase.SaveCategoryOverrideUseCase
import com.chelovecheck.domain.utils.ItemNameNormalizer
import com.chelovecheck.data.analytics.AnalyticsRunStore
import com.chelovecheck.presentation.model.AnalyticsPeriod
import com.chelovecheck.presentation.money.DisplayMoneyFormatter
import com.chelovecheck.presentation.service.AnalyticsServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val observeLanguageUseCase: ObserveLanguageUseCase,
    private val categoryRepository: CategoryRepository,
    private val retailDisplayGroupsRepository: RetailDisplayGroupsRepository,
    private val saveCategoryOverrideUseCase: SaveCategoryOverrideUseCase,
    private val receiptsChangeTracker: ReceiptsChangeTracker,
    private val progressStore: AnalyticsProgressStore,
    private val cacheStore: AnalyticsCacheStore,
    private val observeAnalyticsPendingPromptUseCase: ObserveAnalyticsPendingPromptUseCase,
    observeItemTranslationLanguageUseCase: ObserveItemTranslationLanguageUseCase,
    observeDisplayCurrencyUseCase: ObserveDisplayCurrencyUseCase,
    private val convertAmountUseCase: ConvertAmountUseCase,
    private val analyticsRunStore: AnalyticsRunStore,
    private val analyticsServiceController: AnalyticsServiceController,
) : ViewModel() {
    private val _state = MutableStateFlow(AnalyticsUiState())
    val state: StateFlow<AnalyticsUiState> = _state.asStateFlow()
    private val categories = mutableMapOf<String, CoicopCategory>()
    private val dismissedPending = mutableSetOf<String>()
    private var lastChangeToken: Long = 0L
    private var pendingPromptShown = false
    private var pendingPromptEnabled = true
    private var lastLoadedPeriod: AnalyticsPeriod? = null
    private var activeRequest: AnalyticsRequest? = null
    private var suppressLoadingOverlay: Boolean = false
    private var debouncedRefreshJob: Job? = null
    val displayCurrency: StateFlow<DisplayCurrency> = observeDisplayCurrencyUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, DisplayCurrency.KZT)
    val itemTranslationLanguage: StateFlow<ItemTranslationLanguage> = observeItemTranslationLanguageUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ItemTranslationLanguage.OFD_SOURCE)

    init {
        _state.update {
            it.copy(pickerDisplayGroupIds = retailDisplayGroupsRepository.pickerDisplayGroupIds())
        }
        viewModelScope.launch {
            categories.clear()
            categories.putAll(categoryRepository.getAllCategories().associateBy { it.id })
            retailDisplayGroupsRepository.getConfig()
            _state.update {
                it.copy(pickerDisplayGroupIds = retailDisplayGroupsRepository.pickerDisplayGroupIds())
            }
        }
        viewModelScope.launch {
            observeLanguageUseCase().collectLatest { language ->
                _state.update { it.copy(language = language) }
            }
        }
        viewModelScope.launch {
            progressStore.stage.collectLatest { stage ->
                _state.update { it.copy(loadingStage = stage) }
            }
        }
        viewModelScope.launch {
            analyticsRunStore.state.collectLatest { runState ->
                val request = activeRequest
                val periodMatches = request != null && runState.period == request.period && runState.token == request.token
                if (runState.summary != null && periodMatches) {
                    suppressLoadingOverlay = false
                    cacheStore.put(request.period, request.token, runState.summary)
                    applySummary(runState.summary, showPrompt = true)
                    activeRequest = null
                } else if (!runState.isRunning && periodMatches) {
                    suppressLoadingOverlay = false
                    if (_state.value.isLoading) {
                        progressStore.clear()
                        _state.update { it.copy(isLoading = false) }
                    }
                    activeRequest = null
                } else if (runState.isRunning && periodMatches && !suppressLoadingOverlay && !_state.value.isLoading) {
                    _state.update { it.copy(isLoading = true) }
                }
            }
        }
        viewModelScope.launch {
            observeAnalyticsPendingPromptUseCase().collectLatest { enabled ->
                pendingPromptEnabled = enabled
                if (!enabled) {
                    _state.update { it.copy(pendingPromptVisible = false) }
                }
            }
        }
        viewModelScope.launch {
            receiptsChangeTracker.changes
                .debounce(300)
                .collectLatest { token ->
                if (token != lastChangeToken) {
                    lastChangeToken = token
                    refresh(force = true)
                }
            }
        }
        refresh()
    }

    fun setPeriod(period: AnalyticsPeriod) {
        _state.update { it.copy(period = period) }
        refresh()
    }

    fun refresh(force: Boolean = false, silent: Boolean = false) {
        if (_state.value.isLoading) return
        val period = _state.value.period
        val currentToken = receiptsChangeTracker.changes.value
        if (!force) {
            cacheStore.get(period, currentToken)?.let { cached ->
                progressStore.clear()
                applySummary(cached, showPrompt = false)
                return
            }
            if (currentToken == lastChangeToken &&
                _state.value.summary != null &&
                lastLoadedPeriod == period
            ) {
                progressStore.clear()
                return
            }
        }
        lastChangeToken = currentToken
        suppressLoadingOverlay = silent
        if (!silent) {
            _state.update { it.copy(isLoading = true) }
        }
        val (from, to) = resolveRange(period)
        activeRequest = AnalyticsRequest(period, currentToken)
        analyticsServiceController.start(period, from, to, currentToken)
    }

    fun pendingItemKey(item: PendingCategoryItem): String = pendingKey(item)

    fun confirmPendingCategory(item: PendingCategoryItem, categoryId: String) {
        viewModelScope.launch {
            val key = pendingKey(item)
            saveCategoryOverrideUseCase(item.sourceItemName, categoryId)
            dismissedPending.remove(key)
            _state.update { st ->
                val s = st.summary ?: return@update st
                val filtered = s.pendingItems.filterNot { pendingKey(it) == key }
                st.copy(
                    summary = s.copy(pendingItems = filtered),
                    pendingItems = filtered.filterNot { dismissedPending.contains(pendingKey(it)) },
                    pendingItemsAll = filtered,
                )
            }
            scheduleDebouncedAnalyticsRefresh()
        }
    }

    private fun scheduleDebouncedAnalyticsRefresh() {
        debouncedRefreshJob?.cancel()
        debouncedRefreshJob = viewModelScope.launch {
            delay(2000)
            refresh(force = true, silent = true)
        }
    }

    fun requestCategoryChange(sourceItemName: String, displayItemName: String) {
        _state.update {
            it.copy(
                categoryChangeItem = PendingCategoryItem(
                    sourceItemName = sourceItemName,
                    displayItemName = displayItemName,
                    candidates = emptyList(),
                ),
            )
        }
    }

    fun confirmCategoryChange(sourceItemName: String, categoryId: String) {
        viewModelScope.launch {
            _state.update { it.copy(resolvingItemName = sourceItemName) }
            saveCategoryOverrideUseCase(sourceItemName, categoryId)
            dismissedPending.remove(
                pendingKey(
                    PendingCategoryItem(
                        sourceItemName = sourceItemName,
                        displayItemName = sourceItemName,
                        candidates = emptyList(),
                    ),
                ),
            )
            _state.update { it.copy(categoryChangeItem = null, resolvingItemName = null) }
            scheduleDebouncedAnalyticsRefresh()
        }
    }

    fun dismissCategoryChange() {
        _state.update { it.copy(categoryChangeItem = null, resolvingItemName = null) }
    }

    fun dismissPendingCategory(item: PendingCategoryItem) {
        dismissedPending.add(pendingKey(item))
        _state.update {
            it.copy(pendingItems = it.pendingItems.filterNot { pending -> pendingKey(pending) == pendingKey(item) })
        }
    }

    fun dismissPendingPrompt() {
        _state.update { it.copy(pendingPromptVisible = false) }
    }

    /** Maps a predicted COICOP rollup id to the retail display-group label shown in pending previews. */
    fun rollupPreviewLabel(rollupId: String): String {
        val displayId = retailDisplayGroupsRepository.displayGroupIdForCoicopRollup(rollupId)
        return categoryLabel(displayId)
    }

    fun categoryLabel(categoryId: String): String {
        val resolvedTag = resolveAnalyticsDisplayLanguageTag()
        retailDisplayGroupsRepository.labelForCategoryOrDisplayId(categoryId, resolvedTag)?.let { return it }
        val category = categories[categoryId] ?: return categoryId
        return resolveLabel(category, resolvedTag)
    }

    private fun resolveAnalyticsDisplayLanguageTag(): String {
        val itemLang = itemTranslationLanguage.value
        return when (itemLang) {
            ItemTranslationLanguage.ENGLISH -> AppLanguage.ENGLISH.tag
            ItemTranslationLanguage.RUSSIAN -> AppLanguage.RUSSIAN.tag
            ItemTranslationLanguage.KAZAKH -> AppLanguage.KAZAKH.tag
            ItemTranslationLanguage.SYSTEM,
            ItemTranslationLanguage.OFD_SOURCE,
            -> {
                val appLanguage = _state.value.language
                when (appLanguage) {
                    AppLanguage.RUSSIAN -> AppLanguage.RUSSIAN.tag
                    AppLanguage.KAZAKH -> AppLanguage.KAZAKH.tag
                    AppLanguage.ENGLISH -> AppLanguage.ENGLISH.tag
                    AppLanguage.SYSTEM -> {
                        val system = java.util.Locale.getDefault().language
                        if (system in listOf(AppLanguage.RUSSIAN.tag, AppLanguage.KAZAKH.tag, AppLanguage.ENGLISH.tag)) {
                            system
                        } else {
                            AppLanguage.ENGLISH.tag
                        }
                    }
                }
            }
        }
    }

    private fun resolveRange(period: AnalyticsPeriod): Pair<Instant?, Instant?> {
        if (period == AnalyticsPeriod.ALL) return null to null
        val now = Instant.now()
        val duration = when (period) {
            AnalyticsPeriod.WEEK -> Duration.ofDays(7)
            AnalyticsPeriod.MONTH -> Duration.ofDays(30)
            AnalyticsPeriod.QUARTER -> Duration.ofDays(90)
            AnalyticsPeriod.YEAR -> Duration.ofDays(365)
            AnalyticsPeriod.ALL -> Duration.ZERO
        }
        return now.minus(duration) to now
    }

    private fun applySummary(summary: AnalyticsSummary, showPrompt: Boolean) {
        activeRequest = null
        val normalizedSummary = normalizeSummaryDisplayGroups(summary)
        val allPending = normalizedSummary.pendingItems
        val visible = allPending.filterNot { dismissedPending.contains(pendingKey(it)) }
        val shouldPrompt = showPrompt && pendingPromptEnabled && !pendingPromptShown && allPending.isNotEmpty()
        if (shouldPrompt) pendingPromptShown = true
        lastLoadedPeriod = _state.value.period
        _state.update {
            it.copy(
                isLoading = false,
                summary = normalizedSummary,
                pendingItems = visible,
                pendingItemsAll = allPending,
                resolvingItemName = null,
                pendingPromptVisible = shouldPrompt,
            )
        }
    }

    private fun normalizeSummaryDisplayGroups(summary: AnalyticsSummary): AnalyticsSummary {
        fun normalizeGroupId(raw: String): String {
            return if (raw == "00") RetailDisplayGroupResolver.FALLBACK_GROUP_ID else raw
        }

        val totalsByGroup = LinkedHashMap<String, Double>()
        summary.categoryTotals.forEach { total ->
            val key = normalizeGroupId(total.categoryId)
            totalsByGroup[key] = (totalsByGroup[key] ?: 0.0) + total.amount
        }
        val safeTotal = summary.totalSpent.takeIf { it > 0.0 } ?: totalsByGroup.values.sum().takeIf { it > 0.0 } ?: 1.0
        val mergedTotals = totalsByGroup.entries
            .sortedByDescending { it.value }
            .map { (id, amount) ->
                com.chelovecheck.domain.model.CategoryTotal(
                    categoryId = id,
                    amount = amount,
                    share = (amount / safeTotal).toFloat(),
                )
            }

        val mergedItems = LinkedHashMap<String, List<com.chelovecheck.domain.model.CategoryItemTotal>>()
        summary.categoryItems.forEach { (categoryId, items) ->
            val normalized = normalizeGroupId(categoryId)
            val existing = mergedItems[normalized].orEmpty()
            mergedItems[normalized] = mergeCategoryItems(existing + items)
        }

        return summary.copy(
            categoryTotals = mergedTotals,
            categoryItems = mergedItems,
        )
    }

    private fun mergeCategoryItems(items: List<com.chelovecheck.domain.model.CategoryItemTotal>): List<com.chelovecheck.domain.model.CategoryItemTotal> {
        if (items.isEmpty()) return emptyList()
        val merged = LinkedHashMap<String, com.chelovecheck.domain.model.CategoryItemTotal>()
        items.forEach { item ->
            val key = ItemNameNormalizer.normalizeForMatch(item.sourceItemName)
                .ifBlank { item.sourceItemName.trim() }
            val current = merged[key]
            if (current == null) {
                merged[key] = item
            } else {
                val display = if (item.displayItemName.length > current.displayItemName.length) {
                    item.displayItemName
                } else {
                    current.displayItemName
                }
                merged[key] = current.copy(
                    displayItemName = display,
                    amount = current.amount + item.amount,
                    count = current.count + item.count,
                )
            }
        }
        return merged.values.sortedByDescending { it.amount }
    }

    private fun resolveLabel(category: CoicopCategory, languageTag: String): String {
        val english = category.names[AppLanguage.ENGLISH.tag]
        val primary = category.names[languageTag]
        val fallback = if (languageTag == AppLanguage.KAZAKH.tag) category.names[AppLanguage.RUSSIAN.tag] else null
        val candidate = primary ?: fallback
        val placeholder = !candidate.isNullOrBlank() && !english.isNullOrBlank() && candidate.equals(english, ignoreCase = true)
        if (languageTag != AppLanguage.ENGLISH.tag && placeholder) {
            category.parentId?.let { parentId ->
                val parent = categories[parentId]
                if (parent != null) {
                    val parentLabel = parent.names[languageTag]
                        ?: parent.names[AppLanguage.RUSSIAN.tag].takeIf { languageTag == AppLanguage.KAZAKH.tag }
                        ?: parent.names[AppLanguage.ENGLISH.tag]
                    if (!parentLabel.isNullOrBlank()) {
                        return parentLabel
                    }
                }
            }
        }
        return candidate
            ?: english
            ?: category.names.values.firstOrNull()
            ?: category.id
    }

    private fun pendingKey(item: PendingCategoryItem): String {
        val base = ItemNameNormalizer.normalizeForMatch(item.sourceItemName)
        val net = item.networkKey ?: "__unknown__"
        return "$base|$net"
    }

    private data class AnalyticsRequest(
        val period: AnalyticsPeriod,
        val token: Long,
    )

    suspend fun formatKztForDisplay(kztAmount: Double): String {
        val c = displayCurrency.value
        val v = convertAmountUseCase(kztAmount, c)
        return DisplayMoneyFormatter.format(v, c)
    }
}

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val summary: AnalyticsSummary? = null,
    val period: AnalyticsPeriod = AnalyticsPeriod.ALL,
    /** Retail display group ids for manual category dialogs (excludes adjustments bucket). */
    val pickerDisplayGroupIds: List<String> = emptyList(),
    val pendingItems: List<PendingCategoryItem> = emptyList(),
    val pendingItemsAll: List<PendingCategoryItem> = emptyList(),
    val categoryChangeItem: PendingCategoryItem? = null,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val resolvingItemName: String? = null,
    val loadingStage: AnalyticsLoadStage? = null,
    val pendingPromptVisible: Boolean = false,
)

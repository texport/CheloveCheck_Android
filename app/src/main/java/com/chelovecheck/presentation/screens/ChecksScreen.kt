package com.chelovecheck.presentation.screens

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chelovecheck.R
import com.chelovecheck.domain.model.ReceiptFilter
import com.chelovecheck.domain.model.ReceiptListItem
import com.chelovecheck.presentation.adaptive.AdaptiveLayoutPolicy
import com.chelovecheck.presentation.screens.checks.ChecksGroupBottomSheet
import com.chelovecheck.presentation.screens.checks.ChecksListEntry
import com.chelovecheck.presentation.screens.checks.ChecksSortBottomSheet
import com.chelovecheck.presentation.screens.checks.EmptySearchState
import com.chelovecheck.presentation.screens.checks.EmptyState
import com.chelovecheck.presentation.screens.checks.ErrorEmptyState
import com.chelovecheck.presentation.screens.checks.FiltersRow
import com.chelovecheck.presentation.screens.checks.buildChecksListEntries
import com.chelovecheck.presentation.screens.checks.hasActiveQuery
import com.chelovecheck.presentation.screens.checks.shouldLoadMore
import com.chelovecheck.presentation.components.M3MaxWidthColumn
import com.chelovecheck.presentation.money.rememberChecksDisplayMoney
import com.chelovecheck.presentation.viewmodel.ChecksViewModel
import com.chelovecheck.presentation.viewmodel.ChecksUiEvent
import com.chelovecheck.presentation.viewmodel.RefreshMode
import com.chelovecheck.presentation.utils.openMap
import com.chelovecheck.presentation.utils.rememberHapticPerformer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecksScreen(
    adaptivePolicy: AdaptiveLayoutPolicy? = null,
    onOpenReceipt: (fiscalSign: String, searchQuery: String) -> Unit,
    scrollToTopSignal: Int = 0,
    viewModel: ChecksViewModel = hiltViewModel(),
) {
    val isTwoPaneLayout = adaptivePolicy?.preferTwoPane == true
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mapProvider by viewModel.mapProvider.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val groupMode by viewModel.groupMode.collectAsStateWithLifecycle()
    var sortSheetOpen by remember { mutableStateOf(false) }
    var groupSheetOpen by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val locale = LocalConfiguration.current.locales[0]
    val dayFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
    }
    val monthFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    }
    val listEntries = remember(state.receipts, groupMode, sortOrder, locale) {
        buildChecksListEntries(
            state.receipts,
            groupMode,
            sortOrder,
            zone,
            formatDayTitle = { dayFormatter.format(it) },
            formatMonthTitle = { ym -> monthFormatter.format(ym.atDay(1)) },
        )
    }
    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = rememberHapticPerformer()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<ReceiptListItem?>(null) }
    var addressError by remember { mutableStateOf(false) }
    val shouldLoadMore by remember {
        derivedStateOf {
            if (state.receipts.isEmpty()) {
                false
            } else {
                shouldLoadMore(listState, state.receipts.size)
            }
        }
    }
    val pullRefreshState = rememberPullToRefreshState()
    val horizontalPadding = if (isTwoPaneLayout) 24.dp else 16.dp

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onScreenResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadNextPage()
        }
    }

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ChecksUiEvent.ScrollToTop -> listState.animateScrollToItem(0)
            }
        }
    }

    LaunchedEffect(state.errorMessage, state.receipts.size) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        if (state.receipts.isEmpty()) return@LaunchedEffect
        try {
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = context.getString(R.string.action_retry),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.dismissError()
                viewModel.refresh(RefreshMode.Full)
            }
        } finally {
            viewModel.dismissError()
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_receipt_confirm_title)) },
            text = { Text(stringResource(R.string.delete_receipt_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val item = pendingDelete
                        pendingDelete = null
                        if (item != null) {
                            haptics(HapticFeedbackType.LongPress)
                            viewModel.deleteReceipt(item.summary.fiscalSign)
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (addressError) {
        AlertDialog(
            onDismissRequest = { addressError = false },
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(stringResource(R.string.error_no_address)) },
            confirmButton = {
                TextButton(onClick = { addressError = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_receipts)) },
                actions = {
                    IconButton(
                        onClick = { sortSheetOpen = true },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Sort,
                            contentDescription = stringResource(R.string.cd_checks_sort),
                        )
                    }
                    IconButton(
                        onClick = { groupSheetOpen = true },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = stringResource(R.string.cd_checks_group),
                        )
                    }
                },
            )
        },
    ) { padding ->
        val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            start = padding.calculateStartPadding(layoutDirection),
            top = padding.calculateTopPadding(),
            end = padding.calculateEndPadding(layoutDirection),
            bottom = 0.dp,
        )
        M3MaxWidthColumn(
            modifier = Modifier
                .padding(contentPadding)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                    )
                },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                SearchField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    onClear = {
                        haptics(HapticFeedbackType.GestureThresholdActivate)
                        viewModel.onSearchQueryChange("")
                    },
                    onSearchSubmit = {
                        haptics(HapticFeedbackType.GestureThresholdActivate)
                    },
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FiltersRow(
                    activeFilter = state.activeFilter,
                    ownershipFilter = state.ownershipFilter,
                    onFilterSelected = { filter ->
                        haptics(HapticFeedbackType.GestureThresholdActivate)
                        viewModel.onFilterSelected(filter)
                    },
                    onOwnershipSelected = { own ->
                        haptics(HapticFeedbackType.GestureThresholdActivate)
                        viewModel.onOwnershipFilterSelected(own)
                    },
                    onOpenDatePicker = {
                        haptics(HapticFeedbackType.GestureThresholdActivate)
                    },
                    onDateChosen = {
                        haptics(HapticFeedbackType.GestureThresholdActivate)
                    },
                )
            }

            val listPaneModifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pullToRefresh(
                    isRefreshing = state.isRefreshing,
                    state = pullRefreshState,
                    enabled = !state.isLoading || state.receipts.isNotEmpty(),
                    onRefresh = { viewModel.refresh(RefreshMode.SoftResume) },
                )

            when {
                state.isLoading && state.receipts.isEmpty() && state.errorMessage == null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.receipts.isEmpty() && !state.isLoading && state.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        ErrorEmptyState(
                            detailMessage = state.errorMessage,
                            onRetry = {
                                haptics(HapticFeedbackType.GestureThresholdActivate)
                                viewModel.dismissError()
                                viewModel.refresh(RefreshMode.Full)
                            },
                        )
                    }
                }

                state.receipts.isEmpty() && !state.isLoading && state.errorMessage == null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (hasActiveQuery(state)) {
                            EmptySearchState()
                        } else {
                            EmptyState()
                        }
                    }
                }

                else -> {
                    Box(modifier = listPaneModifier) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(
                                items = listEntries,
                                key = { it.stableKey },
                                contentType = { entry ->
                                    when (entry) {
                                        is ChecksListEntry.SectionHeader -> "header"
                                        is ChecksListEntry.ReceiptRow -> "receipt"
                                    }
                                },
                            ) { entry ->
                                when (entry) {
                                    is ChecksListEntry.SectionHeader -> {
                                        Text(
                                            text = entry.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                        )
                                    }
                                    is ChecksListEntry.ReceiptRow -> {
                                        val item = entry.item
                                        val totalFormatted =
                                            rememberChecksDisplayMoney(
                                                item.summary.totalSum,
                                                item.summary.dateTimeEpochMillis,
                                                viewModel,
                                            )
                                        SwipeableReceiptCard(
                                            item = item,
                                            totalFormatted = totalFormatted,
                                            searchHighlight = state.searchQuery,
                                            onOpenReceipt = { fs ->
                                                haptics(HapticFeedbackType.GestureThresholdActivate)
                                                Log.d(
                                                    "ReceiptNav",
                                                    "open receipt click: fiscalSign=$fs searchQuery='${state.searchQuery.take(80)}'",
                                                )
                                                onOpenReceipt(fs, state.searchQuery)
                                            },
                                            onToggleFavorite = {
                                                haptics(HapticFeedbackType.GestureThresholdActivate)
                                                viewModel.toggleFavorite(
                                                    item.summary.fiscalSign,
                                                    !item.summary.isFavorite,
                                                )
                                            },
                                            onTogglePin = {
                                                haptics(HapticFeedbackType.GestureThresholdActivate)
                                                viewModel.togglePinned(
                                                    item.summary.fiscalSign,
                                                    !item.summary.isPinned,
                                                )
                                            },
                                            onRequestDelete = {
                                                viewModel.logSwipe(
                                                    "delete request: fiscalSign=${item.summary.fiscalSign} " +
                                                        "pending=${pendingDelete?.summary?.fiscalSign ?: "null"}",
                                                )
                                                if (pendingDelete == null) {
                                                    pendingDelete = item
                                                }
                                            },
                                            onOpenMap = {
                                                val address = item.summary.companyAddress
                                                val (query, source) = when {
                                                    address.isNotBlank() -> address to "address"
                                                    item.displayName.isNotBlank() -> item.displayName to "brand"
                                                    item.summary.companyName.isNotBlank() ->
                                                        item.summary.companyName to "company"
                                                    else -> "" to "empty"
                                                }
                                                viewModel.logMapOpen(mapProvider, source, query)
                                                val result = openMap(
                                                    context = context,
                                                    provider = mapProvider,
                                                    query = query,
                                                )
                                                viewModel.logMapOpenResult(
                                                    provider = mapProvider,
                                                    source = source,
                                                    query = query,
                                                    primaryUri = result.primaryUri?.toString().orEmpty(),
                                                    fallbackUri = result.fallbackUri?.toString().orEmpty(),
                                                    success = result.success,
                                                    usedFallback = result.usedFallback,
                                                    failureReason = result.failureReason,
                                                    usedPackage = result.usedPackage,
                                                )
                                                if (!result.success) {
                                                    addressError = true
                                                }
                                            },
                                            onSwipeLog = viewModel::logSwipe,
                                        )
                                    }
                                }
                            }
                            items(
                                count = if (state.isPaging) 1 else 0,
                                key = { "paging_loader" },
                                contentType = { "loader" },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                        PullToRefreshDefaults.Indicator(
                            modifier = Modifier.align(Alignment.TopCenter),
                            isRefreshing = state.isRefreshing,
                            state = pullRefreshState,
                            containerColor = MaterialTheme.colorScheme.surface,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }

    ChecksSortBottomSheet(
        visible = sortSheetOpen,
        current = sortOrder,
        onDismiss = { sortSheetOpen = false },
        onHaptic = { haptics(HapticFeedbackType.GestureThresholdActivate) },
        onSelect = { order ->
            sortSheetOpen = false
            viewModel.setSortOrder(order)
        },
    )
    ChecksGroupBottomSheet(
        visible = groupSheetOpen,
        current = groupMode,
        onDismiss = { groupSheetOpen = false },
        onHaptic = { haptics(HapticFeedbackType.GestureThresholdActivate) },
        onSelect = { mode ->
            groupSheetOpen = false
            viewModel.setGroupMode(mode)
        },
    )
}

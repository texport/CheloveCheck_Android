package com.chelovecheck.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelovecheck.R
import com.chelovecheck.domain.model.AnalyticsPeriod
import com.chelovecheck.presentation.permissions.PermissionManager
import com.chelovecheck.presentation.screens.analytics.CategoryItemsDialog
import com.chelovecheck.presentation.screens.analytics.CategoryResolveDialog
import com.chelovecheck.presentation.screens.analytics.CategoryRow
import com.chelovecheck.presentation.screens.analytics.MerchantRow
import com.chelovecheck.presentation.screens.analytics.PaymentRow
import com.chelovecheck.presentation.screens.analytics.PendingCategoriesDialog
import com.chelovecheck.presentation.screens.analytics.PeriodChips
import com.chelovecheck.presentation.screens.analytics.SectionTitle
import com.chelovecheck.presentation.screens.analytics.SectionTitleWithAction
import com.chelovecheck.presentation.screens.analytics.SummaryCard
import com.chelovecheck.presentation.components.M3MaxWidthColumn
import com.chelovecheck.presentation.adaptive.AdaptiveLayoutPolicy
import com.chelovecheck.presentation.viewmodel.AnalyticsViewModel
import com.chelovecheck.presentation.utils.rememberHapticPerformer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnalyticsScreen(
    adaptivePolicy: AdaptiveLayoutPolicy? = null,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pendingCount = state.pendingItemsAll.size
    var showPendingDialog by rememberSaveable { mutableStateOf(false) }
    var selectedPending by remember { mutableStateOf<com.chelovecheck.domain.model.PendingCategoryItem?>(null) }
    var categoriesExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val horizontalPadding = if (adaptivePolicy?.preferTwoPane == true) 24.dp else 16.dp
    val haptics = rememberHapticPerformer()
    val permissionManager = remember(context) { PermissionManager(context.applicationContext) }
    var hasNotificationPermission by remember {
        mutableStateOf(permissionManager.hasNotificationPermission())
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasNotificationPermission = granted },
    )

    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && permissionManager.shouldRequestNotificationPermission()) {
            notificationPermissionLauncher.launch(permissionManager.notificationPermission)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_analytics)) },
                actions = {
                    IconButton(onClick = {
                        haptics(HapticFeedbackType.GestureThresholdActivate)
                        showPendingDialog = true
                    }) {
                        BadgedBox(
                            badge = {
                                if (pendingCount > 0) {
                                    Badge { Text(pendingCount.toString()) }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Rule,
                                contentDescription = stringResource(R.string.analytics_pending_action),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        M3MaxWidthColumn(
            modifier = Modifier
                .padding(padding)
                .padding(start = horizontalPadding, end = horizontalPadding, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PeriodChips(
                selected = state.period,
                onSelected = { period ->
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    viewModel.setPeriod(period)
                },
            )

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val stageText = when (state.loadingStage) {
                                com.chelovecheck.domain.model.AnalyticsLoadStage.LOADING_MODEL ->
                                    stringResource(R.string.analytics_loading_model)
                                com.chelovecheck.domain.model.AnalyticsLoadStage.BUILDING_INDEX ->
                                    stringResource(R.string.analytics_loading_index)
                                com.chelovecheck.domain.model.AnalyticsLoadStage.ANALYZING_RECEIPTS ->
                                    stringResource(R.string.analytics_loading_analyze)
                                null -> stringResource(R.string.analytics_loading)
                            }
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = stageText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.analytics_loading_hint),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                state.summary == null || state.summary?.totalSpent == 0.0 -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.analytics_empty))
                    }
                }
                else -> {
                    val summary = state.summary!!
                    val maxVisibleCategories = 6
                    val hasMoreCategories = summary.categoryTotals.size > maxVisibleCategories
                    val visibleCategories = if (categoriesExpanded || !hasMoreCategories) {
                        summary.categoryTotals
                    } else {
                        summary.categoryTotals.take(maxVisibleCategories)
                    }
                    if (adaptivePolicy?.preferTwoPane == true) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                item {
                                    SummaryCard(
                                        total = summary.totalSpent,
                                        receiptsCount = summary.receiptsCount,
                                        average = summary.averageReceipt,
                                        viewModel = viewModel,
                                    )
                                }
                                item {
                                    SectionTitleWithAction(
                                        icon = Icons.Outlined.BarChart,
                                        title = stringResource(R.string.analytics_categories),
                                        actionLabel = if (hasMoreCategories) {
                                            if (categoriesExpanded) {
                                                stringResource(R.string.action_collapse)
                                            } else {
                                                stringResource(
                                                    R.string.analytics_show_all,
                                                    summary.categoryTotals.size - maxVisibleCategories,
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        onActionClick = {
                                            haptics(HapticFeedbackType.GestureThresholdActivate)
                                            categoriesExpanded = !categoriesExpanded
                                        },
                                    )
                                }
                                items(visibleCategories) { item ->
                                    CategoryRow(
                                        item = item,
                                        categoryLabel = viewModel.categoryLabel(item.categoryId),
                                        viewModel = viewModel,
                                        onClick = {
                                            haptics(HapticFeedbackType.GestureThresholdActivate)
                                            selectedCategoryId = item.categoryId
                                        },
                                    )
                                }
                            }
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                item {
                                    SectionTitle(
                                        icon = Icons.Outlined.Payments,
                                        title = stringResource(R.string.analytics_payments),
                                    )
                                }
                                items(summary.paymentTotals) { item ->
                                    PaymentRow(item, viewModel)
                                }
                                item {
                                    SectionTitle(
                                        icon = Icons.Outlined.Insights,
                                        title = stringResource(R.string.analytics_merchants),
                                    )
                                }
                                items(summary.topMerchants) { item ->
                                    MerchantRow(item, viewModel)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item {
                                SummaryCard(
                                    total = summary.totalSpent,
                                    receiptsCount = summary.receiptsCount,
                                    average = summary.averageReceipt,
                                    viewModel = viewModel,
                                )
                            }

                            item {
                                SectionTitleWithAction(
                                    icon = Icons.Outlined.BarChart,
                                    title = stringResource(R.string.analytics_categories),
                                    actionLabel = if (hasMoreCategories) {
                                        if (categoriesExpanded) {
                                            stringResource(R.string.action_collapse)
                                        } else {
                                            stringResource(
                                                R.string.analytics_show_all,
                                                summary.categoryTotals.size - maxVisibleCategories,
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    onActionClick = {
                                        haptics(HapticFeedbackType.GestureThresholdActivate)
                                        categoriesExpanded = !categoriesExpanded
                                    },
                                )
                            }
                            items(visibleCategories) { item ->
                                CategoryRow(
                                    item = item,
                                    categoryLabel = viewModel.categoryLabel(item.categoryId),
                                    viewModel = viewModel,
                                    onClick = {
                                        haptics(HapticFeedbackType.GestureThresholdActivate)
                                        selectedCategoryId = item.categoryId
                                    },
                                )
                            }

                            item {
                                SectionTitle(
                                    icon = Icons.Outlined.Payments,
                                    title = stringResource(R.string.analytics_payments),
                                )
                            }
                            items(summary.paymentTotals) { item ->
                                PaymentRow(item, viewModel)
                            }

                            item {
                                SectionTitle(
                                    icon = Icons.Outlined.Insights,
                                    title = stringResource(R.string.analytics_merchants),
                                )
                            }
                            items(summary.topMerchants) { item ->
                                MerchantRow(item, viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.pendingPromptVisible) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPendingPrompt,
            title = { Text(stringResource(R.string.analytics_pending_prompt_title)) },
            text = { Text(stringResource(R.string.analytics_pending_prompt_body)) },
            confirmButton = {
                TextButton(onClick = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    viewModel.dismissPendingPrompt()
                    showPendingDialog = true
                }) {
                    Text(stringResource(R.string.action_choose))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptics(HapticFeedbackType.GestureThresholdActivate)
                    viewModel.dismissPendingPrompt()
                }) {
                    Text(stringResource(R.string.action_later))
                }
            },
        )
    }

    if (showPendingDialog) {
        PendingCategoriesDialog(
            items = state.pendingItemsAll,
            rollupPreviewLabel = viewModel::rollupPreviewLabel,
            onSelect = { item ->
                selectedPending = item
            },
            onDismiss = { showPendingDialog = false },
        )
    }

    val activePending = selectedPending
    if (activePending != null) {
        CategoryResolveDialog(
            item = activePending,
            pickerGroupIds = state.pickerDisplayGroupIds,
            labelProvider = viewModel::categoryLabel,
            onSelect = { id ->
                viewModel.confirmPendingCategory(activePending, id)
                selectedPending = null
            },
            onDismiss = {
                viewModel.dismissPendingCategory(activePending)
                selectedPending = null
            },
            isResolving = false,
        )
    }

    val activeCategoryId = selectedCategoryId
    if (activeCategoryId != null) {
        val items = state.summary?.categoryItems?.get(activeCategoryId).orEmpty()
        CategoryItemsDialog(
            title = stringResource(
                R.string.analytics_category_items_title,
                viewModel.categoryLabel(activeCategoryId),
            ),
            items = items,
            viewModel = viewModel,
            onItemClick = {
                selectedCategoryId = null
                viewModel.requestCategoryChange(it.sourceItemName, it.displayItemName)
            },
            onDismiss = { selectedCategoryId = null },
        )
    }

    val changeItem = state.categoryChangeItem
    if (changeItem != null) {
        CategoryResolveDialog(
            item = changeItem,
            pickerGroupIds = state.pickerDisplayGroupIds,
            labelProvider = viewModel::categoryLabel,
            onSelect = { id ->
                viewModel.confirmCategoryChange(changeItem.sourceItemName, id)
            },
            onDismiss = viewModel::dismissCategoryChange,
            isResolving = state.resolvingItemName == changeItem.sourceItemName,
        )
    }
}

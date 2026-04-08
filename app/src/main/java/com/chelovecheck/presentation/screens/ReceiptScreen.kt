package com.chelovecheck.presentation.screens

import android.util.Base64
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelovecheck.R
import com.chelovecheck.domain.model.Receipt
import com.chelovecheck.presentation.adaptive.AdaptiveLayoutPolicy
import com.chelovecheck.presentation.money.rememberReceiptDisplayMoney
import com.chelovecheck.presentation.strings.formatMoney
import com.chelovecheck.presentation.strings.formatDecimal
import com.chelovecheck.presentation.strings.ofdLabel
import com.chelovecheck.presentation.strings.operationLabel
import com.chelovecheck.presentation.strings.paymentLabel
import com.chelovecheck.presentation.strings.unitShortLabel
import com.chelovecheck.presentation.viewmodel.ReceiptViewModel
import com.chelovecheck.presentation.viewmodel.ReceiptLoadingStage
import com.chelovecheck.presentation.utils.rememberHapticPerformer
import com.chelovecheck.presentation.utils.buildSearchHighlightedText
import com.chelovecheck.presentation.screens.receiptItemsSection
import com.chelovecheck.presentation.screens.ReceiptItemCard
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.io.FileOutputStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    adaptivePolicy: AdaptiveLayoutPolicy? = null,
    fiscalSign: String,
    highlightKey: String = "-",
    onClose: () -> Unit,
    onOpenProduct: (String) -> Unit = {},
    viewModel: ReceiptViewModel = hiltViewModel(),
) {
    val isTwoPaneLayout = adaptivePolicy?.preferTwoPane == true
    val searchHighlight = remember(highlightKey) { decodeReceiptHighlightKey(highlightKey) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var infoDialogMessage by remember { mutableStateOf<String?>(null) }
    var showEditAddressDialog by remember { mutableStateOf(false) }
    var addressDraft by remember { mutableStateOf("") }
    var pendingDeleteIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHapticPerformer()

    LaunchedEffect(highlightKey, searchHighlight) {
        Log.d(
            "ReceiptSearch",
            "screen init: fiscalSign=$fiscalSign highlightKey='$highlightKey' decodedQuery='${searchHighlight?.take(80)}'",
        )
    }

    LaunchedEffect(fiscalSign) {
        viewModel.loadReceipt(fiscalSign)
    }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            onClose()
        }
    }

    val refetchErr = state.refetchError
    if (refetchErr != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRefetchError,
            confirmButton = {
                TextButton(onClick = viewModel::dismissRefetchError) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            title = { Text(stringResource(R.string.receipt_refetch)) },
            text = { Text(refetchErr) },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    val selectedCount = state.selectedIds.size
                    Text(
                        text = if (state.isSelectionMode) {
                            stringResource(R.string.selected_count, selectedCount)
                        } else {
                            stringResource(R.string.title_receipt)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptics(HapticFeedbackType.GestureThresholdActivate)
                        if (state.isSelectionMode) {
                            viewModel.clearSelection()
                        } else {
                            onClose()
                        }
                    }) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                },
                actions = {
                    if (state.isSelectionMode) {
                        IconButton(onClick = {
                            haptics(HapticFeedbackType.LongPress)
                            pendingDeleteIds = state.selectedIds.toList()
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    } else {
                        IconButton(
                            onClick = {
                                haptics(HapticFeedbackType.GestureThresholdActivate)
                                viewModel.refetchFromOfd(fiscalSign)
                            },
                            enabled = state.receipt != null && !state.isRefetching,
                        ) {
                            if (state.isRefetching) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            } else {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = stringResource(R.string.receipt_refetch),
                                )
                            }
                        }
                        IconButton(onClick = {
                            haptics(HapticFeedbackType.GestureThresholdActivate)
                            val receipt = state.receipt ?: return@IconButton
                            runCatching { shareReceiptPdf(context, receipt) }
                                .onFailure {
                                    scope.launch { infoDialogMessage = context.getString(R.string.error_pdf) }
                                }
                        }) {
                            Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.action_share))
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (state.loadingStage) {
                            ReceiptLoadingStage.LOADING_RECEIPT -> stringResource(R.string.receipt_loading_receipt)
                            ReceiptLoadingStage.TRANSLATING_ITEMS -> stringResource(R.string.receipt_loading_translating_items)
                            null -> stringResource(R.string.receipt_loading_receipt)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.receipt != null -> {
                val receipt = state.receipt
                if (receipt != null) {
                    ReceiptContent(
                        isTwoPaneLayout = isTwoPaneLayout,
                        receipt = receipt,
                        items = state.items,
                        selectedIds = state.selectedIds,
                        isSelectionMode = state.isSelectionMode,
                        onToggleSelect = viewModel::toggleSelection,
                        onOpenProduct = onOpenProduct,
                        receiptViewModel = viewModel,
                        searchHighlight = searchHighlight,
                        onEditAddress = {
                            addressDraft = receipt.companyAddress
                            showEditAddressDialog = true
                        },
                        modifier = Modifier.padding(padding),
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(R.string.receipt_not_found))
                }
            }
        }
    }

    if (infoDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { infoDialogMessage = null },
            confirmButton = {
                TextButton(onClick = { infoDialogMessage = null }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            title = { Text(stringResource(R.string.title_receipt)) },
            text = { Text(infoDialogMessage.orEmpty()) },
        )
    }

    if (pendingDeleteIds.isNotEmpty()) {
        val isMultiple = pendingDeleteIds.size > 1
        AlertDialog(
            onDismissRequest = { pendingDeleteIds = emptyList() },
            title = {
                Text(
                    stringResource(
                        if (isMultiple) R.string.delete_items_confirm_title else R.string.delete_item_confirm_title,
                    ),
                )
            },
            text = { Text(stringResource(R.string.delete_item_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ids = pendingDeleteIds
                        pendingDeleteIds = emptyList()
                        if (ids.size > 1) {
                            viewModel.deleteSelectedItems()
                        } else {
                            ids.firstOrNull()?.let(viewModel::deleteItem)
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteIds = emptyList() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showEditAddressDialog) {
        AlertDialog(
            onDismissRequest = { showEditAddressDialog = false },
            title = { Text(stringResource(R.string.receipt_edit_address_title)) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = addressDraft,
                    onValueChange = { addressDraft = it },
                    label = { Text(stringResource(R.string.receipt_edit_address_label)) },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateAddress(addressDraft.trim())
                    showEditAddressDialog = false
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAddressDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

private fun decodeReceiptHighlightKey(key: String): String? {
    if (key == "-" || key.isEmpty()) return null
    return try {
        String(
            Base64.decode(key, Base64.URL_SAFE or Base64.NO_PADDING),
            StandardCharsets.UTF_8,
        )
    } catch (_: IllegalArgumentException) {
        null
    }
}

@Composable
private fun ReceiptContent(
    isTwoPaneLayout: Boolean,
    receipt: Receipt,
    items: List<com.chelovecheck.presentation.viewmodel.ReceiptItemUi>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onToggleSelect: (Long) -> Unit,
    onOpenProduct: (String) -> Unit,
    receiptViewModel: ReceiptViewModel,
    searchHighlight: String?,
    onEditAddress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNarrowWidth = LocalConfiguration.current.screenWidthDp <= 380
    val datePattern = stringResource(R.string.date_time_format)
    val date = remember(receipt.dateTime, datePattern) {
        val formatter = DateTimeFormatter.ofPattern(datePattern)
        formatter.format(receipt.dateTime.atZone(ZoneId.systemDefault()))
    }
    val totalSummaryText = rememberReceiptDisplayMoney(receipt.totalSum, receiptViewModel)

    Box(modifier = modifier.fillMaxSize()) {
        if (isTwoPaneLayout) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        SectionTitle(
                            title = stringResource(R.string.receipt_section_items),
                            searchHighlight = searchHighlight,
                        )
                        Text(
                            text = buildSearchHighlightedText(
                                stringResource(R.string.receipt_items_tap_hint),
                                searchHighlight,
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    receiptItemsSection(
                        items = items,
                        selectedIds = selectedIds,
                        isSelectionMode = isSelectionMode,
                        onToggleSelect = onToggleSelect,
                        viewModel = receiptViewModel,
                        onOpenProduct = onOpenProduct,
                        searchHighlight = searchHighlight,
                    )
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        SummaryCard(
                            receipt = receipt,
                            date = date,
                            totalFormatted = totalSummaryText,
                            isNarrowWidth = isNarrowWidth,
                            searchHighlight = searchHighlight,
                            onEditAddress = onEditAddress,
                        )
                    }
                    item {
                        SectionTitle(
                            title = stringResource(R.string.receipt_section_payments),
                            searchHighlight = searchHighlight,
                        )
                        PaymentsCard(
                            receipt = receipt,
                            viewModel = receiptViewModel,
                            searchHighlight = searchHighlight,
                        )
                    }
                    item {
                        SectionTitle(
                            title = stringResource(R.string.receipt_section_details),
                            searchHighlight = searchHighlight,
                        )
                        DetailsCard(receipt = receipt, searchHighlight = searchHighlight)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    SummaryCard(
                        receipt = receipt,
                        date = date,
                        totalFormatted = totalSummaryText,
                        isNarrowWidth = isNarrowWidth,
                        searchHighlight = searchHighlight,
                        onEditAddress = onEditAddress,
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionTitle(
                            title = stringResource(R.string.receipt_section_items),
                            searchHighlight = searchHighlight,
                        )
                        Text(
                            text = buildSearchHighlightedText(
                                stringResource(R.string.receipt_items_tap_hint),
                                searchHighlight,
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                receiptItemsSection(
                    items = items,
                    selectedIds = selectedIds,
                    isSelectionMode = isSelectionMode,
                    onToggleSelect = onToggleSelect,
                    viewModel = receiptViewModel,
                    onOpenProduct = onOpenProduct,
                    searchHighlight = searchHighlight,
                )

                item {
                    SectionTitle(
                        title = stringResource(R.string.receipt_section_payments),
                        searchHighlight = searchHighlight,
                    )
                    PaymentsCard(
                        receipt = receipt,
                        viewModel = receiptViewModel,
                        searchHighlight = searchHighlight,
                    )
                }

                item {
                    SectionTitle(
                        title = stringResource(R.string.receipt_section_details),
                        searchHighlight = searchHighlight,
                    )
                    DetailsCard(receipt = receipt, searchHighlight = searchHighlight)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    receipt: Receipt,
    date: String,
    totalFormatted: String,
    isNarrowWidth: Boolean,
    searchHighlight: String?,
    onEditAddress: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            val hlBg = MaterialTheme.colorScheme.secondaryContainer
            val hlFg = MaterialTheme.colorScheme.onSecondaryContainer
            Text(
                text = buildSearchHighlightedText(
                    receipt.companyName,
                    searchHighlight,
                    hlBg,
                    hlFg,
                ),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val addrText = receipt.companyAddress.ifBlank { stringResource(R.string.receipt_summary_address_missing) }
                Text(
                    text = buildSearchHighlightedText(addrText, searchHighlight, hlBg, hlFg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onEditAddress) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.action_edit))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isNarrowWidth) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column {
                        Text(
                            text = buildSearchHighlightedText(
                                stringResource(R.string.receipt_summary_date),
                                searchHighlight,
                                hlBg,
                                hlFg,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = buildSearchHighlightedText(date, searchHighlight, hlBg, hlFg),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Column {
                        Text(
                            text = buildSearchHighlightedText(
                                stringResource(R.string.receipt_summary_total),
                                searchHighlight,
                                hlBg,
                                hlFg,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = buildSearchHighlightedText(totalFormatted, searchHighlight, hlBg, hlFg),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            buildSearchHighlightedText(
                                stringResource(R.string.receipt_summary_date),
                                searchHighlight,
                                hlBg,
                                hlFg,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = buildSearchHighlightedText(date, searchHighlight, hlBg, hlFg),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            buildSearchHighlightedText(
                                stringResource(R.string.receipt_summary_total),
                                searchHighlight,
                                hlBg,
                                hlFg,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = buildSearchHighlightedText(totalFormatted, searchHighlight, hlBg, hlFg),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = buildSearchHighlightedText(
                        operationLabel(receipt.typeOperation),
                        searchHighlight,
                        hlBg,
                        hlFg,
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, searchHighlight: String?) {
    Text(
        text = buildSearchHighlightedText(
            title,
            searchHighlight,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun PaymentsCard(receipt: Receipt, viewModel: ReceiptViewModel, searchHighlight: String?) {
    val hlBg = MaterialTheme.colorScheme.secondaryContainer
    val hlFg = MaterialTheme.colorScheme.onSecondaryContainer
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            receipt.totalType.forEach { payment ->
                PaymentAmountRow(
                    label = {
                        Text(
                            buildSearchHighlightedText(
                                paymentLabel(payment.type),
                                searchHighlight,
                                hlBg,
                                hlFg,
                            ),
                        )
                    },
                    amountKzt = payment.sum,
                    viewModel = viewModel,
                    searchHighlight = searchHighlight,
                )
            }
            receipt.taken?.takeIf { it > 0 }?.let { taken ->
                PaymentAmountRow(
                    label = {
                        Text(
                            buildSearchHighlightedText(
                                stringResource(R.string.receipt_payment_taken),
                                searchHighlight,
                                hlBg,
                                hlFg,
                            ),
                        )
                    },
                    amountKzt = taken,
                    viewModel = viewModel,
                    searchHighlight = searchHighlight,
                )
            }
            receipt.change?.takeIf { it > 0 }?.let { change ->
                PaymentAmountRow(
                    label = {
                        Text(
                            buildSearchHighlightedText(
                                stringResource(R.string.receipt_payment_change),
                                searchHighlight,
                                hlBg,
                                hlFg,
                            ),
                        )
                    },
                    amountKzt = change,
                    viewModel = viewModel,
                    searchHighlight = searchHighlight,
                )
            }
            val totalText = rememberReceiptDisplayMoney(receipt.totalSum, viewModel)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    buildSearchHighlightedText(stringResource(R.string.receipt_total), searchHighlight, hlBg, hlFg),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    buildSearchHighlightedText(totalText, searchHighlight, hlBg, hlFg),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PaymentAmountRow(
    label: @Composable () -> Unit,
    amountKzt: Double,
    viewModel: ReceiptViewModel,
    searchHighlight: String?,
) {
    val text = rememberReceiptDisplayMoney(amountKzt, viewModel)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Box(modifier = Modifier.weight(1f)) {
            label()
        }
        Text(
            text = buildSearchHighlightedText(
                text,
                searchHighlight,
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun DetailsCard(receipt: Receipt, searchHighlight: String?) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailRow(stringResource(R.string.receipt_details_ofd), ofdLabel(receipt.ofd), searchHighlight)
            DetailRow(stringResource(R.string.receipt_details_iin), receipt.iinBin, searchHighlight)
            DetailRow(stringResource(R.string.receipt_details_fp), receipt.fiscalSign, searchHighlight)
            DetailRow(stringResource(R.string.receipt_details_kkm), receipt.serialNumber, searchHighlight)
            DetailRow(stringResource(R.string.receipt_details_kgd), receipt.kgdId, searchHighlight)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, searchHighlight: String?) {
    val hlBg = MaterialTheme.colorScheme.secondaryContainer
    val hlFg = MaterialTheme.colorScheme.onSecondaryContainer
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = buildSearchHighlightedText(label, searchHighlight, hlBg, hlFg),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = buildSearchHighlightedText(value, searchHighlight, hlBg, hlFg),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
    }
}

private fun shareReceiptPdf(context: Context, receipt: Receipt) {
    val file = generateReceiptPdf(context, receipt)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
}

private fun generateReceiptPdf(context: Context, receipt: Receipt): File {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(226, 1000, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 10f
        typeface = Typeface.DEFAULT
    }

    var y = 20f
    val maxWidth = pageInfo.pageWidth - 20

    y += drawText(canvas, paint, receipt.companyName, 10f, y, maxWidth, bold = true)
    y += drawText(
        canvas,
        paint,
        context.getString(R.string.receipt_pdf_iin_bin, receipt.iinBin),
        10f,
        y,
        maxWidth,
    )
    if (receipt.companyAddress.isNotBlank()) {
        y += drawText(
            canvas,
            paint,
            context.getString(R.string.receipt_pdf_address, receipt.companyAddress),
            10f,
            y,
            maxWidth,
        )
    }

    val datePattern = context.getString(R.string.date_time_format)
    val date = DateTimeFormatter.ofPattern(datePattern)
        .format(receipt.dateTime.atZone(ZoneId.systemDefault()))
    y += drawText(canvas, paint, context.getString(R.string.receipt_pdf_date, date), 10f, y, maxWidth)

    y += 10f
    y += drawText(canvas, paint, context.getString(R.string.receipt_pdf_items), 12f, y, maxWidth, bold = true)

    receipt.items.forEachIndexed { index, item ->
        val unit = unitShortLabel(context, item.unit)
        val countFormatted = formatDecimal(context, item.count)
        val priceFormatted = formatDecimal(context, item.price)
        val line = if (unit.isBlank()) {
            context.getString(
                R.string.receipt_pdf_item_format,
                index + 1,
                item.name,
                countFormatted,
                priceFormatted,
            )
        } else {
            context.getString(
                R.string.receipt_pdf_item_unit_format,
                index + 1,
                item.name,
                countFormatted,
                unit,
                priceFormatted,
            )
        }
        y += drawText(canvas, paint, line, 9f, y, maxWidth)
        val itemSum = formatMoney(context, item.sum)
        y += drawText(
            canvas,
            paint,
            context.getString(R.string.receipt_pdf_sum_line, itemSum),
            9f,
            y,
            maxWidth,
        )
        y += 6f
    }

    val total = formatMoney(context, receipt.totalSum)
    y += drawText(
        canvas,
        paint,
        context.getString(R.string.receipt_pdf_total, total),
        12f,
        y,
        maxWidth,
        bold = true,
    )

    val qr = generateQrBitmap(receipt.url, 160)
    canvas.drawBitmap(qr, 20f, y + 12f, null)

    document.finishPage(page)

    val file = File(context.cacheDir, "receipt_${receipt.fiscalSign}.pdf")
    FileOutputStream(file).use { output ->
        document.writeTo(output)
    }
    document.close()
    return file
}

private fun drawText(
    canvas: Canvas,
    paint: Paint,
    text: String,
    textSize: Float,
    y: Float,
    maxWidth: Int,
    bold: Boolean = false,
): Float {
    paint.textSize = textSize
    paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

    val words = text.split(" ")
    var line = ""
    var lineHeight = paint.fontSpacing
    var currentY = y
    val x = 10f

    words.forEach { word ->
        val testLine = if (line.isEmpty()) word else "$line $word"
        if (paint.measureText(testLine) > maxWidth) {
            canvas.drawText(line, x, currentY, paint)
            currentY += lineHeight
            line = word
        } else {
            line = testLine
        }
    }

    if (line.isNotEmpty()) {
        canvas.drawText(line, x, currentY, paint)
        currentY += lineHeight
    }

    return currentY - y
}

private fun generateQrBitmap(content: String, size: Int): Bitmap {
    val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bmp
}

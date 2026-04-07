package com.chelovecheck.presentation.screens.checks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chelovecheck.R
import com.chelovecheck.domain.model.ReceiptGroupMode
import com.chelovecheck.domain.model.ReceiptListSortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChecksSortBottomSheet(
    visible: Boolean,
    current: ReceiptListSortOrder,
    onDismiss: () -> Unit,
    onSelect: (ReceiptListSortOrder) -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .selectableGroup(),
        ) {
            Text(
                text = stringResource(R.string.checks_sort_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            ReceiptListSortOrder.entries.forEach { order ->
                val label = sortOrderLabel(order)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = order == current,
                            onClick = { onSelect(order) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = order == current,
                        onClick = null,
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun sortOrderLabel(order: ReceiptListSortOrder): String = when (order) {
    ReceiptListSortOrder.DEFAULT -> stringResource(R.string.checks_sort_default)
    ReceiptListSortOrder.DATE_NEWEST -> stringResource(R.string.checks_sort_date_newest)
    ReceiptListSortOrder.DATE_OLDEST -> stringResource(R.string.checks_sort_date_oldest)
    ReceiptListSortOrder.AMOUNT_DESC -> stringResource(R.string.checks_sort_amount_desc)
    ReceiptListSortOrder.AMOUNT_ASC -> stringResource(R.string.checks_sort_amount_asc)
    ReceiptListSortOrder.MERCHANT_AZ -> stringResource(R.string.checks_sort_merchant_az)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChecksGroupBottomSheet(
    visible: Boolean,
    current: ReceiptGroupMode,
    onDismiss: () -> Unit,
    onSelect: (ReceiptGroupMode) -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .selectableGroup(),
        ) {
            Text(
                text = stringResource(R.string.checks_group_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            ReceiptGroupMode.entries.forEach { mode ->
                val label = groupModeLabel(mode)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = mode == current,
                            onClick = { onSelect(mode) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = mode == current,
                        onClick = null,
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun groupModeLabel(mode: ReceiptGroupMode): String = when (mode) {
    ReceiptGroupMode.NONE -> stringResource(R.string.checks_group_none)
    ReceiptGroupMode.BY_DAY -> stringResource(R.string.checks_group_by_day)
    ReceiptGroupMode.BY_MONTH -> stringResource(R.string.checks_group_by_month)
}

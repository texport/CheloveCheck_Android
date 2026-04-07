package com.chelovecheck.presentation.screens.checks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chelovecheck.R
import com.chelovecheck.domain.model.ReceiptFilter
import com.chelovecheck.domain.model.ReceiptOwnershipFilter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FiltersRow(
    activeFilter: ReceiptFilter,
    ownershipFilter: ReceiptOwnershipFilter,
    onFilterSelected: (ReceiptFilter) -> Unit,
    onOwnershipSelected: (ReceiptOwnershipFilter) -> Unit,
    onOpenDatePicker: () -> Unit,
    onDateChosen: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateChosen()
                        onFilterSelected(ReceiptFilter.ByDate(date))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.action_choose))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val dateLabel = when (activeFilter) {
        is ReceiptFilter.ByDate -> {
            val pattern = stringResource(R.string.date_format)
            activeFilter.date.format(DateTimeFormatter.ofPattern(pattern))
        }
        else -> stringResource(R.string.filter_pick_date)
    }

    val filters = listOf(
        stringResource(R.string.filter_all) to ReceiptFilter.All,
        stringResource(R.string.filter_today) to ReceiptFilter.Today,
        stringResource(R.string.filter_week) to ReceiptFilter.LastWeek,
        stringResource(R.string.filter_month) to ReceiptFilter.LastMonth,
    )

    val ownershipChips = listOf(
        stringResource(R.string.filter_ownership_all) to ReceiptOwnershipFilter.All,
        stringResource(R.string.filter_favorites) to ReceiptOwnershipFilter.FavoritesOnly,
        stringResource(R.string.filter_pinned) to ReceiptOwnershipFilter.PinnedOnly,
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(filters) { (label, filter) ->
            val selected = activeFilter::class == filter::class
            FilterChip(
                selected = selected,
                onClick = { onFilterSelected(filter) },
                label = { Text(label) },
            )
        }
        items(
            count = 1,
            key = { "filter_by_date" },
        ) {
            val selected = activeFilter is ReceiptFilter.ByDate
            FilterChip(
                selected = selected,
                onClick = {
                    onOpenDatePicker()
                    showDatePicker = true
                },
                label = { Text(dateLabel) },
            )
        }
        items(
            ownershipChips,
            key = { it.second },
        ) { (label, own) ->
            FilterChip(
                selected = ownershipFilter == own,
                onClick = { onOwnershipSelected(own) },
                label = { Text(label) },
            )
        }
    }
}

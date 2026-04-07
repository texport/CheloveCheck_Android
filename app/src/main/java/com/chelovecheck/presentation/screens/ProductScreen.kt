package com.chelovecheck.presentation.screens

import android.util.Base64
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelovecheck.R
import com.chelovecheck.domain.model.ItemPurchaseRow
import com.chelovecheck.presentation.money.rememberProductDisplayMoney
import com.chelovecheck.presentation.viewmodel.ProductViewModel
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    encodedKey: String,
    onClose: () -> Unit,
    onOpenReceipt: (String) -> Unit,
    viewModel: ProductViewModel = hiltViewModel(),
) {
    val normalizedKey = decodeProductRouteKey(encodedKey)
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    LaunchedEffect(normalizedKey) {
        if (normalizedKey.isNotBlank()) {
            viewModel.load(normalizedKey)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val subtitle = stringResource(R.string.product_screen_subtitle)
                    Column {
                        Text(stringResource(R.string.title_product))
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                },
            )
        },
    ) { padding ->
        when {
            normalizedKey.isBlank() -> {
                Text(
                    stringResource(R.string.receipt_not_found),
                    modifier = Modifier.padding(padding).padding(16.dp),
                )
            }
            loading -> {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            rows.isEmpty() -> {
                Text(
                    stringResource(R.string.product_history_empty),
                    modifier = Modifier.padding(padding).padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            else -> {
                val pattern = stringResource(R.string.date_time_format)
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        rows,
                        key = { index, row ->
                            "${row.fiscalSign}_${row.dateTimeEpochMillis}_${index}_${row.sum}_${row.itemName}"
                        },
                    ) { _, row ->
                        PurchaseRow(
                            row = row,
                            datePattern = pattern,
                            viewModel = viewModel,
                            onOpenReceipt = { onOpenReceipt(row.fiscalSign) },
                        )
                    }
                }
            }
        }
    }
}

private fun decodeProductRouteKey(encodedKey: String): String {
    return try {
        val bytes = Base64.decode(
            encodedKey,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        String(bytes, StandardCharsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        ""
    }
}

@Composable
private fun PurchaseRow(
    row: ItemPurchaseRow,
    datePattern: String,
    viewModel: ProductViewModel,
    onOpenReceipt: () -> Unit,
) {
    val formatter = DateTimeFormatter.ofPattern(datePattern)
    val whenText = formatter.format(
        Instant.ofEpochMilli(row.dateTimeEpochMillis).atZone(ZoneId.systemDefault()),
    )
    val sumText = rememberProductDisplayMoney(row.sum, viewModel)
    ListItem(
        headlineContent = { Text(row.companyName, maxLines = 2) },
        supportingContent = { Text(whenText) },
        trailingContent = {
            Text(
                sumText,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clickable(onClick = onOpenReceipt),
    )
}

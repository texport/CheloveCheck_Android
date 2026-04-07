package com.chelovecheck.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelovecheck.R
import com.chelovecheck.presentation.components.M3MaxWidthColumn
import com.chelovecheck.presentation.viewmodel.ExchangeRatesViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeRatesScreen(
    onClose: () -> Unit,
    viewModel: ExchangeRatesViewModel = hiltViewModel(),
) {
    val snap by viewModel.snapshot.collectAsStateWithLifecycle()
    val zone = ZoneId.systemDefault()
    val datePattern = stringResource(R.string.date_time_format)
    val lastFmt = remember(datePattern) { DateTimeFormatter.ofPattern(datePattern) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_exchange_rates)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                },
            )
        },
    ) { padding ->
        M3MaxWidthColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.exchange_rates_source_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            val last = snap.lastUpdatedEpochMillis
            Text(
                text = if (last != null) {
                    stringResource(
                        R.string.exchange_rates_last_updated,
                        lastFmt.format(Instant.ofEpochMilli(last).atZone(zone)),
                    )
                } else {
                    stringResource(R.string.exchange_rates_never_updated)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            val rows = snap.tengePerUnitByCode.entries.sortedBy { it.key }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(rows, key = { it.key }) { (code, tenge) ->
                    ListItem(
                        headlineContent = { Text(code) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    R.string.exchange_rates_row_subtitle,
                                    tenge,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

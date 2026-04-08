package com.chelovecheck.presentation.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kotlin.OptIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chelovecheck.R
import com.chelovecheck.domain.model.AnalyticsPeriod
import com.chelovecheck.presentation.strings.analyticsPeriodLabel
import com.chelovecheck.presentation.viewmodel.AnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodChips(
    selected: AnalyticsPeriod,
    onSelected: (AnalyticsPeriod) -> Unit,
) {
    val periods = listOf(
        AnalyticsPeriod.ALL,
        AnalyticsPeriod.WEEK,
        AnalyticsPeriod.MONTH,
        AnalyticsPeriod.QUARTER,
        AnalyticsPeriod.YEAR,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(periods) { period ->
            FilterChip(
                selected = selected == period,
                onClick = { onSelected(period) },
                label = { Text(analyticsPeriodLabel(period)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            )
        }
    }
}

@Composable
internal fun SummaryCard(
    total: Double,
    receiptsCount: Int,
    average: Double,
    viewModel: AnalyticsViewModel,
) {
    val totalText = rememberAnalyticsDisplayMoney(total, viewModel)
    val avgText = rememberAnalyticsDisplayMoney(average, viewModel)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Text(
                text = stringResource(R.string.analytics_total_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = totalText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(
                    title = stringResource(R.string.analytics_receipts_count),
                    value = receiptsCount.toString(),
                )
                StatChip(
                    title = stringResource(R.string.analytics_average_receipt),
                    value = avgText,
                )
            }
        }
    }
}

@Composable
internal fun StatChip(title: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun SectionTitleWithAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    actionLabel: String?,
    onActionClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (actionLabel != null) {
            TextButton(onClick = onActionClick) {
                Text(actionLabel)
            }
        }
    }
}

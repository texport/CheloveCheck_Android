package com.chelovecheck.presentation.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chelovecheck.R
import com.chelovecheck.domain.model.CategoryItemTotal
import com.chelovecheck.domain.model.CategoryIds
import com.chelovecheck.domain.model.PendingCategoryItem
import com.chelovecheck.domain.utils.ItemNameNormalizer
import com.chelovecheck.presentation.viewmodel.AnalyticsViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CategoryResolveDialog(
    item: PendingCategoryItem,
    pickerGroupIds: List<String>,
    labelProvider: (String) -> String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    isResolving: Boolean,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.analytics_category_resolve_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = ItemNameNormalizer.cleanDisplayName(item.itemName),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (isResolving) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LoadingIndicator(modifier = Modifier.size(20.dp))
                        Text(
                            text = stringResource(R.string.analytics_resolve_saving),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    pickerGroupIds.forEach { groupId ->
                        TextButton(
                            onClick = { onSelect(groupId) },
                            enabled = !isResolving,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = labelProvider(groupId).trim(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    TextButton(
                        onClick = { onSelect(CategoryIds.UNCATEGORIZED) },
                        enabled = !isResolving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = labelProvider(CategoryIds.UNCATEGORIZED).trim(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !isResolving) {
                Text(stringResource(R.string.action_later))
            }
        },
    )
}

@Composable
internal fun PendingCategoriesDialog(
    items: List<PendingCategoryItem>,
    rollupPreviewLabel: (String) -> String,
    onSelect: (PendingCategoryItem) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.analytics_pending_title)) },
        text = {
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.analytics_pending_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items.forEach { item ->
                        val topCandidate = item.candidates.maxByOrNull { it.score }
                        TextButton(
                            onClick = { onSelect(item) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = ItemNameNormalizer.cleanDisplayName(item.itemName),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val net = item.networkKey
                                if (!net.isNullOrBlank()) {
                                    Text(
                                        text = stringResource(R.string.analytics_pending_network, net),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (topCandidate != null) {
                                    Text(
                                        text = rollupPreviewLabel(topCandidate.categoryId),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryItemsDialog(
    title: String,
    items: List<CategoryItemTotal>,
    viewModel: AnalyticsViewModel,
    onItemClick: (CategoryItemTotal) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(title) },
        text = {
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.analytics_category_items_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items.forEach { item ->
                        val amountText = rememberAnalyticsDisplayMoney(item.amount, viewModel)
                        Card(
                            onClick = { onItemClick(item) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = ItemNameNormalizer.cleanDisplayName(item.itemName),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = amountText,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (item.count > 1) {
                                    Text(
                                        text = stringResource(R.string.analytics_item_count, item.count),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

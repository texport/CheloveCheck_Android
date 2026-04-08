package com.chelovecheck.presentation.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chelovecheck.presentation.strings.formatDecimal
import com.chelovecheck.presentation.strings.unitShortLabel
import com.chelovecheck.presentation.money.rememberReceiptDisplayMoney
import com.chelovecheck.domain.utils.ItemNameNormalizer
import com.chelovecheck.domain.model.analyticsSourceName
import com.chelovecheck.presentation.viewmodel.ReceiptItemUi
import com.chelovecheck.presentation.viewmodel.ReceiptViewModel
import com.chelovecheck.presentation.utils.buildSearchHighlightedText
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.chelovecheck.R

fun LazyListScope.receiptItemsSection(
    items: List<ReceiptItemUi>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onToggleSelect: (Long) -> Unit,
    viewModel: ReceiptViewModel,
    onOpenProduct: ((String) -> Unit)? = null,
    searchHighlight: String? = null,
) {
    itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
        val sumText = rememberReceiptDisplayMoney(item.item.sum, viewModel)
        ReceiptItemCard(
            item = item,
            sumDisplayText = sumText,
            selected = selectedIds.contains(item.id),
            selectionEnabled = true,
            isSelectionMode = isSelectionMode,
            modifier = Modifier
                .animateItem()
                .clip(RoundedCornerShape(18.dp)),
            onToggleSelect = { onToggleSelect(item.id) },
            onOpenProduct = onOpenProduct,
            searchHighlight = searchHighlight,
        )
    }
}


@Composable
fun ReceiptItemCard(
    item: ReceiptItemUi,
    sumDisplayText: String,
    selected: Boolean,
    selectionEnabled: Boolean,
    isSelectionMode: Boolean,
    modifier: Modifier = Modifier,
    onToggleSelect: () -> Unit,
    onOpenProduct: ((String) -> Unit)? = null,
    searchHighlight: String? = null,
) {
    val isSelectable = selectionEnabled
    val containerColor = when {
        selected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val border = if (selected) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }

    Card(
        modifier = modifier.combinedClickable(
            enabled = isSelectable || (!isSelectionMode && onOpenProduct != null),
            role = Role.Button,
            onClickLabel = stringResource(R.string.action_select_item),
            onLongClickLabel = stringResource(R.string.action_select_item),
            onClick = {
                when {
                    isSelectionMode -> onToggleSelect()
                    onOpenProduct != null -> {
                        val key = ItemNameNormalizer.normalizeForMatch(item.item.analyticsSourceName())
                        if (key.isNotBlank()) onOpenProduct.invoke(key)
                    }
                }
            },
            onLongClick = onToggleSelect,
        ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        val unit = unitShortLabel(item.item.unit)
        val countFormatted = formatDecimal(item.item.count)
        val priceFormatted = formatDecimal(item.item.price)
        val countText = if (unit.isBlank()) {
            "${countFormatted} × ${priceFormatted}"
        } else {
            "${countFormatted} ${unit} × ${priceFormatted}"
        }
        ListItem(
            headlineContent = {
                val name = ItemNameNormalizer.cleanDisplayName(item.item.name)
                val hlBg = MaterialTheme.colorScheme.secondaryContainer
                val hlFg = MaterialTheme.colorScheme.onSecondaryContainer
                if (searchHighlight.isNullOrBlank()) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = buildSearchHighlightedText(name, searchHighlight, hlBg, hlFg),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            supportingContent = {
                val hlBg = MaterialTheme.colorScheme.secondaryContainer
                val hlFg = MaterialTheme.colorScheme.onSecondaryContainer
                Text(
                    text = buildSearchHighlightedText(countText, searchHighlight, hlBg, hlFg),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                val hlBg = MaterialTheme.colorScheme.secondaryContainer
                val hlFg = MaterialTheme.colorScheme.onSecondaryContainer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildSearchHighlightedText(sumDisplayText, searchHighlight, hlBg, hlFg),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    if (!isSelectionMode && onOpenProduct != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Outlined.NavigateNext,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

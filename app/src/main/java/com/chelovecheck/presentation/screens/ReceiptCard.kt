package com.chelovecheck.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.chelovecheck.R
import com.chelovecheck.domain.model.ReceiptListItem
import com.chelovecheck.presentation.utils.buildSearchHighlightedText
import com.chelovecheck.presentation.strings.ofdLabel
import com.chelovecheck.presentation.strings.operationLabel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReceiptCard(
    item: ReceiptListItem,
    totalFormatted: String,
    searchHighlight: String? = null,
    onOpenReceipt: (String) -> Unit,
    onLongPress: () -> Unit,
) {
    val summary = item.summary
    val hlBg = MaterialTheme.colorScheme.secondaryContainer
    val hlFg = MaterialTheme.colorScheme.onSecondaryContainer
    val datePattern = stringResource(R.string.date_time_format)
    val date = remember(summary.dateTime, datePattern) { formatDate(summary.dateTime, datePattern) }
    val pinnedShape = RoundedCornerShape(20.dp)
    val cardModifier = Modifier
        .fillMaxWidth()
        .then(
            if (summary.isPinned) {
                Modifier.border(
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    pinnedShape,
                )
            } else {
                Modifier
            },
        )
        .combinedClickable(
            role = Role.Button,
            onClickLabel = stringResource(R.string.action_open_receipt),
            onLongClickLabel = stringResource(R.string.cd_receipt_long_press),
            onClick = { onOpenReceipt(summary.fiscalSign) },
            onLongClick = onLongPress,
        )

    Card(
        modifier = cardModifier,
        shape = pinnedShape,
        colors = CardDefaults.cardColors(
            containerColor = if (summary.isPinned) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (summary.isPinned) 6.dp else 4.dp,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildSearchHighlightedText(
                            item.displayName.ifBlank { summary.companyName },
                            searchHighlight,
                            hlBg,
                            hlFg,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildSearchHighlightedText(
                            ofdLabel(summary.ofd),
                            searchHighlight,
                            hlBg,
                            hlFg,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = totalFormatted,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .widthIn(max = 140.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.weight(1f, fill = false),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = buildSearchHighlightedText(
                            operationLabel(summary.typeOperation),
                            searchHighlight,
                            hlBg,
                            hlFg,
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (summary.itemsCount > 0) {
                    Text(
                        text = stringResource(R.string.positions_short, summary.itemsCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun formatDate(instant: java.time.Instant, pattern: String): String {
    val formatter = DateTimeFormatter.ofPattern(pattern)
    return formatter.format(instant.atZone(ZoneId.systemDefault()))
}

package com.chelovecheck.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chelovecheck.R
import com.chelovecheck.presentation.utils.rememberHapticPerformer
import com.chelovecheck.domain.model.ReceiptListItem
import kotlinx.coroutines.delay

@Composable
fun SwipeableReceiptCard(
    item: ReceiptListItem,
    totalFormatted: String,
    searchHighlight: String? = null,
    onOpenReceipt: (String) -> Unit,
    onRequestDelete: () -> Unit,
    onOpenMap: () -> Unit,
    onSwipeLog: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val haptics = rememberHapticPerformer()
    var menuExpanded by remember { mutableStateOf(false) }
    var actionInFlight by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<SwipeAction?>(null) }
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { total -> total * 0.2f },
    )

    LaunchedEffect(dismissState.targetValue, pendingAction, actionInFlight) {
        val value = dismissState.targetValue
        if (value == SwipeToDismissBoxValue.Settled) return@LaunchedEffect
        if (pendingAction != null || actionInFlight) return@LaunchedEffect
        pendingAction = when (value) {
            SwipeToDismissBoxValue.StartToEnd -> SwipeAction.OpenMap
            SwipeToDismissBoxValue.EndToStart -> SwipeAction.Delete
            SwipeToDismissBoxValue.Settled -> null
        }
        onSwipeLog("trigger action=$value")
    }

    LaunchedEffect(pendingAction) {
        val action = pendingAction ?: return@LaunchedEffect
        actionInFlight = true
        when (action) {
            SwipeAction.OpenMap -> {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                onSwipeLog("action=OpenMap")
                onOpenMap()
            }
            SwipeAction.Delete -> {
                haptics(HapticFeedbackType.GestureThresholdActivate)
                onSwipeLog("action=Delete")
                onRequestDelete()
            }
        }
        delay(120)
        val resetResult = runCatching { dismissState.reset() }
        onSwipeLog("reset result=${resetResult.exceptionOrNull()?.javaClass?.simpleName ?: "ok"}")
        onSwipeLog("action done")
        pendingAction = null
        actionInFlight = false
    }

    val summary = item.summary

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            SwipeActionBackground(dismissState.dismissDirection)
        },
    ) {
        Box {
            ReceiptCard(
                item = item,
                totalFormatted = totalFormatted,
                searchHighlight = searchHighlight,
                onOpenReceipt = onOpenReceipt,
                onLongPress = {
                    haptics(HapticFeedbackType.LongPress)
                    menuExpanded = true
                },
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (summary.isFavorite) {
                                stringResource(R.string.menu_receipt_unfavorite)
                            } else {
                                stringResource(R.string.menu_receipt_favorite)
                            },
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (summary.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        haptics(HapticFeedbackType.GestureThresholdActivate)
                        onToggleFavorite()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (summary.isPinned) {
                                stringResource(R.string.menu_receipt_unpin)
                            } else {
                                stringResource(R.string.menu_receipt_pin)
                            },
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (summary.isPinned) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        haptics(HapticFeedbackType.GestureThresholdActivate)
                        onTogglePin()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_open_map)) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Place,
                            contentDescription = stringResource(R.string.action_open_map),
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        haptics(HapticFeedbackType.GestureThresholdActivate)
                        onOpenMap()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete)) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        haptics(HapticFeedbackType.GestureThresholdActivate)
                        onRequestDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun SwipeActionBackground(direction: SwipeToDismissBoxValue?) {
    if (direction == null || direction == SwipeToDismissBoxValue.Settled) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val (color, icon, tint, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> SwipeBackgroundConfig(
            MaterialTheme.colorScheme.primaryContainer,
            Icons.Outlined.Place,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Alignment.CenterStart,
        )
        SwipeToDismissBoxValue.EndToStart -> SwipeBackgroundConfig(
            MaterialTheme.colorScheme.errorContainer,
            Icons.Outlined.Delete,
            MaterialTheme.colorScheme.onErrorContainer,
            Alignment.CenterEnd,
        )
        SwipeToDismissBoxValue.Settled -> SwipeBackgroundConfig(
            MaterialTheme.colorScheme.surface,
            Icons.Outlined.Delete,
            MaterialTheme.colorScheme.onSurface,
            Alignment.CenterEnd,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        contentAlignment = alignment,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
        )
    }
}

private data class SwipeBackgroundConfig(
    val color: androidx.compose.ui.graphics.Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: androidx.compose.ui.graphics.Color,
    val alignment: Alignment,
)

private enum class SwipeAction {
    OpenMap,
    Delete,
}

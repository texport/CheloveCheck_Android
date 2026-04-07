package com.chelovecheck.presentation.components

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Limits readable line length on expanded window sizes (M3 / large-screen guidance).
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun M3MaxWidthColumn(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 960.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val activity = LocalContext.current as Activity
    val windowSizeClass = calculateWindowSizeClass(activity)
    val widthMod = if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
        Modifier.widthIn(max = maxWidth)
    } else {
        Modifier
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(widthMod)
                .fillMaxWidth(),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}

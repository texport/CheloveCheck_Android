package com.chelovecheck.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.appcompat.app.AppCompatActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelovecheck.domain.repository.AppLocaleApplicator
import com.chelovecheck.domain.model.ThemeMode
import com.chelovecheck.presentation.navigation.AppRoot
import com.chelovecheck.presentation.theme.CheloveCheckTheme
import com.chelovecheck.presentation.utils.HapticsProvider
import com.chelovecheck.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var appLocaleApplicator: AppLocaleApplicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val language by viewModel.language.collectAsStateWithLifecycle()
            val colorSource by viewModel.colorSource.collectAsStateWithLifecycle()
            val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()
            val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
            val darkTheme = resolveDarkTheme(themeMode)

            CheloveCheckTheme(
                darkTheme = darkTheme,
                colorSource = colorSource,
                accentColor = accentColor,
            ) {
                androidx.compose.runtime.LaunchedEffect(language) {
                    appLocaleApplicator.apply(language)
                }
                HapticsProvider(enabled = hapticsEnabled) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun resolveDarkTheme(mode: ThemeMode): Boolean {
    return when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}

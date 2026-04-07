package com.chelovecheck.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelovecheck.domain.model.AccentColor
import com.chelovecheck.domain.model.AppLanguage
import com.chelovecheck.domain.model.ColorSource
import com.chelovecheck.domain.model.ThemeMode
import com.chelovecheck.domain.usecase.ObserveAccentColorUseCase
import com.chelovecheck.domain.usecase.ObserveLanguageUseCase
import com.chelovecheck.domain.usecase.ObserveColorSourceUseCase
import com.chelovecheck.domain.usecase.ObserveHapticsEnabledUseCase
import com.chelovecheck.domain.usecase.ObserveThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
    observeThemeModeUseCase: ObserveThemeModeUseCase,
    observeLanguageUseCase: ObserveLanguageUseCase,
    observeColorSourceUseCase: ObserveColorSourceUseCase,
    observeAccentColorUseCase: ObserveAccentColorUseCase,
    observeHapticsEnabledUseCase: ObserveHapticsEnabledUseCase,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = observeThemeModeUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val language: StateFlow<AppLanguage> = observeLanguageUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.SYSTEM)

    val colorSource: StateFlow<ColorSource> = observeColorSourceUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ColorSource.DYNAMIC)

    val accentColor: StateFlow<AccentColor> = observeAccentColorUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccentColor.PURPLE)

    val hapticsEnabled: StateFlow<Boolean> = observeHapticsEnabledUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
}

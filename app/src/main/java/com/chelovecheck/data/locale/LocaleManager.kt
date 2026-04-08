package com.chelovecheck.data.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.chelovecheck.domain.model.AppLanguage
import com.chelovecheck.domain.repository.AppLocaleApplicator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleManager @Inject constructor() : AppLocaleApplicator {
    override fun apply(language: AppLanguage) {
        val locales = if (language == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}

package com.chelovecheck.domain.repository

import com.chelovecheck.domain.model.AppLanguage

fun interface AppLocaleApplicator {
    fun apply(language: AppLanguage)
}

package com.chelovecheck.domain.model

import com.chelovecheck.domain.model.AppLanguage

data class CoicopCategory(
    val id: String,
    val level: Int,
    val parentId: String?,
    val names: Map<String, String>,
    val aliases: Map<String, List<String>>,
) {
    fun label(languageTag: String): String {
        return names[languageTag]
            ?: names[AppLanguage.ENGLISH.tag]
            ?: names.values.firstOrNull()
            ?: id
    }

    fun allLabels(): List<String> = names.values.toList()
}

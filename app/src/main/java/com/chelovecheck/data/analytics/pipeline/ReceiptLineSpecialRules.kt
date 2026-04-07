package com.chelovecheck.data.analytics.pipeline

internal object ReceiptLineSpecialRules {
    private val technicalPlaceholder = Regex("""(?i)^(товар|позиция|item|product)$""")
    private val serviceModifier = Regex(
        """(?i)(без\s+льда|без\s+экстра|добавк|увелич|комбо\s*up|extra\s+sauce|no\s+ice)""",
    )
    private val foodServiceMenu = Regex(
        """(?i)(^|\s)(воппер|вопп|whopper|биф\s*ролл|хачапури|цезарь|комбо|крылышк\S*|бургер\s*меню|burger\s*meal|обед\s+\S*вопп\S*|двойн\S*\s+вопп\S*)(\s|$)""",
    )

    fun isTechnicalPlaceholder(normalized: String): Boolean =
        normalized.isNotBlank() && technicalPlaceholder.matches(normalized)

    fun isServiceModifier(normalized: String): Boolean =
        normalized.isNotBlank() && serviceModifier.containsMatchIn(normalized)

    fun isFoodServiceMenu(normalized: String): Boolean =
        normalized.isNotBlank() && foodServiceMenu.containsMatchIn(normalized)
}

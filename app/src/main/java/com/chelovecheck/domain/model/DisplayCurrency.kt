package com.chelovecheck.domain.model

/** ISO-style codes for display; amounts in DB remain in KZT. */
enum class DisplayCurrency(val code: String) {
    KZT("KZT"),
    USD("USD"),
    EUR("EUR"),
    RUB("RUB"),
    ;

    fun displaySymbol(): String = when (this) {
        KZT -> "₸"
        USD -> "$"
        EUR -> "€"
        RUB -> "₽"
    }
}

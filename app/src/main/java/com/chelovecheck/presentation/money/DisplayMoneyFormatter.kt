package com.chelovecheck.presentation.money

import com.chelovecheck.domain.model.DisplayCurrency
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Formats a value already in [displayCurrency] units (after conversion from KZT). */
object DisplayMoneyFormatter {
    fun format(amount: Double, displayCurrency: DisplayCurrency): String {
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        val df = DecimalFormat("#,##0.00", symbols)
        return "${df.format(amount)} ${displayCurrency.displaySymbol()}"
    }
}

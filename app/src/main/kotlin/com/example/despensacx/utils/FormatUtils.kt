package com.example.despensacx.utils

import java.text.NumberFormat
import java.util.Locale

object FormatUtils {
    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))

    @JvmStatic
    fun formatCurrency(amount: Double): String {
        synchronized(currencyFormatter) {
            return currencyFormatter.format(amount)
        }
    }
}

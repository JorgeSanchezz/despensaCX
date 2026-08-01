package com.example.despensacx;

import java.text.NumberFormat;
import java.util.Locale;

public class FormatUtils {

    private static final NumberFormat currencyFormatter;

    static {
        // Formato para México / Español ($4,000.00)
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
    }

    public static String formatCurrency(double amount) {
        synchronized (currencyFormatter) {
            return currencyFormatter.format(amount);
        }
    }
}
package dev.batipy.rungo.ui.common

import java.util.Locale

fun currencySymbol(currency: String): String = when (currency) {
    "try" -> "₺"
    "syp" -> "S£"
    else -> "$"
}

/**
 * The backend used to compute order totals using a SYP rate that was 100x too
 * large (same underlying issue as /api/v1/exchange-rate/'s syp_rate — see
 * CatalogRepository), so stored SYP amounts came back 100x inflated. That's
 * now fixed backend-side, so amounts are used as-is.
 */
fun formatOrderAmount(amount: String?, currency: String): String {
    val value = amount?.toDoubleOrNull() ?: return "${currencySymbol(currency)}${amount ?: "0.00"}"
    return "${currencySymbol(currency)}${String.format(Locale.US, "%.2f", value)}"
}

package dev.batipy.rungo.ui.common

import java.util.Locale

fun currencySymbol(currency: String): String = when (currency) {
    "try" -> "₺"
    "syp" -> "S£"
    else -> "$"
}

/**
 * The backend's own SYP rate is 100x too large when it computes order totals
 * (same underlying issue as /api/v1/exchange-rate/'s syp_rate — see
 * CatalogRepository), so stored SYP amounts come back 100x inflated. Dividing
 * by 100 here keeps every screen consistent with the price shown to the
 * client at order-creation time.
 */
fun formatOrderAmount(amount: String?, currency: String): String {
    val value = amount?.toDoubleOrNull() ?: return "${currencySymbol(currency)}${amount ?: "0.00"}"
    val corrected = if (currency == "syp") value / 100 else value
    return "${currencySymbol(currency)}${String.format(Locale.US, "%.2f", corrected)}"
}

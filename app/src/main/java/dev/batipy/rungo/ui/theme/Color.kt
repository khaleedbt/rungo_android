package dev.batipy.rungo.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val RunGoBackground = Color(0xFF1B222D)
val RunGoField = Color(0xFF2B3440)
val RunGoAccent = Color(0xFF4C5FE0)
val RunGoAccentLight = Color(0xFFAEBBF7)

// New brand colors (from the updated logo) — trying these out on the courier
// screens first before rolling out app-wide. RunGoOnBrandOrange is black
// rather than white: the orange is much lighter than RunGoAccent, so white
// text/icons on top of it fail contrast (see theme_onprimary_fix history).
val RunGoBrandOrange = Color(0xFFFBB03B)
val RunGoOnBrandOrange = Color(0xFF000000)

// Light-theme trial palette — courier screens only for now (see
// CourierOrdersScreen.kt / CourierOrderDetailScreen.kt). RunGoBrandOrange
// itself reads poorly as *text* on a light background (same contrast issue
// as white-on-orange, just inverted), so RunGoLightAccentText is a darker
// burnt-orange for that specific case; solid orange fills (buttons, badges)
// keep using RunGoBrandOrange + RunGoOnBrandOrange as-is.
val RunGoLightBackground = Color(0xFFF7F4EF)
val RunGoLightField = Color(0xFFFFFFFF)
val RunGoLightSurfaceMuted = Color(0xFFEDEAE3)
val RunGoLightTextPrimary = Color(0xFF1C1B18)
val RunGoLightTextSecondary = Color(0xFF7A7468)
val RunGoLightAccentText = Color(0xFFC97C1F)
val RunGoTextPrimary = Color(0xFFF2F3F5)
val RunGoTextSecondary = Color(0xFF9AA5B1)
val RunGoPlaceholder = Color(0xFF6B7684)

package dev.batipy.rungo.data.shop

import android.content.Context

private const val PREFS_NAME = "shop_display_prefs"
private const val KEY_GRID_VIEW = "grid_view"

/**
 * Persists the Магазин grid/list toggle across full app restarts (not just
 * navigation or backgrounding) — plain SharedPreferences since it's a single
 * boolean read synchronously once per process, no need for DataStore's Flow
 * ceremony here.
 */
class ShopDisplayPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isGridView: Boolean
        get() = prefs.getBoolean(KEY_GRID_VIEW, false)
        set(value) = prefs.edit().putBoolean(KEY_GRID_VIEW, value).apply()
}

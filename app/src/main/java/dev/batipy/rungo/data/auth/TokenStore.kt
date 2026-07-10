package dev.batipy.rungo.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

private val Context.authDataStore by preferencesDataStore(name = "auth")

data class TokenPair(val access: String, val refresh: String)

// `_tokens` is written exclusively from saveTokens/updateAccessToken/clearTokens/
// awaitInitialTokens below, in the same coroutine that performs the DataStore edit.
// It used to also be kept in sync via a background collector on
// context.authDataStore.data, but that collector runs on its own dispatch and could
// resume out of order with saveTokens/clearTokens — e.g. a logout's "cleared" emission
// landing *after* the following login's saveTokens() had already set the new tokens,
// silently reverting _tokens to null right after login and breaking anything (like FCM
// device-token registration) that ran in that window. A single synchronous writer avoids
// that race entirely.
class TokenStore(private val context: Context) {

    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")

    private val _tokens = MutableStateFlow<TokenPair?>(null)

    val currentTokens: TokenPair?
        get() = _tokens.value

    val tokens: StateFlow<TokenPair?> = _tokens.asStateFlow()

    suspend fun awaitInitialTokens(): TokenPair? {
        _tokens.value?.let { return it }
        val prefs = context.authDataStore.data.first()
        val access = prefs[accessKey]
        val refresh = prefs[refreshKey]
        val loaded = if (access != null && refresh != null) TokenPair(access, refresh) else null
        _tokens.value = loaded
        return loaded
    }

    suspend fun saveTokens(tokens: TokenPair) {
        context.authDataStore.edit { prefs ->
            prefs[accessKey] = tokens.access
            prefs[refreshKey] = tokens.refresh
        }
        _tokens.value = tokens
    }

    suspend fun updateAccessToken(access: String) {
        val refresh = _tokens.value?.refresh ?: return
        saveTokens(TokenPair(access, refresh))
    }

    suspend fun clearTokens() {
        context.authDataStore.edit { it.clear() }
        _tokens.value = null
    }
}

package dev.batipy.rungo.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.io.IOException

private val Context.authDataStore by preferencesDataStore(name = "auth")

data class TokenPair(val access: String, val refresh: String)

class TokenStore(private val context: Context, applicationScope: CoroutineScope) {

    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")

    private val _tokens = MutableStateFlow<TokenPair?>(null)

    init {
        context.authDataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { prefs ->
                val access = prefs[accessKey]
                val refresh = prefs[refreshKey]
                if (access != null && refresh != null) TokenPair(access, refresh) else null
            }
            .onEach { _tokens.value = it }
            .launchIn(applicationScope)
    }

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

package dev.batipy.rungo.ui.register

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.R
import dev.batipy.rungo.data.auth.AuthRepository
import dev.batipy.rungo.data.auth.RegisterResult
import dev.batipy.rungo.data.catalog.CatalogRepository
import dev.batipy.rungo.data.network.dto.CityDto
import dev.batipy.rungo.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RegisterUiState {
    data object Idle : RegisterUiState
    data object Loading : RegisterUiState
    data object Success : RegisterUiState
    data class Error(val message: String) : RegisterUiState
}

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val catalogRepository: CatalogRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _cities = MutableStateFlow<List<CityDto>>(emptyList())
    val cities: StateFlow<List<CityDto>> = _cities.asStateFlow()

    init {
        viewModelScope.launch {
            _cities.value = catalogRepository.getCities().getOrDefault(emptyList())
        }
    }

    fun register(
        username: String,
        password: String,
        password2: String,
        fullName: String,
        phone: String,
        cityId: Int?
    ) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = RegisterUiState.Error(context.getString(R.string.login_error_empty))
            return
        }
        if (password.length < 8) {
            _uiState.value = RegisterUiState.Error(context.getString(R.string.register_password_min_length))
            return
        }
        if (password != password2) {
            _uiState.value = RegisterUiState.Error(context.getString(R.string.register_password_mismatch))
            return
        }
        _uiState.value = RegisterUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.register(username, password, password2, fullName, phone)) {
                is RegisterResult.Success -> {
                    // RegisterSerializer doesn't accept city directly — set it via the
                    // same profile-update endpoint used elsewhere, right after the new
                    // account's tokens are saved. Best-effort: a failure here shouldn't
                    // block the (already successful) registration.
                    if (cityId != null) {
                        profileRepository.updateCity(cityId)
                    }
                    _uiState.value = RegisterUiState.Success
                }
                is RegisterResult.Error -> _uiState.value = RegisterUiState.Error(result.message)
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository,
        private val catalogRepository: CatalogRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RegisterViewModel(authRepository, profileRepository, catalogRepository, context) as T
        }
    }
}

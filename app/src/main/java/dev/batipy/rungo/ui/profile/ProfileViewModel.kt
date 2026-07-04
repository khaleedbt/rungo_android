package dev.batipy.rungo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.data.network.dto.LocationDto
import dev.batipy.rungo.data.network.dto.UserDto
import dev.batipy.rungo.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: UserDto, val locations: List<LocationDto>) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            _uiState.value = fetch()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _uiState.value = fetch()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetch(): ProfileUiState {
        val user = repository.getMe().getOrNull()
        val locations = repository.getLocations().getOrNull()
        return if (user != null && locations != null) {
            ProfileUiState.Success(user, locations)
        } else {
            ProfileUiState.Error("Не удалось загрузить профиль")
        }
    }

    fun deleteLocation(id: Int) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        viewModelScope.launch {
            repository.deleteLocation(id)
                .onSuccess {
                    _uiState.value = current.copy(locations = current.locations.filterNot { it.id == id })
                }
                .onFailure { _message.value = "Не удалось удалить локацию" }
        }
    }

    fun requestLocationViaBot() {
        viewModelScope.launch {
            repository.requestLocationViaBot()
                .onSuccess { _message.value = "Запрос отправлен в Telegram-бот" }
                .onFailure { _message.value = "Не удалось отправить запрос" }
        }
    }

    fun setLanguage(lang: String) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        viewModelScope.launch {
            repository.updateLanguage(lang)
                .onSuccess { updatedUser -> _uiState.value = current.copy(user = updatedUser) }
                .onFailure { _message.value = "Не удалось сменить язык" }
        }
    }

    fun sendSupportMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendSupportMessage(text)
                .onSuccess { _message.value = "Сообщение отправлено оператору" }
                .onFailure { _message.value = "Не удалось отправить сообщение" }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    class Factory(private val repository: ProfileRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(repository) as T
        }
    }
}

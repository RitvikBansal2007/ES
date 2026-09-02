package ac.iiit.rtltutor.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ac.iiit.rtltutor.data.UserRepository

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    data class Success(val displayName: String) : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

class RegisterViewModel : ViewModel() {

    private val _uiState = MutableLiveData<RegisterUiState>(RegisterUiState.Idle)
    val uiState: LiveData<RegisterUiState> = _uiState

    fun register(username: String, displayName: String, password: String, confirmPassword: String) {
        when {
            username.isBlank()         -> { _uiState.value = RegisterUiState.Error("Username required"); return }
            username.length < 3        -> { _uiState.value = RegisterUiState.Error("Username must be at least 3 characters"); return }
            password.length < 6        -> { _uiState.value = RegisterUiState.Error("Password must be at least 6 characters"); return }
            password != confirmPassword -> { _uiState.value = RegisterUiState.Error("Passwords do not match"); return }
        }

        _uiState.value = RegisterUiState.Loading

        val name = displayName.trim().ifBlank { username.trim() }
        val user = UserRepository.createUser(username.trim(), name, password)

        _uiState.value = if (user != null) {
            RegisterUiState.Success(user.displayName)
        } else {
            RegisterUiState.Error("Username \"$username\" is already taken")
        }
    }

    fun resetState() { _uiState.value = RegisterUiState.Idle }
}

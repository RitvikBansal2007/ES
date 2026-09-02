package ac.iiit.rtltutor.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ac.iiit.rtltutor.data.UserRepository
import ac.iiit.rtltutor.models.UserRole

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class StudentSuccess(val displayName: String) : LoginUiState()
    object AdminSuccess : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel : ViewModel() {

    private val _uiState = MutableLiveData<LoginUiState>(LoginUiState.Idle)
    val uiState: LiveData<LoginUiState> = _uiState

    fun login(username: String, password: String) {
        if (username.isBlank()) {
            _uiState.value = LoginUiState.Error("Username cannot be empty")
            return
        }
        if (password.isBlank()) {
            _uiState.value = LoginUiState.Error("Password cannot be empty")
            return
        }

        _uiState.value = LoginUiState.Loading

        val user = UserRepository.login(username, password)
        if (user == null) {
            _uiState.value = LoginUiState.Error("Invalid username or password")
            return
        }

        _uiState.value = when (user.role) {
            UserRole.ADMIN   -> LoginUiState.AdminSuccess
            UserRole.STUDENT -> LoginUiState.StudentSuccess(user.displayName)
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}

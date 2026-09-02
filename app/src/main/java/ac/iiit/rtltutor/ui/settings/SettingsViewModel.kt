package ac.iiit.rtltutor.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    private val _displayName = MutableLiveData("—")
    val displayName: LiveData<String> = _displayName

    private val _role = MutableLiveData("Student")
    val role: LiveData<String> = _role

    private val _aiModelName = MutableLiveData("Llama 3.2 3B (mock)")
    val aiModelName: LiveData<String> = _aiModelName

    private val _rtlUrl = MutableLiveData("rtl.iiit.ac.in")
    val rtlUrl: LiveData<String> = _rtlUrl

    private val _notificationsEnabled = MutableLiveData(true)
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    /** Called by SettingsFragment once the real user is known. */
    fun setUser(displayName: String, role: String) {
        _displayName.value = displayName
        _role.value = role
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }
}

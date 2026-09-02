package ac.iiit.rtltutor.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ac.iiit.rtltutor.models.ExperimentState
import ac.iiit.rtltutor.models.UserLearningProfile

class HomeViewModel : ViewModel() {

    private val _displayName = MutableLiveData("Ritvik")
    val displayName: LiveData<String> = _displayName

    private val _streakDays = MutableLiveData(7)
    val streakDays: LiveData<Int> = _streakDays

    private val _sessionCount = MutableLiveData(24)
    val sessionCount: LiveData<Int> = _sessionCount

    private val _currentBloomLevel = MutableLiveData(3)
    val currentBloomLevel: LiveData<Int> = _currentBloomLevel

    private val _currentKolbStage = MutableLiveData("LEARN")
    val currentKolbStage: LiveData<String> = _currentKolbStage

    private val _rtlConnected = MutableLiveData(true)
    val rtlConnected: LiveData<Boolean> = _rtlConnected

    private val _latestExperimentState = MutableLiveData<ExperimentState?>()
    val latestExperimentState: LiveData<ExperimentState?> = _latestExperimentState

    private val _weakTopics = MutableLiveData(listOf("RC Transients", "Impedance", "Thevenin's"))
    val weakTopics: LiveData<List<String>> = _weakTopics

    private val _learningProfile = MutableLiveData<UserLearningProfile?>()
    val learningProfile: LiveData<UserLearningProfile?> = _learningProfile

    fun setDisplayName(name: String) {
        _displayName.value = name
    }

    fun refreshData() {
        // TODO: Load from repository
    }

    fun updateRtlState(state: ExperimentState) {
        _latestExperimentState.value = state
    }
}

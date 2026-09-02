package ac.iiit.rtltutor.ui.rtllive

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ac.iiit.rtltutor.models.ExperimentState

class RtlLiveViewModel : ViewModel() {

    private val _currentState = MutableLiveData<ExperimentState?>()
    val currentState: LiveData<ExperimentState?> = _currentState

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private val _stateHistory = MutableLiveData<List<ExperimentState>>(emptyList())
    val stateHistory: LiveData<List<ExperimentState>> = _stateHistory

    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    fun toggleSimulation() {
        val running = _isRunning.value ?: false
        if (running) stopSimulation() else startSimulation()
    }

    fun startSimulation() {
        _isRunning.value = true
        _isConnected.value = true
        // TODO: delegate to RTLSimulator
        // Stub: push a sample state
        val state = ExperimentState(
            voltage = 5.0,
            current = 2.5,
            frequency = 100.0,
            timestamp = System.currentTimeMillis(),
            rawJson = """{"v":5.0,"i":2.5,"f":100.0}"""
        )
        _currentState.value = state
        _stateHistory.value = listOf(state)
    }

    fun stopSimulation() {
        _isRunning.value = false
        // TODO: RTLSimulator.stop()
    }

    fun onNewState(state: ExperimentState) {
        _currentState.value = state
        val hist = _stateHistory.value.orEmpty().takeLast(99) + state
        _stateHistory.value = hist
    }

    override fun onCleared() {
        super.onCleared()
        stopSimulation()
    }
}

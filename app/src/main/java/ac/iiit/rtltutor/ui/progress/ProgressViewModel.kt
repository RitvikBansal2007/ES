package ac.iiit.rtltutor.ui.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ac.iiit.rtltutor.models.UserLearningProfile

class ProgressViewModel : ViewModel() {

    private val _profile = MutableLiveData<UserLearningProfile?>()
    val profile: LiveData<UserLearningProfile?> = _profile

    private val _bloomMastery = MutableLiveData<Map<Int, Float>>(
        mapOf(1 to 0.85f, 2 to 0.72f, 3 to 0.55f, 4 to 0.38f, 5 to 0.20f, 6 to 0.08f)
    )
    val bloomMastery: LiveData<Map<Int, Float>> = _bloomMastery

    init {
        loadProfile()
    }

    private fun loadProfile() {
        // TODO: load from UserRepository
        _profile.value = UserLearningProfile(
            userId = "current_user",
            bloomMastery = mapOf(1 to 0.85f, 2 to 0.72f, 3 to 0.55f, 4 to 0.38f, 5 to 0.20f, 6 to 0.08f),
            weakTopics = listOf("RC Transients", "Impedance", "Thevenin's"),
            strongTopics = listOf("Ohm's Law", "Kirchhoff's Laws"),
            sessionCount = 24
        )
    }
}

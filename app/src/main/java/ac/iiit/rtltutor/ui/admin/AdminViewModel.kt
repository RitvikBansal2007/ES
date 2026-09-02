package ac.iiit.rtltutor.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ac.iiit.rtltutor.data.UserRepository
import ac.iiit.rtltutor.models.User

data class StudentInfo(
    val user: User,
    val sessionCount: Int,
    val streak: Int
)

class AdminViewModel : ViewModel() {

    private val _students = MutableLiveData<List<StudentInfo>>(emptyList())
    val students: LiveData<List<StudentInfo>> = _students

    private val _totalUsers = MutableLiveData(0)
    val totalUsers: LiveData<Int> = _totalUsers

    private val _totalSessions = MutableLiveData(0)
    val totalSessions: LiveData<Int> = _totalSessions

    init { refresh() }

    fun refresh() {
        val allStudents = UserRepository.getAllStudents()
        val infos = allStudents.map { user ->
            val (sessions, streak) = UserRepository.getUserStats(user.id)
            StudentInfo(user, sessions, streak)
        }
        _students.value = infos
        _totalUsers.value = UserRepository.getAllUsers().size
        _totalSessions.value = infos.sumOf { it.sessionCount }
    }
}

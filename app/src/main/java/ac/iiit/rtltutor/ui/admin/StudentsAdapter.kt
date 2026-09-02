package ac.iiit.rtltutor.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ac.iiit.rtltutor.databinding.ItemStudentBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentsAdapter : ListAdapter<StudentInfo, StudentsAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<StudentInfo>() {
            override fun areItemsTheSame(o: StudentInfo, n: StudentInfo) = o.user.id == n.user.id
            override fun areContentsTheSame(o: StudentInfo, n: StudentInfo) = o == n
        }
        private val DATE_FMT = SimpleDateFormat("MMM d", Locale.getDefault())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStudentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val b: ItemStudentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(info: StudentInfo) {
            b.tvStudentName.text = info.user.displayName
            b.tvStudentUsername.text = "@${info.user.username} · ${info.sessionCount} sessions"
            b.tvStudentInitial.text = info.user.displayName.firstOrNull()?.uppercase() ?: "?"
            b.tvStudentJoined.text = DATE_FMT.format(Date(info.user.createdAt))
        }
    }
}

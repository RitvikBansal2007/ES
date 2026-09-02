package ac.iiit.rtltutor.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ac.iiit.rtltutor.R
import ac.iiit.rtltutor.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private const val VIEW_TYPE_AI = 0
        private const val VIEW_TYPE_USER = 1
        private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
                oldItem == newItem
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).isFromAI) VIEW_TYPE_AI else VIEW_TYPE_USER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_AI -> {
                val view = inflater.inflate(R.layout.item_message_ai, parent, false)
                AiMessageViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_message_user, parent, false)
                UserMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is AiMessageViewHolder -> holder.bind(msg)
            is UserMessageViewHolder -> holder.bind(msg)
        }
    }

    class AiMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tv_ai_message)
        private val tvBloom: TextView = view.findViewById(R.id.tv_bloom_badge)
        private val tvTime: TextView = view.findViewById(R.id.tv_timestamp)

        fun bind(msg: ChatMessage) {
            tvMessage.text = msg.content
            tvBloom.text = "Bloom L${msg.bloomLevel}"
            tvTime.text = TIME_FORMAT.format(Date(msg.timestamp))
        }
    }

    class UserMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tv_user_message)
        private val tvTime: TextView = view.findViewById(R.id.tv_timestamp)

        fun bind(msg: ChatMessage) {
            tvMessage.text = msg.content
            tvTime.text = TIME_FORMAT.format(Date(msg.timestamp))
        }
    }
}

package com.max.pandaai.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.max.pandaai.R
import com.max.pandaai.data.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter that renders user and assistant bubbles with timestamps.
 */
class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int = when (getItem(position).sender) {
        ChatMessage.Sender.USER -> VIEW_TYPE_USER
        ChatMessage.Sender.ASSISTANT -> VIEW_TYPE_ASSISTANT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            val view = inflater.inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_chat_assistant, parent, false)
            AssistantViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        val formattedTime = timeFormatter.format(Date(message.timestamp))
        when (holder) {
            is UserViewHolder -> holder.bind(message.text, formattedTime)
            is AssistantViewHolder -> holder.bind(message.text, formattedTime)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean = oldItem == newItem
    }

    private class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        fun bind(text: String, time: String) {
            messageText.text = text
            messageTime.text = time
        }
    }

    private class AssistantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        fun bind(text: String, time: String) {
            messageText.text = text
            messageTime.text = time
        }
    }

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_ASSISTANT = 2
        private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    }
}

package com.example.nilopartneron.chat

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.nilopartneron.R
import com.example.nilopartneron.databinding.ItemChatBinding
import com.example.nilopartneron.entities.Message

class ChatAdapter(private val messageList: MutableList<Message>, private val listener: OnChatListener
    ) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private lateinit var context : Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false)

        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messageList[position]

        holder.setListener(message)

        var gravity = Gravity.END
        var background = ContextCompat.getDrawable(context, R.drawable.background_chat_support)
        var textcolor = ContextCompat.getColor(context, R.color.colorOnPrimary)

        val marginHorizontal = context.resources.getDimensionPixelSize(R.dimen.chat_margin_horizontal)
        val params= holder.binding.tvMessage.layoutParams as ViewGroup.MarginLayoutParams
        params.marginStart= marginHorizontal
        params.marginEnd = 0

        if (position>0 && message.isSentByMe() != messageList[position-1].isSentByMe()){
            params.topMargin= context.resources.getDimensionPixelSize(R.dimen.common_padding_min)
        }

        if (!message.isSentByMe()){
             gravity = Gravity.START
             background = ContextCompat.getDrawable(context, R.drawable.background_chat_client)
             textcolor = ContextCompat.getColor(context, R.color.colorOnSecondary)
            params.marginStart =0
            params.marginEnd = marginHorizontal

        }

        holder.binding.root.gravity = gravity
        holder.binding.tvMessage.layoutParams = params
        holder.binding.tvMessage.setBackground(background)
        holder.binding.tvMessage.setTextColor(textcolor)

        holder.binding.tvMessage.text = message.message
    }

    override fun getItemCount(): Int = messageList.size

    fun add(message: Message) {
        if (!messageList.contains(message)) {
            messageList.add(message)
            notifyItemInserted(messageList.size -1)

        }
    }
    fun update(message: Message){
        val index = messageList.indexOf(message)
        if (index != -1){
            messageList.set(index, message)
            notifyItemChanged(index)
        }
    }
    fun delete(message: Message){
        val index = messageList.indexOf(message)
        if (index != -1){
            messageList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemChatBinding.bind(view)

        fun setListener(message: Message) {
            binding.tvMessage.setOnClickListener {
                listener.deleteMessage(message)
                true
            }
        }
    }


}
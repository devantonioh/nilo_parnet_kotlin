package com.example.nilopartneron.chat

import com.example.nilopartneron.entities.Message

interface OnChatListener {
    fun deleteMessage(message: Message)
}
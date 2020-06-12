package com.example.chatwebsocketdemo.model

import java.util.*

data class Chat(val id: UUID)

data class ChatMessage(val type: MessageType, val sender: String, val content: String? = null)

enum class MessageType {
    CHAT, JOIN, LEAVE
}

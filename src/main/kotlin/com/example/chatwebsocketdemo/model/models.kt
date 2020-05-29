package com.example.chatwebsocketdemo.model

data class ChatMessage(val type: MessageType, val sender: String, val content: String? = null)

enum class MessageType {
    CHAT, JOIN, LEAVE
}

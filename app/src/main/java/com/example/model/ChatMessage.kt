package com.example.model

import java.util.UUID

enum class MessageRole { USER, AI, TOOL }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val text: String = "",
    val toolName: String? = null,
    val isTyping: Boolean = false
)

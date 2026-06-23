package com.example.model

import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Conversation",
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

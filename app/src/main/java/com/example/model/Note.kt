package com.example.model

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Note",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

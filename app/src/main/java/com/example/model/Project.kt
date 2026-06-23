package com.example.model

import java.util.UUID

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "New Project",
    val description: String = "",
    val instructions: String = "",
    val modelId: String = "google/gemini-2.5-flash",
    val memory: String = "",
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis()
)

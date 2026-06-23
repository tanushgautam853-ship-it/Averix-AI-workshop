package com.example.api

import com.example.model.ModelInfo

interface ModelProviderAdapter {
    suspend fun fetchModels(): Result<List<ModelInfo>>
    val isConfigured: Boolean
    val disabledReason: String?
}

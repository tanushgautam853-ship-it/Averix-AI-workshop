package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>
)

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatResponse(
    val id: String?,
    val choices: List<OpenRouterChoice>?
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    val message: OpenRouterMessage?
)

@JsonClass(generateAdapter = true)
data class OpenRouterModelsResponse(
    val data: List<OpenRouterModelInfo>
)

@JsonClass(generateAdapter = true)
data class OpenRouterModelPricing(
    val prompt: String?,
    val completion: String?
)

@JsonClass(generateAdapter = true)
data class OpenRouterModelInfo(
    val id: String,
    val name: String?,
    val description: String?,
    val context_length: Int?,
    val pricing: OpenRouterModelPricing? = null
) {
    val isFree: Boolean
        get() = (pricing?.prompt?.toDoubleOrNull() ?: 0.0) == 0.0 && 
                (pricing?.completion?.toDoubleOrNull() ?: 0.0) == 0.0 || 
                id.contains(":free") || 
                id.contains("free", ignoreCase = true)
    
    val provider: String
        get() = id.substringBefore("/")

    val displayName: String
        get() = name ?: id.substringAfter("/")
}

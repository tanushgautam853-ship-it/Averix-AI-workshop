package com.example.model

enum class ModelProviderType {
    GEMINI, OPEN_ROUTER, GROQ, GITHUB
}

data class ModelInfo(
    val id: String,
    val name: String,
    val provider: ModelProviderType,
    val description: String?,
    val contextWindow: Int?,
    val free: Boolean,
    val freeTier: Boolean,
    val inputPrice: Double?, // Price per 1M tokens or similar
    val outputPrice: Double?,
    val supportsVision: Boolean,
    val supportsImageGeneration: Boolean,
    val supportsVideoGeneration: Boolean,
    val supportsAudio: Boolean,
    val supportsReasoning: Boolean,
    val supportsCoding: Boolean,
    val supportsTools: Boolean,
    val speedScore: Int?, // 1-10
    val qualityScore: Int? // 1-10
)

enum class TaskType {
    CHAT, CODING, RESEARCH, STUDY, REASONING, VISION, IMAGE_GENERATION, VIDEO_GENERATION, AUDIO
}

object AutoRouter {
    fun getBestModel(models: List<ModelInfo>, task: TaskType): ModelInfo? {
        if (models.isEmpty()) return null
        
        return when (task) {
            TaskType.CODING -> models.filter { it.supportsCoding }.maxByOrNull { it.qualityScore ?: 0 }
            TaskType.REASONING -> models.filter { it.supportsReasoning }.maxByOrNull { it.qualityScore ?: 0 }
            TaskType.VISION -> models.filter { it.supportsVision }.maxByOrNull { it.qualityScore ?: 0 }
            TaskType.IMAGE_GENERATION -> models.filter { it.supportsImageGeneration }.maxByOrNull { it.qualityScore ?: 0 }
            TaskType.VIDEO_GENERATION -> models.filter { it.supportsVideoGeneration }.maxByOrNull { it.qualityScore ?: 0 }
            TaskType.AUDIO -> models.filter { it.supportsAudio }.maxByOrNull { it.qualityScore ?: 0 }
            TaskType.RESEARCH, TaskType.STUDY -> models.maxByOrNull { it.contextWindow ?: 0 }
            TaskType.CHAT -> models.maxByOrNull { it.speedScore ?: 0 }
        } ?: models.firstOrNull()
    }
}

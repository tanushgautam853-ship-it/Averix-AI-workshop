package com.example.api

import com.example.model.ModelInfo
import com.example.model.ModelProviderType
import java.lang.Exception

class OpenRouterProviderAdapter(private val apiKey: String?) : ModelProviderAdapter {
    override val isConfigured: Boolean get() = !apiKey.isNullOrBlank()
    override val disabledReason: String? get() = if (isConfigured) null else "Missing OpenRouter API Key in Secrets"

    override suspend fun fetchModels(): Result<List<ModelInfo>> {
        if (!isConfigured) return Result.failure(Exception(disabledReason))
        return try {
            val response = com.example.network.OpenRouterClient.api.getModels()
            val mapped = response.data.map { info ->
                ModelInfo(
                    id = info.id,
                    name = info.displayName,
                    provider = ModelProviderType.OPEN_ROUTER,
                    description = info.description,
                    contextWindow = info.context_length,
                    free = info.isFree,
                    freeTier = info.isFree,
                    inputPrice = info.pricing?.prompt?.toDoubleOrNull(),
                    outputPrice = info.pricing?.completion?.toDoubleOrNull(),
                    supportsVision = info.id.contains("vision", true) || info.description?.contains("vision", true) == true,
                    supportsImageGeneration = false,
                    supportsVideoGeneration = false,
                    supportsAudio = false,
                    supportsReasoning = info.id.contains("reason", true) || info.description?.contains("reason", true) == true,
                    supportsCoding = info.id.contains("coder", true) || info.description?.contains("code", true) == true,
                    supportsTools = info.description?.contains("tool", true) == true || info.description?.contains("function", true) == true,
                    speedScore = if (info.id.contains("flash", true) || info.id.contains("haiku", true)) 9 else 6,
                    qualityScore = if (info.id.contains("opus", true) || info.id.contains("pro", true) || info.id.contains("gpt-4", true)) 9 else 6
                )
            }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GeminiProviderAdapter(private val apiKey: String?) : ModelProviderAdapter {
    override val isConfigured: Boolean get() = !apiKey.isNullOrBlank()
    override val disabledReason: String? get() = if (isConfigured) null else "Missing Google Gemini API Key in Secrets"

    override suspend fun fetchModels(): Result<List<ModelInfo>> {
        if (!isConfigured) return Result.failure(Exception(disabledReason))
        return try {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
                .build()
            
            val response = kotlinx.coroutines.Dispatchers.IO.let {
                kotlinx.coroutines.withContext(it) {
                    client.newCall(request).execute()
                }
            }
            if (!response.isSuccessful) throw Exception("Gemini API error: ${response.code}")
            
            val body = response.body?.string() ?: throw Exception("Empty response body")
            val jsonObject = org.json.JSONObject(body)
            val dataArray = jsonObject.optJSONArray("models") ?: org.json.JSONArray()
            
            val models = mutableListOf<ModelInfo>()
            for (i in 0 until dataArray.length()) {
                val modelObj = dataArray.getJSONObject(i)
                val id = modelObj.optString("name")
                val supportedMethods = modelObj.optJSONArray("supportedGenerationMethods")
                var supportsGenerateContent = false
                if (supportedMethods != null) {
                    for (j in 0 until supportedMethods.length()) {
                        if (supportedMethods.getString(j) == "generateContent") {
                            supportsGenerateContent = true
                            break
                        }
                    }
                }
                
                // Only include models that support chat/content generation to avoid listing embedding models everywhere
                if (supportsGenerateContent || id.contains("gemini", true)) {
                    val isPro = id.contains("pro")
                    val isFlash = id.contains("flash")
                    val isReasoning = id.contains("thinking")
                    
                    models.add(
                        ModelInfo(
                            id = id,
                            name = modelObj.optString("displayName").ifEmpty { id.substringAfterLast("/") },
                            provider = ModelProviderType.GEMINI,
                            description = modelObj.optString("description"),
                            contextWindow = modelObj.optInt("inputTokenLimit", if (isPro) 2097152 else 1048576),
                            free = false,
                            freeTier = true,
                            inputPrice = if (isPro) 1.25 else 0.075,
                            outputPrice = if (isPro) 5.0 else 0.30,
                            supportsVision = true, // Most Gemini 1.5+ support vision
                            supportsImageGeneration = false,
                            supportsVideoGeneration = true,
                            supportsAudio = true,
                            supportsReasoning = isReasoning,
                            supportsCoding = true,
                            supportsTools = true,
                            speedScore = if (isFlash) 10 else 7,
                            qualityScore = if (isPro) 9 else 7
                        )
                    )
                }
            }
            Result.success(models)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GroqProviderAdapter(private val apiKey: String?) : ModelProviderAdapter {
    override val isConfigured: Boolean get() = !apiKey.isNullOrBlank()
    override val disabledReason: String? get() = if (isConfigured) null else "Missing Groq API Key in Secrets"

    override suspend fun fetchModels(): Result<List<ModelInfo>> {
        if (!isConfigured) return Result.failure(Exception(disabledReason))
        return try {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder()
                .url("https://api.groq.com/openai/v1/models")
                .header("Authorization", "Bearer $apiKey")
                .build()
            
            val response = kotlinx.coroutines.Dispatchers.IO.let {
                kotlinx.coroutines.withContext(it) {
                    client.newCall(request).execute()
                }
            }
            if (!response.isSuccessful) throw Exception("Groq API error: ${response.code}")
            
            val body = response.body?.string() ?: throw Exception("Empty response body")
            val jsonObject = org.json.JSONObject(body)
            val dataArray = jsonObject.optJSONArray("data") ?: org.json.JSONArray()
            
            val models = mutableListOf<ModelInfo>()
            for (i in 0 until dataArray.length()) {
                val modelObj = dataArray.getJSONObject(i)
                val id = modelObj.optString("id")
                val isWhisper = id.contains("whisper", true)
                val isVision = id.contains("vision", true)
                models.add(
                    ModelInfo(
                        id = id,
                        name = id.replace("-", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                        provider = ModelProviderType.GROQ,
                        description = "Groq model $id",
                        contextWindow = if (id.contains("32k")) 32768 else if (id.contains("128k")) 131072 else 8192,
                        free = false,
                        freeTier = true,
                        inputPrice = 0.0,
                        outputPrice = 0.0,
                        supportsVision = isVision,
                        supportsImageGeneration = false,
                        supportsVideoGeneration = false,
                        supportsAudio = isWhisper,
                        supportsReasoning = id.contains("deepseek-r1") || id.contains("reason", ignoreCase = true),
                        supportsCoding = id.contains("coder", true) || id.contains("llama", true) || id.contains("deepseek", true),
                        supportsTools = id.contains("llama", true) || id.contains("mixtral", true),
                        speedScore = 10, // Groq is fast
                        qualityScore = if (id.contains("llama-3.3") || id.contains("deepseek-r1", true)) 9 else 7
                    )
                )
            }
            Result.success(models)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GitHubModelsProviderAdapter(private val githubToken: String?) : ModelProviderAdapter {
    override val isConfigured: Boolean get() = !githubToken.isNullOrBlank()
    override val disabledReason: String? get() = if (isConfigured) null else "Missing GitHub Personal Access Token in Secrets"

    override suspend fun fetchModels(): Result<List<ModelInfo>> {
        if (!isConfigured) return Result.failure(Exception(disabledReason))
        return try {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder()
                .url("https://models.inference.ai.azure.com/models")
                .header("Authorization", "Bearer $githubToken")
                .build()
            
            val response = kotlinx.coroutines.Dispatchers.IO.let {
                kotlinx.coroutines.withContext(it) {
                    client.newCall(request).execute()
                }
            }
            if (!response.isSuccessful) throw Exception("GitHub Models API error: ${response.code}")
            
            val body = response.body?.string() ?: throw Exception("Empty response body")
            val jsonArray = org.json.JSONArray(body)
            
            val models = mutableListOf<ModelInfo>()
            for (i in 0 until jsonArray.length()) {
                val modelObj = jsonArray.getJSONObject(i)
                val id = modelObj.optString("name")
                val isVision = modelObj.optString("task").contains("vision-text-to-text", ignoreCase = true) || id.contains("vision", true)
                models.add(
                    ModelInfo(
                        id = id,
                        name = modelObj.optString("friendly_name").ifEmpty { id },
                        provider = ModelProviderType.GITHUB,
                        description = modelObj.optString("summary"),
                        contextWindow = 8192,
                        free = true,
                        freeTier = true,
                        inputPrice = 0.0,
                        outputPrice = 0.0,
                        supportsVision = isVision,
                        supportsImageGeneration = false,
                        supportsVideoGeneration = false,
                        supportsAudio = false,
                        supportsReasoning = id.contains("o1") || id.contains("o3") || id.contains("reason", true),
                        supportsCoding = true,
                        supportsTools = true,
                        speedScore = 7,
                        qualityScore = 8
                    )
                )
            }
            Result.success(models)
        } catch (e: Exception) {
            // Fallback for OpenAI compatible endpoint if the azure one fails
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url("https://models.inference.ai.azure.com/v1/models")
                    .header("Authorization", "Bearer $githubToken")
                    .build()
                
                val response = kotlinx.coroutines.Dispatchers.IO.let {
                    kotlinx.coroutines.withContext(it) {
                        client.newCall(request).execute()
                    }
                }
                if (!response.isSuccessful) throw Exception("GitHub Models API error: ${response.code}")
                
                val body = response.body?.string() ?: throw Exception("Empty response body")
                val jsonObject = org.json.JSONObject(body)
                val dataArray = jsonObject.optJSONArray("data") ?: org.json.JSONArray()
                
                val models = mutableListOf<ModelInfo>()
                for (i in 0 until dataArray.length()) {
                    val modelObj = dataArray.getJSONObject(i)
                    val id = modelObj.getString("id")
                    models.add(
                        ModelInfo(id = id, name = id, provider = ModelProviderType.GITHUB, description = "GitHub Model", contextWindow = 8192, free = true, freeTier = true, inputPrice = 0.0, outputPrice = 0.0, supportsVision = id.contains("vision", true), supportsImageGeneration = false, supportsVideoGeneration = false, supportsAudio = false, supportsReasoning = id.contains("o1"), supportsCoding = true, supportsTools = true, speedScore = 7, qualityScore = 8)
                    )
                }
                Result.success(models)
            } catch (innerE: Exception) {
                Result.failure(e)
            }
        }
    }
}

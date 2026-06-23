package com.example.api

import com.example.model.ModelProviderType
import com.example.model.OpenRouterMessage
import com.example.model.OpenRouterChatRequest
import com.example.network.OpenRouterClient
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object ChatRouter {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun generateChatResponse(
        provider: ModelProviderType,
        modelId: String,
        messages: List<OpenRouterMessage>
    ): String {
        return withContext(Dispatchers.IO) {
            when (provider) {
                ModelProviderType.OPEN_ROUTER -> {
                    val request = OpenRouterChatRequest(model = modelId, messages = messages)
                    val response = OpenRouterClient.api.createChatCompletion(request)
                    response.choices?.firstOrNull()?.message?.content ?: throw Exception("Empty response from OpenRouter")
                }
                ModelProviderType.GROQ -> {
                    val apiKey = BuildConfig.GROQ_API_KEY.takeIf { it.isNotBlank() } ?: throw Exception("Missing Groq API Key")
                    callOpenAICompatibleEndpoint("https://api.groq.com/openai/v1/chat/completions", apiKey, modelId, messages)
                }
                ModelProviderType.GITHUB -> {
                    val apiKey = BuildConfig.GITHUB_TOKEN.takeIf { it.isNotBlank() } ?: throw Exception("Missing GitHub Token")
                    callOpenAICompatibleEndpoint("https://models.inference.ai.azure.com/chat/completions", apiKey, modelId, messages)
                }
                ModelProviderType.GEMINI -> {
                    val apiKey = BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() } ?: throw Exception("Missing Gemini API Key")
                    val normalizedModelId = if (modelId.startsWith("models/")) modelId.substringAfter("models/") else modelId
                    callOpenAICompatibleEndpoint("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions?key=$apiKey", apiKey, normalizedModelId, messages, useBearerHeader = false)
                }
            }
        }
    }

    private fun callOpenAICompatibleEndpoint(
        url: String,
        apiKey: String,
        modelId: String,
        messages: List<OpenRouterMessage>,
        useBearerHeader: Boolean = true
    ): String {
        val messagesJson = JSONArray()
        for (msg in messages) {
            val msgObj = JSONObject()
            msgObj.put("role", msg.role)
            msgObj.put("content", msg.content)
            messagesJson.put(msgObj)
        }
        
        val payload = JSONObject()
        payload.put("model", modelId)
        payload.put("messages", messagesJson)
        
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)
            
        if (useBearerHeader) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }
        
        val request = requestBuilder.build()
            
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            throw IOException("Provider API error ${response.code}: $errorBody")
        }
        
        val responseBody = response.body?.string() ?: throw IOException("Empty response body")
        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices") ?: throw IOException("No choices in response")
        val firstChoice = choices.optJSONObject(0) ?: throw IOException("Empty choices array")
        val message = firstChoice.optJSONObject("message") ?: throw IOException("No message in choice")
        return message.optString("content", "")
    }
}

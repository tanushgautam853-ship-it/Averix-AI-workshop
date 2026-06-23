package com.example.network

import com.example.BuildConfig
import com.example.model.OpenRouterChatRequest
import com.example.model.OpenRouterChatResponse
import com.example.model.OpenRouterModelsResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface OpenRouterApi {
    @GET("models")
    suspend fun getModels(): OpenRouterModelsResponse

    @POST("chat/completions")
    suspend fun createChatCompletion(@Body request: OpenRouterChatRequest): OpenRouterChatResponse
}

object OpenRouterClient {
    private const val BASE_URL = "https://openrouter.ai/api/v1/"

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("Authorization", "Bearer ${BuildConfig.OPENROUTER_API_KEY}")
            .header("HTTP-Referer", "https://averix.ai")
            .header("X-Title", "Averix AI")
        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val api: OpenRouterApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(OpenRouterApi::class.java)
    }
}

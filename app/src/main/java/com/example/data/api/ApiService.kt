package com.example.data.api

import retrofit2.http.*

interface ApiService {
    // Dynamic Gemini API call
    @POST
    suspend fun generateGeminiContent(
        @Url url: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    // Dynamic OpenAI-compatible API call
    @POST
    suspend fun generateOpenaiChatCompletion(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body request: OpenaiChatRequest
    ): OpenaiResponse
}

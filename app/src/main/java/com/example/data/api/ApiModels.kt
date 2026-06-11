package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// === Gemini Models ===

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiSystemInstruction? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "role") val role: String, // "user" or "model"
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiSystemInstruction(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiCandidateContent? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidateContent(
    @Json(name = "parts") val parts: List<GeminiPart>? = null,
    @Json(name = "role") val role: String? = null
)

// === OpenAI Chat Completions Models (Groq, OpenRouter, Hugging Face OpenAI-compatible) ===

@JsonClass(generateAdapter = true)
data class OpenaiChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<OpenaiMessage>,
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "max_tokens") val maxTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class OpenaiMessage(
    @Json(name = "role") val role: String, // "system", "user", "assistant"
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OpenaiResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "model") val model: String? = null,
    @Json(name = "choices") val choices: List<OpenaiChoice>? = null,
    @Json(name = "error") val error: OpenaiError? = null
)

@JsonClass(generateAdapter = true)
data class OpenaiChoice(
    @Json(name = "index") val index: Int? = null,
    @Json(name = "message") val message: OpenaiMessage? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenaiError(
    @Json(name = "message") val message: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "code") val code: String? = null
)

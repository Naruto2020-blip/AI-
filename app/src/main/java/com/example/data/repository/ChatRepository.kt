package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.ApiService
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.OpenaiChatRequest
import com.example.data.api.OpenaiMessage
import com.example.data.api.RetrofitClient
import com.example.data.local.ChatDao
import com.example.data.model.ApiEngineConfig
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatRepository(private val chatDao: ChatDao) {

    private val apiService: ApiService = RetrofitClient.apiService

    // Observables
    val sessionsFlow: Flow<List<ChatSession>> = chatDao.getAllSessions()
    val enginesFlow: Flow<List<ApiEngineConfig>> = chatDao.getAllEngineConfigsFlow()

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun getSessions(): List<ChatSession> = withContext(Dispatchers.IO) {
        // Just return current sessions or let the Flow handle it
        return@withContext emptyList()
    }

    // Initialize Default Engines incrementally so updates don't require database clear
    suspend fun initializeDefaultEngines() = withContext(Dispatchers.IO) {
        val existing = chatDao.getAllEngineConfigs()
        val existingIds = existing.map { it.engineId }.toSet()
        val defaults = listOf(
            ApiEngineConfig(
                engineId = "gemini",
                displayName = "Google Gemini 1.5 Flash (Créditos Gratis)",
                isEnabled = true,
                apiKey = "", // Uses BuildConfig.GEMINI_API_KEY as fallback if empty!
                apiBaseUrl = "v1beta/models/gemini-3.5-flash:generateContent",
                modelName = "gemini-3.5-flash",
                priority = 1,
                isBuiltIn = true
            ),
            ApiEngineConfig(
                engineId = "pollinations-openai",
                displayName = "GPT-4o (Gratis e Ilimitado)",
                isEnabled = true,
                apiKey = "", // Keyless!
                apiBaseUrl = "https://text.pollinations.ai/openai/v1/chat/completions",
                modelName = "openai",
                priority = 2,
                isBuiltIn = true
            ),
            ApiEngineConfig(
                engineId = "pollinations-llama",
                displayName = "Llama 3.1 70B (Gratis e Ilimitado)",
                isEnabled = true,
                apiKey = "", // Keyless!
                apiBaseUrl = "https://text.pollinations.ai/openai/v1/chat/completions",
                modelName = "llama",
                priority = 3,
                isBuiltIn = true
            ),
            ApiEngineConfig(
                engineId = "pollinations-unity",
                displayName = "Unity Coder (Gratis e Ilimitado)",
                isEnabled = true,
                apiKey = "", // Keyless!
                apiBaseUrl = "https://text.pollinations.ai/openai/v1/chat/completions",
                modelName = "unity",
                priority = 4,
                isBuiltIn = true
            ),
            ApiEngineConfig(
                engineId = "huggingface",
                displayName = "Hugging Face (Qwen 72B)",
                isEnabled = true,
                apiKey = "", // Works without key, or with HF token
                apiBaseUrl = "https://api-inference.huggingface.co/v1/chat/completions",
                modelName = "Qwen/Qwen2.5-72B-Instruct",
                priority = 5,
                isBuiltIn = true
            ),
            ApiEngineConfig(
                engineId = "groq",
                displayName = "Groq Cloud (Llama 3.3)",
                isEnabled = false,
                apiKey = "",
                apiBaseUrl = "https://api.groq.com/openai/v1/chat/completions",
                modelName = "llama-3.3-70b-versatile",
                priority = 6,
                isBuiltIn = true
            ),
            ApiEngineConfig(
                engineId = "openrouter",
                displayName = "OpenRouter (Modelos $0 gratis)",
                isEnabled = false,
                apiKey = "",
                apiBaseUrl = "https://openrouter.ai/api/v1/chat/completions",
                modelName = "meta-llama/llama-3-8b-instruct:free",
                priority = 7,
                isBuiltIn = true
            )
        )
        
        val missingDefaults = defaults.filter { it.engineId !in existingIds }
        if (missingDefaults.isNotEmpty()) {
            chatDao.insertEngineConfigs(missingDefaults)
        }
    }

    // DB Operations
    suspend fun createNewSession(title: String): Long = withContext(Dispatchers.IO) {
        chatDao.insertSession(ChatSession(title = title))
    }

    suspend fun updateSessionTitle(sessionId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            chatDao.updateSession(session.copy(title = newTitle))
        }
    }

    suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            chatDao.deleteSession(session)
        }
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        chatDao.deleteAllSessions()
    }

    suspend fun saveEngineConfig(config: ApiEngineConfig) = withContext(Dispatchers.IO) {
        chatDao.updateEngineConfig(config)
    }

    // Dynamic Central AI Chat Call with Auto-Fallback routing
    suspend fun sendMessageWithFallback(
        sessionId: Long,
        userMessageContent: String,
        autoFallbackEnabled: Boolean,
        onFallbackAttempt: (String, String) -> Unit
    ): ChatMessage = withContext(Dispatchers.IO) {
        // 1. Get enabled engines list, sorted by priority
        val configs = chatDao.getAllEngineConfigs().filter { it.isEnabled }
        if (configs.isEmpty()) {
            throw IllegalStateException("¡No hay motores de IA activos! Habilita al menos uno en Ajustes para poder chatear.")
        }

        // 2. Insert user message in DB
        val userMessage = ChatMessage(sessionId = sessionId, role = "user", content = userMessageContent)
        chatDao.insertMessage(userMessage)

        // 3. Insert temporary loading message in DB
        val assistantPlaceholderId = chatDao.insertMessage(
            ChatMessage(sessionId = sessionId, role = "assistant", content = "Conectando al motor de IA...", status = "sending")
        )

        // 4. Retrieve complete chat history for multi-turn model context
        val rawHistory = chatDao.getMessagesForSessionOnce(sessionId)
        // Keep only successful/sent history, filter out the placeholder message and errors
        val validHistory = rawHistory.filter { it.status == "success" && it.id != assistantPlaceholderId }

        // 5. Determine retry list
        val enginesToTry = if (autoFallbackEnabled) {
            configs
        } else {
            // Only try the selected engine
            val currentSession = chatDao.getSessionById(sessionId)
            val selectedEngineId = currentSession?.lastActiveEngine ?: "gemini"
            val match = configs.find { it.engineId == selectedEngineId } ?: configs.firstOrNull()
            if (match != null) listOf(match) else configs
        }

        var successOutputText = ""
        var successEngineId = ""
        val errorLogs = StringBuilder()

        for (config in enginesToTry) {
            try {
                onFallbackAttempt(config.engineId, "[INTENTO] Conectando con ${config.displayName}...")
                
                val output = executeNetworkCall(config, validHistory)
                if (output.isNotBlank()) {
                    successOutputText = output
                    successEngineId = config.engineId
                    chatDao.incrementSuccessCount(config.engineId)
                    onFallbackAttempt(config.engineId, "[ÉXITO] Respuesta recibida de forma exitosa.")
                    break
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: e.message ?: "Sin respuesta del servidor"
                errorLogs.append("- ${config.displayName}: $errorMsg\n")
                chatDao.incrementFailureCount(config.engineId, errorMsg)
                onFallbackAttempt(config.engineId, "[FALLO] Error en llamado: $errorMsg")
                
                if (!autoFallbackEnabled) {
                    // Update DB with the error and bubble up
                    val finalErrorMessage = "Error en ${config.displayName}:\n$errorMsg"
                    chatDao.updateMessageStatus(assistantPlaceholderId, "error", finalErrorMessage)
                    throw Exception(finalErrorMessage)
                }
            }
        }

        if (successEngineId.isNotEmpty() && successOutputText.isNotEmpty()) {
            // Update session last active engine
            val currentSession = chatDao.getSessionById(sessionId)
            if (currentSession != null) {
                chatDao.updateSession(currentSession.copy(lastActiveEngine = successEngineId))
            }

            // Save the received assistant response in DB
            val finalMessage = ChatMessage(
                id = assistantPlaceholderId,
                sessionId = sessionId,
                role = "assistant",
                content = successOutputText,
                engineUsed = successEngineId,
                status = "success"
            )
            chatDao.insertMessage(finalMessage)
            return@withContext finalMessage
        } else {
            val failureSummary = "¡Ha fallado la conexión con los servidores!\n\nHistorial de Errores:\n$errorLogs\n\n📌 Solución: Verifica tu conexión a internet o ingresa tus propias llaves API en la pestaña Ajustes."
            chatDao.updateMessageStatus(assistantPlaceholderId, "error", failureSummary)
            return@withContext ChatMessage(
                id = assistantPlaceholderId,
                sessionId = sessionId,
                role = "assistant",
                content = failureSummary,
                status = "error"
            )
        }
    }

    private suspend fun executeNetworkCall(config: ApiEngineConfig, history: List<ChatMessage>): String {
        return when (config.engineId) {
            "gemini" -> {
                val geminiContents = history.map { msg ->
                    GeminiContent(
                        role = if (msg.role == "user") "user" else "model",
                        parts = listOf(GeminiPart(text = msg.content))
                    )
                }
                val request = GeminiRequest(contents = geminiContents)
                
                // Construct URL correctly
                val relativeUrl = "v1beta/models/${config.modelName}:generateContent"
                
                // Key fallback matching AI Studio specifications (GEMINI_API_KEY)
                val key = config.apiKey.ifBlank {
                    BuildConfig.GEMINI_API_KEY
                }
                
                if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
                    throw IllegalStateException("API Key de Gemini no especificada en Ajustes ni en el servidor.")
                }

                val response = apiService.generateGeminiContent(relativeUrl, key, request)
                val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (textResponse.isNullOrBlank()) {
                    throw Exception("Gemini retornó un candidato vacío o bloqueado por políticas de contenido.")
                }
                textResponse
            }
            else -> {
                // OpenAI-compatible format (Groq, OpenRouter, Hugging Face)
                val openaiMessages = history.map { msg ->
                    OpenaiMessage(
                        role = if (msg.role == "user") "user" else "assistant",
                        content = msg.content
                    )
                }
                val request = OpenaiChatRequest(
                    model = config.modelName,
                    messages = openaiMessages,
                    temperature = 0.7f
                )

                // Auth Header selection
                val header = if (config.apiKey.isNotBlank()) "Bearer ${config.apiKey}" else ""
                
                // For Hugging Face, let's inject a fallback header or warning if key is blank is okay, it gets free tier fallback
                val response = apiService.generateOpenaiChatCompletion(
                    url = config.apiBaseUrl,
                    authHeader = header,
                    request = request
                )

                val textResponse = response.choices?.firstOrNull()?.message?.content
                if (textResponse.isNullOrBlank()) {
                    val apiError = response.error?.message
                    if (!apiError.isNullOrBlank()) {
                        throw Exception(apiError)
                    } else {
                        throw Exception("El servidor retornó una respuesta de chat vacía.")
                    }
                }
                textResponse
            }
        }
    }
}

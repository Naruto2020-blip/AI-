package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ApiEngineConfig
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val chatDao = AppDatabase.getDatabase(application).chatDao()
    private val repository = ChatRepository(chatDao)

    // Global Flows from database
    val sessions: StateFlow<List<ChatSession>> = repository.sessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val engines: StateFlow<List<ApiEngineConfig>> = repository.enginesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chat Management State
    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

    // Observable Messages for active session
    val activeMessages: StateFlow<List<ChatMessage>> = _activeSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesForSession(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback States
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _autoFallback = MutableStateFlow(true) // Enabled by default!
    val autoFallback: StateFlow<Boolean> = _autoFallback.asStateFlow()

    private val _fallbackLogs = MutableStateFlow<List<String>>(emptyList())
    val fallbackLogs: StateFlow<List<String>> = _fallbackLogs.asStateFlow()

    private val _alertMessage = MutableStateFlow<String?>(null)
    val alertMessage: StateFlow<String?> = _alertMessage.asStateFlow()

    init {
        viewModelScope.launch {
            // Setup pre-packaged defaults in database
            repository.initializeDefaultEngines()
            
            // Auto select latest chat session if exists
            sessions.collectFirst { list ->
                if (list.isNotEmpty() && _activeSessionId.value == null) {
                    _activeSessionId.value = list.first().id
                }
            }
        }
    }

    // Helper extension to collect first non-empty emissions safely
    private fun <T> Flow<T>.collectFirst(action: suspend (T) -> Unit) {
        viewModelScope.launch {
            this@collectFirst.firstOrNull()?.let { action(it) }
        }
    }

    fun selectSession(sessionId: Long) {
        _activeSessionId.value = sessionId
    }

    fun createNewSession(title: String = "Nueva Conversación") {
        viewModelScope.launch {
            val newId = repository.createNewSession(title)
            _activeSessionId.value = newId
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_activeSessionId.value == sessionId) {
                // Return fallback to next or null
                val remaining = sessions.value.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    _activeSessionId.value = remaining.first().id
                } else {
                    _activeSessionId.value = null
                }
            }
        }
    }

    fun updateSessionTitle(sessionId: Long, title: String) {
        viewModelScope.launch {
            repository.updateSessionTitle(sessionId, title)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _activeSessionId.value = null
            _fallbackLogs.value = emptyList()
        }
    }

    fun toggleFallback(enabled: Boolean) {
        _autoFallback.value = enabled
    }

    fun clearLogs() {
        _fallbackLogs.value = emptyList()
    }

    fun addManualLog(log: String) {
        _fallbackLogs.value = _fallbackLogs.value + log
    }

    fun toggleEngine(engineId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val current = engines.value.find { it.engineId == engineId }
            if (current != null) {
                repository.saveEngineConfig(current.copy(isEnabled = isEnabled))
            }
        }
    }

    fun updateEngineConfig(config: ApiEngineConfig) {
        viewModelScope.launch {
            repository.saveEngineConfig(config)
        }
    }

    fun setAlert(message: String?) {
        _alertMessage.value = message
    }

    // Principal message sender orchestrator
    fun sendMessage(content: String) {
        val sessionId = _activeSessionId.value ?: return
        if (content.isBlank() || _isSending.value) return

        viewModelScope.launch {
            _isSending.value = true
            _fallbackLogs.value = emptyList() // clear on starting new text query

            try {
                // If the session title is the default, rename it creatively based on user input
                val currentSession = sessions.value.find { it.id == sessionId }
                if (currentSession != null && (currentSession.title == "Nueva Conversación" || currentSession.title.isBlank())) {
                    val promptSlice = if (content.length > 25) content.take(25) + "..." else content
                    repository.updateSessionTitle(sessionId, promptSlice)
                }

                repository.sendMessageWithFallback(
                    sessionId = sessionId,
                    userMessageContent = content,
                    autoFallbackEnabled = _autoFallback.value,
                    onFallbackAttempt = { engineId, statusMsg ->
                        viewModelScope.launch {
                            val engineName = engines.value.find { it.engineId == engineId }?.displayName ?: engineId
                            val timedLog = "• $engineName: $statusMsg"
                            _fallbackLogs.value = _fallbackLogs.value + timedLog
                        }
                    }
                )
            } catch (e: Exception) {
                _fallbackLogs.value = _fallbackLogs.value + "❌ [FALLO COMBINADO] No se pudo obtener respuesta de ningún motor activo."
            } finally {
                _isSending.value = false
            }
        }
    }
}

// ViewModel factory helper
class ChatViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val lastActiveEngine: String = "gemini"
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val engineUsed: String? = null,
    val status: String = "success" // "sending", "success", "error"
)

@Entity(tableName = "api_engine_configs")
data class ApiEngineConfig(
    @PrimaryKey val engineId: String, // "gemini", "huggingface", "groq", "openrouter"
    val displayName: String,
    val isEnabled: Boolean = true,
    val apiKey: String = "",
    val apiBaseUrl: String = "",
    val modelName: String = "",
    val priority: Int = 0,
    val isBuiltIn: Boolean = false,
    val lastErrorMessage: String? = null,
    val successCount: Int = 0,
    val failureCount: Int = 0
)

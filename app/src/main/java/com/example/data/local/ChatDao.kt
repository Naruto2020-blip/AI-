package com.example.data.local

import androidx.room.*
import com.example.data.model.ChatSession
import com.example.data.model.ChatMessage
import com.example.data.model.ApiEngineConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // Chat Sessions
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Update
    suspend fun updateSession(session: ChatSession)

    @Delete
    suspend fun deleteSession(session: ChatSession)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions()

    // Chat Messages
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionOnce(sessionId: Long): List<ChatMessage>

    @Query("SELECT * FROM chat_messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: Long): ChatMessage?

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): ChatSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)

    @Query("UPDATE chat_messages SET status = :status, content = :content WHERE id = :id")
    suspend fun updateMessageStatus(id: Long, status: String, content: String)

    // Api Engine Configs
    @Query("SELECT * FROM api_engine_configs ORDER BY priority ASC")
    fun getAllEngineConfigsFlow(): Flow<List<ApiEngineConfig>>

    @Query("SELECT * FROM api_engine_configs ORDER BY priority ASC")
    suspend fun getAllEngineConfigs(): List<ApiEngineConfig>

    @Query("SELECT * FROM api_engine_configs WHERE engineId = :engineId LIMIT 1")
    suspend fun getEngineConfig(engineId: String): ApiEngineConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEngineConfigs(configs: List<ApiEngineConfig>)

    @Update
    suspend fun updateEngineConfig(config: ApiEngineConfig)

    @Query("UPDATE api_engine_configs SET successCount = successCount + 1, lastErrorMessage = NULL WHERE engineId = :engineId")
    suspend fun incrementSuccessCount(engineId: String)

    @Query("UPDATE api_engine_configs SET failureCount = failureCount + 1, lastErrorMessage = :errorMessage WHERE engineId = :engineId")
    suspend fun incrementFailureCount(engineId: String, errorMessage: String)
}

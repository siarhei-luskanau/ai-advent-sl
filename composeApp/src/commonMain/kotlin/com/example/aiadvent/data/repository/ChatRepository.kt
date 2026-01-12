package com.example.aiadvent.data.repository

import com.example.aiadvent.data.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(chatId: String): Flow<List<Message>>
    suspend fun sendMessage(message: Message): Result<Message>
    suspend fun sendMessageToLLM(chatId: String, userMessage: String): Result<Message>
    suspend fun deleteMessage(messageId: String)
    suspend fun clearAllMessages()
}

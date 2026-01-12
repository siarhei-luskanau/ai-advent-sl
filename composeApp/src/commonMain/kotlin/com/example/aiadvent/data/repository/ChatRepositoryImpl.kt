package com.example.aiadvent.data.repository

import com.example.aiadvent.data.local.dao.MessageDao
import com.example.aiadvent.data.local.entity.toEntity
import com.example.aiadvent.data.local.entity.toMessage
import com.example.aiadvent.data.model.Message
import com.example.aiadvent.data.model.MessageRole
import com.example.aiadvent.data.remote.LLMRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class ChatRepositoryImpl(
    private val messageDao: MessageDao,
    private val remoteDataSource: LLMRemoteDataSource
) : ChatRepository {

    override fun getMessages(chatId: String): Flow<List<Message>> {
        return messageDao.getMessagesByChatId(chatId).map { entities ->
            entities.map { it.toMessage() }
        }
    }

    override suspend fun sendMessage(message: Message): Result<Message> {
        return try {
            messageDao.insertMessage(message.toEntity())
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessageToLLM(chatId: String, userMessage: String): Result<Message> {
        return try {
            // Create and save user message
            val userMsg = Message(
                id = generateId(),
                content = userMessage,
                role = MessageRole.USER,
                timestamp = Clock.System.now(),
                chatId = chatId
            )
            messageDao.insertMessage(userMsg.toEntity())

            // Get conversation history
            val messages = messageDao.getMessagesByChatId(chatId)
                .map { entities -> entities.map { it.toMessage() } }

            // Send to LLM (this would need conversation history)
            val response = remoteDataSource.sendMessage(listOf(userMsg))

            // Create and save assistant message
            val assistantMsg = Message(
                id = response.id,
                content = response.choices.firstOrNull()?.message?.content ?: "",
                role = MessageRole.ASSISTANT,
                timestamp = Clock.System.now(),
                chatId = chatId
            )
            messageDao.insertMessage(assistantMsg.toEntity())

            Result.success(assistantMsg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: String) {
        messageDao.deleteMessage(messageId)
    }

    override suspend fun clearAllMessages() {
        messageDao.clearAll()
    }

    private fun generateId(): String {
        return Clock.System.now().toEpochMilliseconds().toString()
    }
}

package com.example.llmchat.data.repository

import com.example.llmchat.data.local.database.dao.MessageDao
import com.example.llmchat.data.model.Message
import com.example.llmchat.data.model.toDomain
import com.example.llmchat.data.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageRepository(
    private val messageDao: MessageDao
) {
    fun getMessages(conversationId: String): Flow<List<Message>> =
        messageDao.getMessagesByConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun saveMessage(message: Message) {
        messageDao.insertMessage(message.toEntity())
    }

    suspend fun getLastMessage(conversationId: String): Message? =
        messageDao.getLastMessage(conversationId)?.toDomain()
}

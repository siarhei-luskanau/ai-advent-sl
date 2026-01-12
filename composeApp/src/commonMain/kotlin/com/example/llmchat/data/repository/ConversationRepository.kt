package com.example.llmchat.data.repository

import com.example.llmchat.data.local.database.dao.ConversationDao
import com.example.llmchat.data.local.database.entity.ConversationEntity
import com.example.llmchat.data.model.Conversation
import com.example.llmchat.data.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ConversationRepository(
    private val conversationDao: ConversationDao
) {
    fun getAllConversations(): Flow<List<Conversation>> =
        conversationDao.getAllConversations().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getConversationById(id: String): Conversation? =
        conversationDao.getConversationById(id)?.toDomain()

    @OptIn(ExperimentalUuidApi::class)
    suspend fun createConversation(
        title: String,
        systemPrompt: String?,
        provider: String,
        model: String
    ): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val entity = ConversationEntity(
            id = Uuid.random().toString(),
            title = title,
            systemPrompt = systemPrompt,
            llmProvider = provider,
            modelName = model,
            createdAt = now,
            updatedAt = now
        )
        conversationDao.insertConversation(entity)
        return entity.id
    }

    suspend fun deleteConversation(conversationId: String) {
        conversationDao.getConversationById(conversationId)?.let {
            conversationDao.deleteConversation(it)
        }
    }

    suspend fun updateTimestamp(conversationId: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        conversationDao.updateTimestamp(conversationId, now)
    }
}

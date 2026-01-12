package com.example.llmchat.domain.usecase.conversation

import com.example.llmchat.data.model.Conversation
import com.example.llmchat.data.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow

class GetConversationsUseCase(
    private val conversationRepository: ConversationRepository
) {
    operator fun invoke(): Flow<List<Conversation>> {
        return conversationRepository.getAllConversations()
    }
}

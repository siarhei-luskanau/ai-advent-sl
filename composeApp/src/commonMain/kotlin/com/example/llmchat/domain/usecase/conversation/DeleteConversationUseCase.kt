package com.example.llmchat.domain.usecase.conversation

import com.example.llmchat.data.repository.ConversationRepository

class DeleteConversationUseCase(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(conversationId: String) {
        conversationRepository.deleteConversation(conversationId)
    }
}

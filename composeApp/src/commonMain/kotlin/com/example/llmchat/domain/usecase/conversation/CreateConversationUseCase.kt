package com.example.llmchat.domain.usecase.conversation

import com.example.llmchat.data.repository.ConversationRepository

class CreateConversationUseCase(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(
        title: String,
        systemPrompt: String?,
        provider: String,
        model: String
    ): String {
        return conversationRepository.createConversation(
            title = title,
            systemPrompt = systemPrompt,
            provider = provider,
            model = model
        )
    }
}

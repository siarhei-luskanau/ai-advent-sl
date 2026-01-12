package com.example.aiadvent.domain.usecase

import com.example.aiadvent.data.model.Message
import com.example.aiadvent.data.repository.ChatRepository

class SendMessageUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String, userMessage: String): Result<Message> {
        if (userMessage.isBlank()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty"))
        }

        return repository.sendMessageToLLM(chatId, userMessage)
    }
}

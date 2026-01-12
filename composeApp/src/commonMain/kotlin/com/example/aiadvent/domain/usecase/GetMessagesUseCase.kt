package com.example.aiadvent.domain.usecase

import com.example.aiadvent.data.model.Message
import com.example.aiadvent.data.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class GetMessagesUseCase(private val repository: ChatRepository) {
    operator fun invoke(chatId: String): Flow<List<Message>> {
        return repository.getMessages(chatId)
    }
}

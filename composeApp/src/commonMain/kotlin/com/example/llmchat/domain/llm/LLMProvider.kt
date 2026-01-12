package com.example.llmchat.domain.llm

import com.example.llmchat.data.model.Message
import kotlinx.coroutines.flow.Flow

interface LLMProvider {
    suspend fun sendMessage(
        messages: List<Message>,
        systemPrompt: String?,
        model: String
    ): String

    fun streamMessage(
        messages: List<Message>,
        systemPrompt: String?,
        model: String
    ): Flow<String>
}

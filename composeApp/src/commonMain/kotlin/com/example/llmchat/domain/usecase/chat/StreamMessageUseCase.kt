package com.example.llmchat.domain.usecase.chat

import com.example.llmchat.data.model.Message
import com.example.llmchat.data.repository.ConversationRepository
import com.example.llmchat.data.repository.MessageRepository
import com.example.llmchat.domain.llm.LLMProviderFactory
import com.example.llmchat.domain.llm.LLMProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class StreamMessageUseCase(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val llmProviderFactory: LLMProviderFactory
) {
    @OptIn(ExperimentalUuidApi::class)
    operator fun invoke(
        conversationId: String,
        userMessage: String
    ): Flow<StreamResult> = flow {
        try {
            emit(StreamResult.Loading)

            // Save user message
            val userMsg = Message(
                id = Uuid.random().toString(),
                conversationId = conversationId,
                role = "user",
                content = userMessage,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
            messageRepository.saveMessage(userMsg)

            // Get conversation and provider
            val conversation = conversationRepository.getConversationById(conversationId)
                ?: throw IllegalStateException("Conversation not found")

            val provider = llmProviderFactory.createProvider(
                LLMProviderType.valueOf(conversation.llmProvider)
            )

            // Get message history
            val messages = messageRepository.getMessages(conversationId).first()

            // Stream response
            val fullResponse = StringBuilder()

            provider.streamMessage(
                messages = messages,
                systemPrompt = conversation.systemPrompt,
                model = conversation.modelName
            ).collect { chunk ->
                fullResponse.append(chunk)
                emit(StreamResult.Chunk(chunk, fullResponse.toString()))
            }

            // Save complete response
            val assistantMsg = Message(
                id = Uuid.random().toString(),
                conversationId = conversationId,
                role = "assistant",
                content = fullResponse.toString(),
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
            messageRepository.saveMessage(assistantMsg)
            conversationRepository.updateTimestamp(conversationId)

            emit(StreamResult.Complete(assistantMsg))
        } catch (e: Exception) {
            emit(StreamResult.Error(e.message ?: "Unknown error"))
        }
    }
}

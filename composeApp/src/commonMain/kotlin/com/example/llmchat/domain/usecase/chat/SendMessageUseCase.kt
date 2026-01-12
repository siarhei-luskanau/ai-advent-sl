package com.example.llmchat.domain.usecase.chat

import com.example.llmchat.data.model.Message
import com.example.llmchat.data.repository.ConversationRepository
import com.example.llmchat.data.repository.MessageRepository
import com.example.llmchat.domain.llm.LLMProviderFactory
import com.example.llmchat.domain.llm.LLMProviderType
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SendMessageUseCase(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val llmProviderFactory: LLMProviderFactory
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        conversationId: String,
        userMessage: String
    ): Result<Message> = try {
        // Save user message
        val userMsg = Message(
            id = Uuid.random().toString(),
            conversationId = conversationId,
            role = "user",
            content = userMessage,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        messageRepository.saveMessage(userMsg)

        // Get conversation config
        val conversation = conversationRepository.getConversationById(conversationId)
            ?: throw IllegalStateException("Conversation not found")

        val provider = llmProviderFactory.createProvider(
            LLMProviderType.valueOf(conversation.llmProvider)
        )

        // Get message history
        val messages = messageRepository.getMessages(conversationId).first()

        // Send to LLM
        val response = provider.sendMessage(
            messages = messages,
            systemPrompt = conversation.systemPrompt,
            model = conversation.modelName
        )

        // Save assistant message
        val assistantMsg = Message(
            id = Uuid.random().toString(),
            conversationId = conversationId,
            role = "assistant",
            content = response,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        messageRepository.saveMessage(assistantMsg)

        // Update conversation timestamp
        conversationRepository.updateTimestamp(conversationId)

        Result.success(assistantMsg)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

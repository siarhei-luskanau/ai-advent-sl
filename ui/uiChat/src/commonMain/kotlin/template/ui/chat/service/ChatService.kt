package template.ui.chat.service

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import kotlinx.datetime.Clock
import template.ui.chat.model.ChatMessage
import template.ui.chat.model.MessageRole
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface ChatService {
    suspend fun sendMessage(
        userMessage: String,
        history: List<ChatMessage>,
    ): Result<String>
}

@OptIn(ExperimentalUuidApi::class)
class OllamaChatService(
    private val baseUrl: String,
) : ChatService {
    private val model = LLModel(
        provider = LLMProvider.Ollama,
        id = "qwen3:0.6b",
        capabilities = listOf(LLMCapability.Temperature),
        contextLength = 8192,
    )

    private val executor by lazy {
        simpleOllamaAIExecutor(baseUrl = baseUrl)
    }

    override suspend fun sendMessage(
        userMessage: String,
        history: List<ChatMessage>,
    ): Result<String> = runCatching {
        val messages = buildMessages(history, userMessage)

        val prompt = Prompt(
            messages = messages,
            id = Uuid.random().toString(),
        )

        val response = executor.execute(
            prompt = prompt,
            model = model,
        )

        response.firstOrNull()?.content ?: ""
    }

    private fun buildMessages(
        history: List<ChatMessage>,
        userMessage: String,
    ): List<Message> {
        val messages = mutableListOf<Message>()

        messages.add(
            Message.System(
                content = "You are a helpful assistant. Respond concisely and helpfully.",
                metaInfo = RequestMetaInfo.create(Clock.System),
            )
        )

        history.forEach { message ->
            when (message.role) {
                MessageRole.USER -> messages.add(
                    Message.User(
                        content = message.content,
                        metaInfo = RequestMetaInfo.create(Clock.System),
                    )
                )
                MessageRole.ASSISTANT -> messages.add(
                    Message.Assistant(
                        content = message.content,
                        metaInfo = ResponseMetaInfo.create(Clock.System),
                    )
                )
                MessageRole.SYSTEM -> {}
            }
        }

        messages.add(
            Message.User(
                content = userMessage,
                metaInfo = RequestMetaInfo.create(Clock.System),
            )
        )

        return messages
    }
}

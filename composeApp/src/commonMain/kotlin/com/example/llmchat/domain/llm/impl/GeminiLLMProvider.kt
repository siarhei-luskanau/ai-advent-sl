package com.example.llmchat.domain.llm.impl

import com.example.llmchat.data.model.Message
import com.example.llmchat.domain.llm.LLMProvider
import ai.koog.agents.AIAgent
import ai.koog.agents.core.prompts.Prompt
import ai.koog.agents.flow.StreamFrame
import ai.koog.llms.GoogleModels
import ai.koog.llms.simpleGoogleExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GeminiLLMProvider(private val apiKey: String) : LLMProvider {

    private fun buildPromptFromMessages(messages: List<Message>): String {
        return messages.joinToString("\n\n") { message ->
            when (message.role) {
                "user" -> "User: ${message.content}"
                "assistant" -> "Assistant: ${message.content}"
                else -> message.content
            }
        }
    }

    override suspend fun sendMessage(
        messages: List<Message>,
        systemPrompt: String?,
        model: String
    ): String {
        val executor = simpleGoogleExecutor(apiKey)
        val agent = AIAgent(
            promptExecutor = executor,
            systemPrompt = systemPrompt ?: "You are a helpful AI assistant.",
            llmModel = GoogleModels.fromString(model)
        )

        val prompt = if (messages.isNotEmpty()) {
            buildPromptFromMessages(messages)
        } else {
            "Hello"
        }

        return agent.run(Prompt(prompt))
    }

    override fun streamMessage(
        messages: List<Message>,
        systemPrompt: String?,
        model: String
    ): Flow<String> = flow {
        val executor = simpleGoogleExecutor(apiKey)
        val agent = AIAgent(
            promptExecutor = executor,
            systemPrompt = systemPrompt ?: "You are a helpful AI assistant.",
            llmModel = GoogleModels.fromString(model)
        )

        val prompt = if (messages.isNotEmpty()) {
            buildPromptFromMessages(messages)
        } else {
            "Hello"
        }

        agent.writeSession {
            val stream = requestLLMStreaming(Prompt(prompt))
            stream.collect { frame ->
                when (frame) {
                    is StreamFrame.Append -> emit(frame.text)
                    is StreamFrame.End -> return@collect
                    else -> {} // Ignore tool calls for now
                }
            }
        }
    }
}

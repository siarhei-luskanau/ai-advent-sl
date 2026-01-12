package com.example.llmchat.domain.llm

import com.example.llmchat.data.repository.SettingsRepository
import com.example.llmchat.domain.llm.impl.ClaudeLLMProvider
import com.example.llmchat.domain.llm.impl.GeminiLLMProvider
import com.example.llmchat.domain.llm.impl.OpenAILLMProvider
import kotlinx.coroutines.flow.first

class LLMProviderFactory(
    private val settingsRepository: SettingsRepository
) {
    suspend fun createProvider(type: LLMProviderType): LLMProvider {
        val settings = settingsRepository.getSettings().first()

        return when (type) {
            LLMProviderType.CLAUDE -> {
                val apiKey = settings.claudeApiKey
                    ?: throw IllegalStateException("Claude API key not configured")
                ClaudeLLMProvider(apiKey)
            }
            LLMProviderType.OPENAI -> {
                val apiKey = settings.openaiApiKey
                    ?: throw IllegalStateException("OpenAI API key not configured")
                OpenAILLMProvider(apiKey)
            }
            LLMProviderType.GEMINI -> {
                val apiKey = settings.geminiApiKey
                    ?: throw IllegalStateException("Gemini API key not configured")
                GeminiLLMProvider(apiKey)
            }
        }
    }
}

package com.example.llmchat.domain.usecase.settings

import com.example.llmchat.data.repository.SettingsRepository
import com.example.llmchat.domain.llm.LLMProviderType

class SaveApiKeyUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(provider: LLMProviderType, apiKey: String) {
        when (provider) {
            LLMProviderType.CLAUDE -> settingsRepository.updateClaudeApiKey(apiKey)
            LLMProviderType.OPENAI -> settingsRepository.updateOpenAIApiKey(apiKey)
            LLMProviderType.GEMINI -> settingsRepository.updateGeminiApiKey(apiKey)
        }
    }
}

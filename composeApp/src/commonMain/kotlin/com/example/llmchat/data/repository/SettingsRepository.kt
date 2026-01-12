package com.example.llmchat.data.repository

import com.example.llmchat.data.local.datastore.AppSettings
import com.example.llmchat.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.flow.Flow

class SettingsRepository(
    private val settingsDataStore: SettingsDataStore
) {
    fun getSettings(): Flow<AppSettings> = settingsDataStore.settings

    suspend fun updateClaudeApiKey(apiKey: String) {
        settingsDataStore.updateClaudeApiKey(apiKey)
    }

    suspend fun updateOpenAIApiKey(apiKey: String) {
        settingsDataStore.updateOpenAIApiKey(apiKey)
    }

    suspend fun updateGeminiApiKey(apiKey: String) {
        settingsDataStore.updateGeminiApiKey(apiKey)
    }

    suspend fun updateDefaultProvider(provider: String) {
        settingsDataStore.updateDefaultProvider(provider)
    }

    suspend fun updateDefaultModel(model: String) {
        settingsDataStore.updateDefaultModel(model)
    }

    suspend fun updateStreamingEnabled(enabled: Boolean) {
        settingsDataStore.updateStreamingEnabled(enabled)
    }
}

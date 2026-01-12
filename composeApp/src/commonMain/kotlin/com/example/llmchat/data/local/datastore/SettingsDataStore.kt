package com.example.llmchat.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsDataStore(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val CLAUDE_API_KEY = stringPreferencesKey("claude_api_key")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val DEFAULT_PROVIDER = stringPreferencesKey("default_provider")
        val DEFAULT_MODEL = stringPreferencesKey("default_model")
        val STREAMING_ENABLED = booleanPreferencesKey("streaming_enabled")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            claudeApiKey = prefs[Keys.CLAUDE_API_KEY],
            openaiApiKey = prefs[Keys.OPENAI_API_KEY],
            geminiApiKey = prefs[Keys.GEMINI_API_KEY],
            defaultProvider = prefs[Keys.DEFAULT_PROVIDER] ?: "CLAUDE",
            defaultModel = prefs[Keys.DEFAULT_MODEL] ?: "claude-3-5-sonnet-20241022",
            streamingEnabled = prefs[Keys.STREAMING_ENABLED] ?: true
        )
    }

    suspend fun updateClaudeApiKey(apiKey: String) {
        dataStore.edit { prefs ->
            prefs[Keys.CLAUDE_API_KEY] = apiKey
        }
    }

    suspend fun updateOpenAIApiKey(apiKey: String) {
        dataStore.edit { prefs ->
            prefs[Keys.OPENAI_API_KEY] = apiKey
        }
    }

    suspend fun updateGeminiApiKey(apiKey: String) {
        dataStore.edit { prefs ->
            prefs[Keys.GEMINI_API_KEY] = apiKey
        }
    }

    suspend fun updateDefaultProvider(provider: String) {
        dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_PROVIDER] = provider
        }
    }

    suspend fun updateDefaultModel(model: String) {
        dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_MODEL] = model
        }
    }

    suspend fun updateStreamingEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.STREAMING_ENABLED] = enabled
        }
    }
}

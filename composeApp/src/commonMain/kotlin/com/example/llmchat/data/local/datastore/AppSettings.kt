package com.example.llmchat.data.local.datastore

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val claudeApiKey: String? = null,
    val openaiApiKey: String? = null,
    val geminiApiKey: String? = null,
    val defaultProvider: String = "CLAUDE",
    val defaultModel: String = "claude-3-5-sonnet-20241022",
    val streamingEnabled: Boolean = true
)

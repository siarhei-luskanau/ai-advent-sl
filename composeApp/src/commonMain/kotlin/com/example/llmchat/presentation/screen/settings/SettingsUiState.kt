package com.example.llmchat.presentation.screen.settings

import com.example.llmchat.data.local.datastore.AppSettings

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isSaving: Boolean = false
)

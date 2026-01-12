package com.example.llmchat.domain.usecase.settings

import com.example.llmchat.data.local.datastore.AppSettings
import com.example.llmchat.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> {
        return settingsRepository.getSettings()
    }
}

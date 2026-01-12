package com.example.llmchat.presentation.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llmchat.domain.llm.LLMProviderType
import com.example.llmchat.domain.usecase.settings.GetSettingsUseCase
import com.example.llmchat.domain.usecase.settings.SaveApiKeyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val saveApiKeyUseCase: SaveApiKeyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun saveApiKey(provider: LLMProviderType, apiKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            saveApiKeyUseCase(provider, apiKey)
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}

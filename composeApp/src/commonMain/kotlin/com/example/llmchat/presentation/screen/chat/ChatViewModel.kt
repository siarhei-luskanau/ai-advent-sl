package com.example.llmchat.presentation.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llmchat.data.repository.ConversationRepository
import com.example.llmchat.data.repository.MessageRepository
import com.example.llmchat.data.repository.SettingsRepository
import com.example.llmchat.domain.usecase.chat.SendMessageUseCase
import com.example.llmchat.domain.usecase.chat.StreamMessageUseCase
import com.example.llmchat.domain.usecase.chat.StreamResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val conversationId: String,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val settingsRepository: SettingsRepository,
    private val streamMessageUseCase: StreamMessageUseCase,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadConversation()
        loadMessages()
    }

    private fun loadConversation() {
        viewModelScope.launch {
            val conversation = conversationRepository.getConversationById(conversationId)
            _uiState.update { it.copy(conversation = conversation) }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            messageRepository.getMessages(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val settings = settingsRepository.getSettings().first()

            if (settings.streamingEnabled) {
                sendStreamingMessage(text)
            } else {
                sendNonStreamingMessage(text)
            }
        }
    }

    private suspend fun sendStreamingMessage(text: String) {
        _uiState.update { it.copy(isStreaming = true, streamingMessage = "", error = null) }

        streamMessageUseCase(conversationId, text).collect { result ->
            when (result) {
                is StreamResult.Loading -> {
                    _uiState.update { it.copy(isStreaming = true) }
                }
                is StreamResult.Chunk -> {
                    _uiState.update { it.copy(streamingMessage = result.fullText) }
                }
                is StreamResult.Complete -> {
                    _uiState.update { it.copy(isStreaming = false, streamingMessage = "") }
                }
                is StreamResult.Error -> {
                    _uiState.update {
                        it.copy(isStreaming = false, streamingMessage = "", error = result.message)
                    }
                }
            }
        }
    }

    private suspend fun sendNonStreamingMessage(text: String) {
        _uiState.update { it.copy(isSending = true, error = null) }

        sendMessageUseCase(conversationId, text)
            .onSuccess {
                _uiState.update { it.copy(isSending = false) }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(isSending = false, error = error.message)
                }
            }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

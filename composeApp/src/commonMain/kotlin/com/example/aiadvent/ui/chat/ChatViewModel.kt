package com.example.aiadvent.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiadvent.data.model.Message
import com.example.aiadvent.domain.usecase.GetMessagesUseCase
import com.example.aiadvent.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val chatId: String = "default"
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            getMessagesUseCase(chatId)
                .catch { e ->
                    _uiState.value = ChatUiState.Error(e.message ?: "Unknown error")
                }
                .collect { messages ->
                    _uiState.value = ChatUiState.Success(messages)
                }
        }
    }

    fun onMessageInputChanged(text: String) {
        _messageInput.value = text
    }

    fun sendMessage() {
        val message = _messageInput.value.trim()
        if (message.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { currentState ->
                if (currentState is ChatUiState.Success) {
                    currentState.copy(isLoading = true)
                } else {
                    currentState
                }
            }

            sendMessageUseCase(chatId, message)
                .onSuccess {
                    _messageInput.value = ""
                    _uiState.update { currentState ->
                        if (currentState is ChatUiState.Success) {
                            currentState.copy(isLoading = false)
                        } else {
                            currentState
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update { currentState ->
                        if (currentState is ChatUiState.Success) {
                            currentState.copy(
                                isLoading = false,
                                error = e.message
                            )
                        } else {
                            currentState
                        }
                    }
                }
        }
    }
}

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data class Success(
        val messages: List<Message>,
        val isLoading: Boolean = false,
        val error: String? = null
    ) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

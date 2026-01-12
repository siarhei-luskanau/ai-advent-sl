package com.example.llmchat.presentation.screen.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llmchat.domain.usecase.conversation.DeleteConversationUseCase
import com.example.llmchat.domain.usecase.conversation.GetConversationsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConversationsViewModel(
    private val getConversationsUseCase: GetConversationsUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getConversationsUseCase().collect { conversations ->
                _uiState.update {
                    it.copy(
                        conversations = conversations,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            deleteConversationUseCase(conversationId)
        }
    }
}

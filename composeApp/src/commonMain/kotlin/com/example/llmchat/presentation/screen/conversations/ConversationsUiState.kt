package com.example.llmchat.presentation.screen.conversations

import com.example.llmchat.data.model.Conversation

data class ConversationsUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = false
)

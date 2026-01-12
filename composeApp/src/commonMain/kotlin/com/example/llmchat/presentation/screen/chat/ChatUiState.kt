package com.example.llmchat.presentation.screen.chat

import com.example.llmchat.data.model.Conversation
import com.example.llmchat.data.model.Message

data class ChatUiState(
    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val streamingMessage: String = "",
    val isStreaming: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

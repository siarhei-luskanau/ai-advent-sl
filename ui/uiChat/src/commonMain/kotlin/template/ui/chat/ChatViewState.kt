package template.ui.chat

import template.ui.chat.model.ChatMessage

sealed interface ChatViewState {
    object Loading : ChatViewState

    data class Success(
        val messages: List<ChatMessage>,
        val isGenerating: Boolean = false,
        val inputText: String = "",
    ) : ChatViewState

    data class Error(
        val error: Throwable,
        val messages: List<ChatMessage>,
    ) : ChatViewState
}

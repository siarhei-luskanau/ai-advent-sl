package template.ui.chat

sealed interface ChatViewEvent {
    data object NavigateBack : ChatViewEvent

    data class SendMessage(
        val text: String,
    ) : ChatViewEvent

    data class UpdateInputText(
        val text: String,
    ) : ChatViewEvent

    data object RetryLastMessage : ChatViewEvent

    data object ClearConversation : ChatViewEvent
}

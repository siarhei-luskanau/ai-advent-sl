package template.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import template.core.common.DispatcherSet
import template.ui.chat.model.ChatMessage
import template.ui.chat.model.MessageRole
import template.ui.chat.service.ChatService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ChatViewModel(
    private val chatService: ChatService,
    private val dispatcherSet: DispatcherSet,
    private val navigationCallback: ChatNavigationCallback,
) : ViewModel() {
    val viewState =
        MutableStateFlow<ChatViewState>(
            ChatViewState.Success(
                messages = emptyList(),
                isGenerating = false,
                inputText = "",
            ),
        )

    private var lastUserMessage: String? = null

    fun onEvent(event: ChatViewEvent) {
        when (event) {
            ChatViewEvent.NavigateBack -> {
                viewModelScope.launch {
                    navigationCallback.goBack()
                }
            }

            is ChatViewEvent.SendMessage -> {
                sendMessage(event.text)
            }

            is ChatViewEvent.UpdateInputText -> {
                updateInputText(event.text)
            }

            ChatViewEvent.RetryLastMessage -> {
                lastUserMessage?.let { sendMessage(it) }
            }

            ChatViewEvent.ClearConversation -> {
                viewState.value =
                    ChatViewState.Success(
                        messages = emptyList(),
                        isGenerating = false,
                        inputText = "",
                    )
                lastUserMessage = null
            }
        }
    }

    private fun updateInputText(text: String) {
        val currentState = viewState.value
        if (currentState is ChatViewState.Success) {
            viewState.value = currentState.copy(inputText = text)
        }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return

        val currentState = viewState.value
        val currentMessages =
            when (currentState) {
                is ChatViewState.Success -> currentState.messages
                is ChatViewState.Error -> currentState.messages
                ChatViewState.Loading -> emptyList()
            }

        lastUserMessage = text

        val userMessage =
            ChatMessage(
                id = Uuid.random().toString(),
                role = MessageRole.USER,
                content = text,
                timestamp = Clock.System.now(),
            )

        val updatedMessages = currentMessages + userMessage

        viewState.value =
            ChatViewState.Success(
                messages = updatedMessages,
                isGenerating = true,
                inputText = "",
            )

        viewModelScope.launch(dispatcherSet.ioDispatcher()) {
            chatService
                .sendMessage(text, currentMessages)
                .onSuccess { response ->
                    val assistantMessage =
                        ChatMessage(
                            id = Uuid.random().toString(),
                            role = MessageRole.ASSISTANT,
                            content = response,
                            timestamp = Clock.System.now(),
                        )
                    viewState.update { state ->
                        when (state) {
                            is ChatViewState.Success -> {
                                state.copy(
                                    messages = state.messages + assistantMessage,
                                    isGenerating = false,
                                )
                            }

                            is ChatViewState.Error -> {
                                ChatViewState.Success(
                                    messages = state.messages + assistantMessage,
                                    isGenerating = false,
                                )
                            }

                            ChatViewState.Loading -> {
                                ChatViewState.Success(
                                    messages = listOf(assistantMessage),
                                    isGenerating = false,
                                )
                            }
                        }
                    }
                }.onFailure { error ->
                    viewState.update { state ->
                        val messages =
                            when (state) {
                                is ChatViewState.Success -> state.messages
                                is ChatViewState.Error -> state.messages
                                ChatViewState.Loading -> emptyList()
                            }
                        ChatViewState.Error(
                            error = error,
                            messages = messages,
                        )
                    }
                }
        }
    }
}

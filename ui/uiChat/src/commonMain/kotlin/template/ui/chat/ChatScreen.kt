package template.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import template.ui.chat.components.ChatInputField
import template.ui.chat.components.MessageList
import template.ui.common.resources.Res
import template.ui.common.resources.back_button
import template.ui.common.resources.ic_arrow_back

@Composable
fun ChatScreen(viewModelProvider: () -> ChatViewModel) {
    val viewModel = viewModel { viewModelProvider() }
    ChatContent(
        viewStateFlow = viewModel.viewState,
        onEvent = viewModel::onEvent,
    )
}

@Composable
internal fun ChatContent(
    viewStateFlow: StateFlow<ChatViewState>,
    onEvent: (ChatViewEvent) -> Unit,
) {
    val viewState = viewStateFlow.collectAsState()
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ChatViewEvent.NavigateBack) }) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_arrow_back),
                            contentDescription = stringResource(Res.string.back_button),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(ChatViewEvent.ClearConversation) }) {
                        Text("Clear")
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .imePadding(),
        ) {
            when (val state = viewState.value) {
                ChatViewState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Loading...")
                    }
                }

                is ChatViewState.Success -> {
                    MessageList(
                        messages = state.messages,
                        isGenerating = state.isGenerating,
                        modifier = Modifier.weight(1f),
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 8.dp,
                    ) {
                        ChatInputField(
                            value = state.inputText,
                            onValueChange = { onEvent(ChatViewEvent.UpdateInputText(it)) },
                            onSendClick = { onEvent(ChatViewEvent.SendMessage(state.inputText)) },
                            enabled = !state.isGenerating,
                        )
                    }
                }

                is ChatViewState.Error -> {
                    MessageList(
                        messages = state.messages,
                        isGenerating = false,
                        modifier = Modifier.weight(1f),
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Error: ${state.error.message ?: "Unknown error"}",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(
                                onClick = { onEvent(ChatViewEvent.RetryLastMessage) },
                                modifier = Modifier.padding(top = 8.dp),
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

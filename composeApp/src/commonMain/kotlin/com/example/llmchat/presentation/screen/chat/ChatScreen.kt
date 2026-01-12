package com.example.llmchat.presentation.screen.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.llmchat.data.model.Message
import com.example.llmchat.presentation.screen.chat.components.MessageBubble
import com.example.llmchat.presentation.screen.chat.components.MessageInput
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size, uiState.streamingMessage) {
        if (uiState.messages.isNotEmpty() || uiState.streamingMessage.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.conversation?.title ?: "Chat") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    actionColor = MaterialTheme.colorScheme.error
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages list
            Box(
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.messages.isEmpty() && !uiState.isStreaming) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Start a conversation",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Send a message to begin",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Show streaming message at bottom (top in reverse layout)
                        if (uiState.isStreaming && uiState.streamingMessage.isNotEmpty()) {
                            item(key = "streaming") {
                                MessageBubble(
                                    message = Message(
                                        id = "streaming",
                                        conversationId = conversationId,
                                        role = "assistant",
                                        content = uiState.streamingMessage,
                                        timestamp = Clock.System.now().toEpochMilliseconds()
                                    ),
                                    isStreaming = true
                                )
                            }
                        }

                        // Regular messages
                        items(
                            items = uiState.messages.reversed(),
                            key = { it.id }
                        ) { message ->
                            MessageBubble(message = message)
                        }
                    }
                }
            }

            // Input area
            MessageInput(
                onSendMessage = viewModel::sendMessage,
                enabled = !uiState.isSending && !uiState.isStreaming
            )
        }
    }
}

package com.example.llmchat.presentation.screen.conversations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.llmchat.domain.llm.LLMProviderType
import com.example.llmchat.domain.usecase.conversation.CreateConversationUseCase
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationDialog(
    onConversationCreated: (String) -> Unit,
    onDismiss: () -> Unit,
    createConversationUseCase: CreateConversationUseCase = koinInject()
) {
    var title by remember { mutableStateOf("") }
    var systemPrompt by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf(LLMProviderType.CLAUDE) }
    var providerExpanded by remember { mutableStateOf(false) }
    var model by remember { mutableStateOf("claude-3-5-sonnet-20241022") }

    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "New Conversation",
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProvider.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("LLM Provider") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false }
                    ) {
                        LLMProviderType.entries.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.name) },
                                onClick = {
                                    selectedProvider = provider
                                    model = when (provider) {
                                        LLMProviderType.CLAUDE -> "claude-3-5-sonnet-20241022"
                                        LLMProviderType.OPENAI -> "gpt-4"
                                        LLMProviderType.GEMINI -> "gemini-pro"
                                    }
                                    providerExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("System Prompt (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    placeholder = { Text("You are a helpful AI assistant.") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val conversationId = createConversationUseCase(
                                    title = title.ifBlank { "New Conversation" },
                                    systemPrompt = systemPrompt.ifBlank { null },
                                    provider = selectedProvider.name,
                                    model = model
                                )
                                onConversationCreated(conversationId)
                            }
                        },
                        enabled = title.isNotBlank() && model.isNotBlank()
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

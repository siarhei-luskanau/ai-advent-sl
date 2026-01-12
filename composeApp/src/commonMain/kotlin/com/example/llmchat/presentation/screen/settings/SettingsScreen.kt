package com.example.llmchat.presentation.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.llmchat.domain.llm.LLMProviderType
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "API Keys",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Configure your API keys for different LLM providers. Keys are stored locally and never shared.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ApiKeyInput(
                label = "Anthropic Claude API Key",
                value = uiState.settings.claudeApiKey ?: "",
                onSave = { viewModel.saveApiKey(LLMProviderType.CLAUDE, it) },
                enabled = !uiState.isSaving
            )

            ApiKeyInput(
                label = "OpenAI API Key",
                value = uiState.settings.openaiApiKey ?: "",
                onSave = { viewModel.saveApiKey(LLMProviderType.OPENAI, it) },
                enabled = !uiState.isSaving
            )

            ApiKeyInput(
                label = "Google Gemini API Key",
                value = uiState.settings.geminiApiKey ?: "",
                onSave = { viewModel.saveApiKey(LLMProviderType.GEMINI, it) },
                enabled = !uiState.isSaving
            )
        }
    }
}

@Composable
fun ApiKeyInput(
    label: String,
    value: String,
    onSave: (String) -> Unit,
    enabled: Boolean
) {
    var apiKey by remember(value) { mutableStateOf(value) }
    var isVisible by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter API key") },
                visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isVisible = !isVisible }) {
                        Icon(
                            if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isVisible) "Hide" else "Show"
                        )
                    }
                },
                enabled = enabled,
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { onSave(apiKey) },
                enabled = enabled && apiKey.isNotBlank() && apiKey != value
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
            }
        }

        if (value.isNotBlank()) {
            Text(
                text = "✓ API key configured",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

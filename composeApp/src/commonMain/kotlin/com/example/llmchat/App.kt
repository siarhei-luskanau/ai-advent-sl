package com.example.llmchat

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.example.llmchat.presentation.navigation.AppNavigation
import com.example.llmchat.presentation.theme.LLMChatTheme

@Composable
fun App() {
    LLMChatTheme {
        Surface {
            AppNavigation()
        }
    }
}

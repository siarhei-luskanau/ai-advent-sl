package com.example.llmchat

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.llmchat.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "LLM Chat"
        ) {
            App()
        }
    }
}

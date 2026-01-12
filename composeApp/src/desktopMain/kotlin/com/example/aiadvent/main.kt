package com.example.aiadvent

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.aiadvent.di.appModule
import com.example.aiadvent.di.platformModule
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModule, platformModule())
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "AI Advent - Chat with LLM"
        ) {
            App()
        }
    }
}

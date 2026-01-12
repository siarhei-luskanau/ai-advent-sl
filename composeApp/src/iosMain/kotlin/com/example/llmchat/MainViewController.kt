package com.example.llmchat

import androidx.compose.ui.window.ComposeUIViewController
import com.example.llmchat.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) {
    App()
}

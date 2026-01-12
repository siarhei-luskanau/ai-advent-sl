package com.example.aiadvent

import androidx.compose.ui.window.ComposeUIViewController
import com.example.aiadvent.di.appModule
import com.example.aiadvent.di.platformModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) {
    App()
}

private fun initKoin() {
    startKoin {
        modules(appModule, platformModule())
    }
}

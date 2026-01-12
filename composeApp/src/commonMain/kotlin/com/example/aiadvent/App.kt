package com.example.aiadvent

import androidx.compose.runtime.Composable
import com.example.aiadvent.ui.navigation.AppNavigation
import com.example.aiadvent.ui.theme.AppTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        AppTheme {
            AppNavigation()
        }
    }
}

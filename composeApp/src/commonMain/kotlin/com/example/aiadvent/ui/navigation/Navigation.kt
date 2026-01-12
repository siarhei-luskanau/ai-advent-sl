package com.example.aiadvent.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aiadvent.ui.chat.ChatScreen

@Composable
fun AppNavigation() {
    val navController = androidx.navigation.compose.rememberNavController()

    androidx.navigation.compose.NavHost(
        navController = navController,
        startDestination = "chat"
    ) {
        composable("chat") {
            ChatScreen()
        }
    }
}

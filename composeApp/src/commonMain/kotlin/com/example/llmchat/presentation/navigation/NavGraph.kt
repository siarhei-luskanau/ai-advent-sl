package com.example.llmchat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.llmchat.presentation.screen.chat.ChatScreen
import com.example.llmchat.presentation.screen.chat.ChatViewModel
import com.example.llmchat.presentation.screen.conversations.ConversationsScreen
import com.example.llmchat.presentation.screen.conversations.NewConversationDialog
import com.example.llmchat.presentation.screen.settings.SettingsScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Conversations
    ) {
        composable<Screen.Conversations> {
            ConversationsScreen(
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.Chat(conversationId))
                },
                onNewConversation = {
                    navController.navigate(Screen.NewConversation)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings)
                }
            )
        }

        composable<Screen.Chat> { backStackEntry ->
            val chat: Screen.Chat = backStackEntry.toRoute()
            val viewModel: ChatViewModel = koinViewModel { parametersOf(chat.conversationId) }

            ChatScreen(
                conversationId = chat.conversationId,
                onBackClick = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        composable<Screen.Settings> {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<Screen.NewConversation> {
            NewConversationDialog(
                onConversationCreated = { conversationId ->
                    navController.navigate(Screen.Chat(conversationId)) {
                        popUpTo(Screen.Conversations)
                    }
                },
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}

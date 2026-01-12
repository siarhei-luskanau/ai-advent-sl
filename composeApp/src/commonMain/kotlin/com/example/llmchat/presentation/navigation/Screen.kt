package com.example.llmchat.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Conversations : Screen

    @Serializable
    data class Chat(val conversationId: String) : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object NewConversation : Screen
}

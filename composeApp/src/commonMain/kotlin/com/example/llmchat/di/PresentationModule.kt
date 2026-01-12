package com.example.llmchat.di

import com.example.llmchat.presentation.screen.chat.ChatViewModel
import com.example.llmchat.presentation.screen.conversations.ConversationsViewModel
import com.example.llmchat.presentation.screen.settings.SettingsViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    // Simple ViewModels
    viewModelOf(::ConversationsViewModel)
    viewModelOf(::SettingsViewModel)

    // ChatViewModel needs conversationId parameter
    viewModel { params ->
        ChatViewModel(
            conversationId = params.get(),
            conversationRepository = get(),
            messageRepository = get(),
            settingsRepository = get(),
            streamMessageUseCase = get(),
            sendMessageUseCase = get()
        )
    }
}

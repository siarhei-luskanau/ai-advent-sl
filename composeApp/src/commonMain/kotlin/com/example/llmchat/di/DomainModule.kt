package com.example.llmchat.di

import com.example.llmchat.domain.llm.LLMProviderFactory
import com.example.llmchat.domain.usecase.chat.SendMessageUseCase
import com.example.llmchat.domain.usecase.chat.StreamMessageUseCase
import com.example.llmchat.domain.usecase.conversation.CreateConversationUseCase
import com.example.llmchat.domain.usecase.conversation.DeleteConversationUseCase
import com.example.llmchat.domain.usecase.conversation.GetConversationsUseCase
import com.example.llmchat.domain.usecase.settings.GetSettingsUseCase
import com.example.llmchat.domain.usecase.settings.SaveApiKeyUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {
    // LLM Provider Factory
    singleOf(::LLMProviderFactory)

    // Use Cases
    factoryOf(::SendMessageUseCase)
    factoryOf(::StreamMessageUseCase)
    factoryOf(::CreateConversationUseCase)
    factoryOf(::GetConversationsUseCase)
    factoryOf(::DeleteConversationUseCase)
    factoryOf(::SaveApiKeyUseCase)
    factoryOf(::GetSettingsUseCase)
}

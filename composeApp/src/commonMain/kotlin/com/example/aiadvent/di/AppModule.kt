package com.example.aiadvent.di

import com.example.aiadvent.data.local.AppDatabase
import com.example.aiadvent.data.remote.ApiClient
import com.example.aiadvent.data.remote.LLMRemoteDataSource
import com.example.aiadvent.data.repository.ChatRepository
import com.example.aiadvent.data.repository.ChatRepositoryImpl
import com.example.aiadvent.domain.usecase.GetMessagesUseCase
import com.example.aiadvent.domain.usecase.SendMessageUseCase
import com.example.aiadvent.ui.chat.ChatViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // Database - provided by platform module
    single { get<AppDatabase>().messageDao() }

    // Network
    singleOf(::ApiClient)
    singleOf(::LLMRemoteDataSource)

    // Repository
    single<ChatRepository> { ChatRepositoryImpl(get(), get()) }

    // Use Cases
    factoryOf(::GetMessagesUseCase)
    factoryOf(::SendMessageUseCase)

    // ViewModels
    viewModelOf(::ChatViewModel)
}

expect fun platformModule(): Module

package com.example.llmchat.di

import com.example.llmchat.data.local.database.getDatabaseBuilder
import com.example.llmchat.data.local.datastore.SettingsDataStore
import com.example.llmchat.data.local.datastore.createDataStore
import com.example.llmchat.data.repository.ConversationRepository
import com.example.llmchat.data.repository.MessageRepository
import com.example.llmchat.data.repository.SettingsRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {
    // Database
    single { getDatabaseBuilder().build() }
    single { get<com.example.llmchat.data.local.database.AppDatabase>().conversationDao() }
    single { get<com.example.llmchat.data.local.database.AppDatabase>().messageDao() }

    // DataStore
    single { createDataStore() }
    singleOf(::SettingsDataStore)

    // Repositories
    singleOf(::ConversationRepository)
    singleOf(::MessageRepository)
    singleOf(::SettingsRepository)
}

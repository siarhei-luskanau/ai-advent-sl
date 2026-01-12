package com.example.llmchat.di

import com.example.llmchat.data.local.database.getDatabaseBuilder
import com.example.llmchat.data.local.datastore.createDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { getDatabaseBuilder(androidContext()) }
    single { createDataStore(androidContext()) }
}

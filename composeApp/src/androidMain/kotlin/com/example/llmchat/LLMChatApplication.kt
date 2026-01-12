package com.example.llmchat

import android.app.Application
import com.example.llmchat.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class LLMChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@LLMChatApplication)
            androidLogger()
        }
    }
}

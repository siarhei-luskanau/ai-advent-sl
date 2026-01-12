package com.example.aiadvent

import android.app.Application
import com.example.aiadvent.di.appModule
import com.example.aiadvent.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AiAdventApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@AiAdventApplication)
            modules(appModule, platformModule())
        }
    }
}

package com.example.llmchat.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            platformModule,
            dataModule,
            domainModule,
            presentationModule
        )
    }
}

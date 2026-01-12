package com.example.aiadvent.di

import com.example.aiadvent.util.getDatabaseBuilder
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { getDatabaseBuilder().build() }
}

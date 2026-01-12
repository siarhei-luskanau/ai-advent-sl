package com.example.llmchat.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

fun createDataStore(context: Context): DataStore<Preferences> {
    return context.dataStore
}

actual fun createDataStore(): DataStore<Preferences> {
    throw IllegalStateException("createDataStore() should not be called without context on Android")
}

package com.example.llmchat.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import java.io.File

actual fun createDataStore(): DataStore<Preferences> {
    val dataStoreFile = File(System.getProperty("user.home"), ".llmchat/settings.preferences_pb")
    dataStoreFile.parentFile.mkdirs()
    return createDataStoreWithPath {
        dataStoreFile.absolutePath
    }
}

private fun createDataStoreWithPath(producePath: () -> String): DataStore<Preferences> =
    androidx.datastore.preferences.core.PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

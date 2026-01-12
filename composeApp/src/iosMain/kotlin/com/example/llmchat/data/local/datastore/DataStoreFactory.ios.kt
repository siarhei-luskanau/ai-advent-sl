package com.example.llmchat.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun createDataStore(): DataStore<Preferences> {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    val dataStoreFile = requireNotNull(documentDirectory?.path) + "/settings.preferences_pb"
    return createDataStoreWithPath {
        dataStoreFile
    }
}

private fun createDataStoreWithPath(producePath: () -> String): DataStore<Preferences> =
    androidx.datastore.preferences.core.PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().let { kotlinx.io.files.Path(it) } }
    )

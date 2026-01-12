package com.example.llmchat.data.local.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("user.home"), ".llmchat/llmchat.db")
    dbFile.parentFile.mkdirs()
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath
    )
}

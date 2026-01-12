package com.example.llmchat.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val dbFile = context.getDatabasePath("llmchat.db")
    return Room.databaseBuilder<AppDatabase>(
        context = context,
        name = dbFile.absolutePath
    )
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    throw IllegalStateException("getDatabaseBuilder() should not be called without context on Android")
}

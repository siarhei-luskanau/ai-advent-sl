package com.example.aiadvent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.aiadvent.data.local.dao.MessageDao
import com.example.aiadvent.data.local.entity.MessageEntity

@Database(
    entities = [MessageEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}

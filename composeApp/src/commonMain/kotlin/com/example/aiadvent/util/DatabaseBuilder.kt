package com.example.aiadvent.util

import androidx.room.RoomDatabase
import com.example.aiadvent.data.local.AppDatabase

expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

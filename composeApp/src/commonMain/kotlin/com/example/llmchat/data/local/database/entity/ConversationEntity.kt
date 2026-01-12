package com.example.llmchat.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    @OptIn(ExperimentalUuidApi::class)
    val id: String = Uuid.random().toString(),
    val title: String,
    val systemPrompt: String? = null,
    val llmProvider: String,
    val modelName: String,
    val createdAt: Long,
    val updatedAt: Long
)

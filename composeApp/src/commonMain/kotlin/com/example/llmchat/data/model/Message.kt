package com.example.llmchat.data.model

import com.example.llmchat.data.local.database.entity.MessageEntity

data class Message(
    val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val timestamp: Long
)

fun MessageEntity.toDomain() = Message(
    id = id,
    conversationId = conversationId,
    role = role,
    content = content,
    timestamp = timestamp
)

fun Message.toEntity() = MessageEntity(
    id = id,
    conversationId = conversationId,
    role = role,
    content = content,
    timestamp = timestamp
)

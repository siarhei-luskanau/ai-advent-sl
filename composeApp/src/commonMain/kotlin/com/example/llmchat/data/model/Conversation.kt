package com.example.llmchat.data.model

import com.example.llmchat.data.local.database.entity.ConversationEntity

data class Conversation(
    val id: String,
    val title: String,
    val systemPrompt: String?,
    val llmProvider: String,
    val modelName: String,
    val createdAt: Long,
    val updatedAt: Long
)

fun ConversationEntity.toDomain() = Conversation(
    id = id,
    title = title,
    systemPrompt = systemPrompt,
    llmProvider = llmProvider,
    modelName = modelName,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Conversation.toEntity() = ConversationEntity(
    id = id,
    title = title,
    systemPrompt = systemPrompt,
    llmProvider = llmProvider,
    modelName = modelName,
    createdAt = createdAt,
    updatedAt = updatedAt
)

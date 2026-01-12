package com.example.aiadvent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.aiadvent.data.model.Message
import com.example.aiadvent.data.model.MessageRole
import kotlinx.datetime.Instant

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val content: String,
    val role: String,
    val timestamp: Long,
    val chatId: String
)

fun MessageEntity.toMessage(): Message {
    return Message(
        id = id,
        content = content,
        role = MessageRole.valueOf(role),
        timestamp = Instant.fromEpochMilliseconds(timestamp),
        chatId = chatId
    )
}

fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        content = content,
        role = role.name,
        timestamp = timestamp.toEpochMilliseconds(),
        chatId = chatId
    )
}

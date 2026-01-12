package com.example.llmchat.domain.usecase.chat

import com.example.llmchat.data.model.Message

sealed class StreamResult {
    data object Loading : StreamResult()
    data class Chunk(val chunk: String, val fullText: String) : StreamResult()
    data class Complete(val message: Message) : StreamResult()
    data class Error(val message: String) : StreamResult()
}

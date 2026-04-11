package template.ui.chat.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Instant,
    val isStreaming: Boolean = false,
)

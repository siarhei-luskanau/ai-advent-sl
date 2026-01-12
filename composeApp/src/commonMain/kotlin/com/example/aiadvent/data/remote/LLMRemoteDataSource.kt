package com.example.aiadvent.data.remote

import com.example.aiadvent.data.model.Message
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class LLMRemoteDataSource(private val apiClient: ApiClient) {

    suspend fun sendMessage(
        messages: List<Message>,
        apiUrl: String = "https://api.example.com/chat/completions"
    ): LLMResponse {
        val request = LLMRequest(
            messages = messages.map {
                LLMMessage(role = it.role.name.lowercase(), content = it.content)
            }
        )

        return apiClient.httpClient.post(apiUrl) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}

@Serializable
data class LLMRequest(
    val messages: List<LLMMessage>,
    val model: String = "gpt-4",
    val temperature: Double = 0.7
)

@Serializable
data class LLMMessage(
    val role: String,
    val content: String
)

@Serializable
data class LLMResponse(
    val id: String,
    val choices: List<LLMChoice>
)

@Serializable
data class LLMChoice(
    val message: LLMMessage,
    val finishReason: String? = null
)

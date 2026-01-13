package ai.advent

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
)

@Serializable
data class OllamaResponse(
    val model: String,
    val response: String,
    val done: Boolean,
)

fun main() =
    runBlocking {
        println("=== Interesting Facts Finder ===")
        println()

        print("Enter a topic: ")
        val topic = readln().trim()

        if (topic.isEmpty()) {
            println("Error: Topic cannot be empty")
            return@runBlocking
        }

        println()
        println("Fetching interesting facts about '$topic'...")
        println()

        val client =
            HttpClient(Apache5) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        },
                    )
                }
            }

        val prompt =
            """
            You are a knowledgeable assistant that provides interesting facts.

            Find 5 interesting facts about $topic.

            Return the response as a JSON object with the following structure:
            {
                "topic": "the topic name",
                "facts": [
                    "fact 1",
                    "fact 2",
                    "fact 3",
                    "fact 4",
                    "fact 5"
                ]
            }

            Return ONLY valid JSON, no additional text or explanation before or after the JSON.
            """.trimIndent()

        try {
            val responseText =
                client
                    .post("http://localhost:11434/api/generate") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            OllamaRequest(
                                model = "qwen3:0.6b",
                                prompt = prompt,
                                stream = false,
                            ),
                        )
                    }.body<String>()

            val json = Json { ignoreUnknownKeys = true }

            val content = buildString {
                responseText.lines()
                    .filter { it.isNotBlank() }
                    .forEach { line ->
                        try {
                            val response = json.decodeFromString<OllamaResponse>(line)
                            append(response.response)
                        } catch (e: Exception) {
                        }
                    }
            }

            println("Raw LLM Response:")
            println("-".repeat(50))
            println(content)
            println("-".repeat(50))
            println()

            val jsonResponse = extractJson(content)

            if (jsonResponse != null) {
                println("Parsed JSON:")
                println("-".repeat(50))

                val json = Json.parseToJsonElement(jsonResponse)
                val jsonObject = json.jsonObject

                val topicName = jsonObject["topic"]?.jsonPrimitive?.content
                val facts = jsonObject["facts"]?.jsonArray

                println("Topic: $topicName")
                println()
                println("Facts:")
                facts?.forEachIndexed { index, fact ->
                    println("${index + 1}. ${fact.jsonPrimitive.content}")
                }
                println("-".repeat(50))
            } else {
                println("Warning: Could not extract valid JSON from response")
                println("You might need to try again or use a different model.")
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            println("\nMake sure:")
            println("1. Ollama is running (try: ollama serve)")
            println("2. qwen3:0.6b model is installed (try: ollama pull qwen3:0.6b)")
            e.printStackTrace()
        } finally {
            client.close()
        }
    }

fun extractJson(text: String): String? {
    val trimmed = text.trim()

    val jsonStart = trimmed.indexOf('{')
    val jsonEnd = trimmed.lastIndexOf('}')

    return if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
        trimmed.substring(jsonStart, jsonEnd + 1)
    } else {
        null
    }
}

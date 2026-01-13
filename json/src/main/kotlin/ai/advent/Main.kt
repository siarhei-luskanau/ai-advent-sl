package ai.advent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

        val systemPrompt =
            """
            You are a knowledgeable assistant that provides interesting facts.
            Always respond with valid JSON only, no additional text or explanation.
            """.trimIndent()

        val userPrompt =
            """
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
            val ollamaModel =
                LLModel(
                    provider = LLMProvider.Ollama,
                    id = "qwen3:0.6b",
                    capabilities =
                        listOf(
                            LLMCapability.Temperature,
                            LLMCapability.Schema.JSON.Basic,
                        ),
                    contextLength = 8192,
                )

            val agent =
                AIAgent(
                    promptExecutor = simpleOllamaAIExecutor(baseUrl = "http://localhost:11434"),
                    llmModel = ollamaModel,
                    systemPrompt = systemPrompt,
                )

            val response = agent.run(userPrompt)

            println("Raw LLM Response:")
            println("-".repeat(50))
            println(response)
            println("-".repeat(50))
            println()

            val jsonResponse = extractJson(response)

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

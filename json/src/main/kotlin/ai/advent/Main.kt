package ai.advent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
@SerialName("InterestingFacts")
data class InterestingFacts(
    val topic: String,
    val facts: List<String>,
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

        try {
            // Define the Ollama model with JSON schema capability
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

            // Create AI agent with Koog
            val ollamaClient = OllamaClient("http://localhost:11434")
            ollamaClient.getModelOrNull(ollamaModel.id)
            val agent =
                AIAgent(
                    promptExecutor = SingleLLMPromptExecutor(ollamaClient),
                    llmModel = ollamaModel,
                    systemPrompt =
                        """
                        You are a knowledgeable assistant that provides interesting facts.
                        You must respond with valid JSON only, following the exact structure provided.
                        Do not include any markdown code blocks or additional text.
                        """.trimIndent(),
                )

            // Build structured prompt with JSON schema
            val jsonSchema =
                """
                {
                  "topic": "string - the topic name",
                  "facts": ["string - fact 1", "string - fact 2", ...]
                }
                """.trimIndent()

            val userPrompt =
                """
                Find 5 interesting facts about $topic.

                Respond with a JSON object matching this structure:
                $jsonSchema

                Return only the JSON object, no additional text.
                """.trimIndent()

            // Get response from agent
            val response = agent.run(userPrompt)

            println("Raw LLM Response:")
            println("-".repeat(50))
            println(response)
            println("-".repeat(50))
            println()

            // Parse JSON response using kotlinx.serialization
            val json =
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }

            val cleanedResponse = extractJson(response) ?: response
            val result = json.decodeFromString<InterestingFacts>(cleanedResponse)

            // Display structured output
            println("Parsed Structured Output:")
            println("-".repeat(50))
            println("Topic: ${result.topic}")
            println()
            println("Facts:")
            result.facts.forEachIndexed { index, fact ->
                println("${index + 1}. $fact")
            }
            println("-".repeat(50))
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

    // Remove markdown code blocks if present
    val withoutMarkdown =
        if (trimmed.startsWith("```")) {
            trimmed
                .lines()
                .drop(1)
                .dropLast(1)
                .joinToString("\n")
                .trim()
        } else {
            trimmed
        }

    val jsonStart = withoutMarkdown.indexOf('{')
    val jsonEnd = withoutMarkdown.lastIndexOf('}')

    return if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
        withoutMarkdown.substring(jsonStart, jsonEnd + 1)
    } else {
        null
    }
}

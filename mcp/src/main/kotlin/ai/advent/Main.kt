package ai.advent

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking
import java.util.UUID

fun main() =
    runBlocking {
        // https://ollama.com/library/glm-4.7-flash
        val glm47flashModel =
            LLModel(
                provider = LLMProvider.Ollama,
                id = "glm-4.7-flash:q4_K_M",
                capabilities =
                    listOf(
                        LLMCapability.Temperature,
                        LLMCapability.Tools,
                        LLMCapability.Schema.JSON.Basic,
                    ),
                contextLength = 198 * 1024,
            )

        try {
            val ollamaClient = OllamaClient("http://localhost:11434")
            val executor = SingleLLMPromptExecutor(ollamaClient)
            ollamaClient.getModelOrNull(glm47flashModel.id)

            val prompt =
                prompt(id = UUID.randomUUID().toString()) {
                    user {
                        text(text = "hi")
                    }
                }
            val response = executor.execute(prompt = prompt, model = glm47flashModel).single()
            response.metaInfo.inputTokensCount
            response.metaInfo.outputTokensCount
            response.metaInfo.totalTokensCount

            executor.close()
        } catch (e: Exception) {
            println("Error: ${e.message}")
            println("\nMake sure:")
            println("1. Ollama is running (try: ollama serve)")
            println("2. ${glm47flashModel.id} model is installed (try: ollama pull ${glm47flashModel.id})")
            e.printStackTrace()
        }
    }

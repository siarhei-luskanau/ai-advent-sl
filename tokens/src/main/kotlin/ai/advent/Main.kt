package ai.advent

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.OllamaModels
import kotlinx.coroutines.runBlocking
import java.util.UUID

fun main() =
    runBlocking {
        // A small Llama model with 2K context https://ollama.com/library/tinyllama
        val tinyLlama2Model =
            LLModel(
                provider = LLMProvider.Ollama,
                id = "tinyllama:1.1b",
                capabilities =
                    listOf(
                        LLMCapability.Temperature,
                        LLMCapability.Schema.JSON.Basic,
                    ),
                contextLength = 2 * 1024,
            )

        val qwen3Model = OllamaModels.Alibaba.QWEN_3_06B

        try {
            // Create AI agent with Koog
            val ollamaClient = OllamaClient("http://localhost:11434")
            val executor = SingleLLMPromptExecutor(ollamaClient)
            ollamaClient.getModelOrNull(tinyLlama2Model.id)
            ollamaClient.getModelOrNull(qwen3Model.id)

            val prompt =
                prompt(id = UUID.randomUUID().toString()) {
                    user {
                        text(text = "")
                    }
                }
            val response = executor.execute(prompt = prompt, model = tinyLlama2Model).single()
            response.metaInfo.inputTokensCount
            response.metaInfo.outputTokensCount
            response.metaInfo.totalTokensCount

            executor.close()
        } catch (e: Exception) {
            println("Error: ${e.message}")
            println("\nMake sure:")
            println("1. Ollama is running (try: ollama serve)")
            println("2. ${tinyLlama2Model.id} model is installed (try: ollama pull ${tinyLlama2Model.id})")
            println("3. ${qwen3Model.id} model is installed (try: ollama pull ${qwen3Model.id})")
            e.printStackTrace()
        }
    }

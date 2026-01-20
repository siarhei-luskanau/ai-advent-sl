package ai.advent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.OllamaModels
import kotlinx.coroutines.runBlocking

fun main() =
    runBlocking {
        // https://ollama.com/library/llama2
        val llama2Model =
            LLModel(
                provider = LLMProvider.Ollama,
                id = "llama2:13b",
                capabilities =
                    listOf(
                        LLMCapability.Temperature,
                        LLMCapability.Schema.JSON.Basic,
                    ),
                contextLength = 4 * 1024,
            )

        // https://ollama.com/library/glm-4.7-flash
        val glm47Model =
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

        val qwen3Model = OllamaModels.Alibaba.QWEN_3_06B

        try {
            val ollamaClient = OllamaClient("http://localhost:11434")
            ollamaClient.getModelOrNull(llama2Model.id)
            ollamaClient.getModelOrNull(glm47Model.id)
            ollamaClient.getModelOrNull(qwen3Model.id)

            val llama2Agent =
                AIAgent(
                    promptExecutor = SingleLLMPromptExecutor(ollamaClient),
                    llmModel = llama2Model,
                    systemPrompt = null,
                )

            val glm47Agent =
                AIAgent(
                    promptExecutor = SingleLLMPromptExecutor(ollamaClient),
                    llmModel = glm47Model,
                    systemPrompt = null,
                )

            val qwen3Agent =
                AIAgent(
                    promptExecutor = SingleLLMPromptExecutor(ollamaClient),
                    llmModel = qwen3Model,
                    systemPrompt = null,
                )

            // TODO
        } catch (e: Exception) {
            println("Error: ${e.message}")
            println("\nMake sure:")
            println("1. Ollama is running (try: ollama serve)")
            println("2. ${llama2Model.id} model is installed (try: ollama pull ${llama2Model.id})")
            println("3. ${glm47Model.id} model is installed (try: ollama pull ${glm47Model.id})")
            println("4. ${qwen3Model.id} model is installed (try: ollama pull ${qwen3Model.id})")
            e.printStackTrace()
        }
    }

package ai.advent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.OllamaModels
import kotlinx.coroutines.runBlocking

fun main() =
    runBlocking {
        // Define the Ollama model with JSON schema capability
        val ollamaModel = OllamaModels.Alibaba.QWEN_3_06B
        try {
            // Create AI agent with Koog
            val ollamaClient = OllamaClient("http://localhost:11434")
            ollamaClient.getModelOrNull(ollamaModel.id)
            val agent =
                AIAgent(
                    promptExecutor = SingleLLMPromptExecutor(ollamaClient),
                    llmModel = ollamaModel,
                    systemPrompt = null,
                )
        } catch (e: Exception) {
            println("Error: ${e.message}")
            println("\nMake sure:")
            println("1. Ollama is running (try: ollama serve)")
            println("2. ${ollamaModel.id} model is installed (try: ollama pull ${ollamaModel.id})")
            e.printStackTrace()
        }
    }

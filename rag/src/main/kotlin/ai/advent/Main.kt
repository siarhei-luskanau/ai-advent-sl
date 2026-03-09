package ai.advent

import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.rag.vector.EmbeddingBasedDocumentStorage
import ai.koog.rag.vector.InMemoryVectorStorage
import ai.koog.rag.vector.JVMTextDocumentEmbedder
import kotlinx.coroutines.runBlocking
import java.util.UUID

fun main() =
    runBlocking {
        // Create an embedder using Ollama
        val embedder = LLMEmbedder(OllamaClient(), OllamaModels.Embeddings.NOMIC_EMBED_TEXT)

        // Create a JVM-specific document embedder
        val documentEmbedder = JVMTextDocumentEmbedder(embedder)

        // Create a ranked document storage using in-memory vector storage
        val rankedDocumentStorage = EmbeddingBasedDocumentStorage(documentEmbedder, InMemoryVectorStorage())

        // https://ollama.com/library/deepseek-r1
        val ollamaModel =
            LLModel(
                provider = LLMProvider.Ollama,
                id = "deepseek-r1:1.5b",
                capabilities =
                    listOf(
                        LLMCapability.Temperature,
                        LLMCapability.Schema.JSON.Basic,
                        LLMCapability.Tools,
                    ),
                contextLength = 32_768,
            )

        try {
            val ollamaClient = OllamaClient("http://localhost:11434")
            val executor = SingleLLMPromptExecutor(ollamaClient)
            ollamaClient.getModelOrNull(ollamaModel.id, pullIfMissing = true)

            val prompt =
                prompt(id = UUID.randomUUID().toString()) {
                    user {
                        text(text = "hi")
                    }
                }
            val response = executor.execute(prompt = prompt, model = ollamaModel).single()

            println("response: $response")

            executor.close()
        } catch (e: Exception) {
            println("Error: ${e.message}")
            println("\nMake sure:")
            println("1. Ollama is running (try: ollama serve)")
            println("2. ${ollamaModel.id} model is installed (try: ollama pull ${ollamaModel.id})")
            e.printStackTrace()
        }
    }

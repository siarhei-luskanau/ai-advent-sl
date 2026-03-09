package ai.advent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.ext.agent.chatAgentStrategy
import ai.koog.embeddings.base.Vector
import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.rag.base.mostRelevantDocuments
import ai.koog.rag.vector.DocumentEmbedder
import ai.koog.rag.vector.EmbeddingBasedDocumentStorage
import ai.koog.rag.vector.InMemoryVectorStorage
import kotlinx.coroutines.runBlocking

data class TextChunk(
    val source: String,
    val content: String,
)

class TextChunkEmbedder(
    private val base: LLMEmbedder,
) : DocumentEmbedder<TextChunk> {
    override suspend fun embed(document: TextChunk): Vector = base.embed(document.content)

    override suspend fun embed(text: String): Vector = base.embed(text)

    override fun diff(
        embedding1: Vector,
        embedding2: Vector,
    ): Double = base.diff(embedding1, embedding2)
}

// Question: Today is March 7. Who has a next birthday and what he/she likes?
val knowledgeBase =
    listOf(
        TextChunk(
            source = "alice.md",
            content =
                "Alice was born on March 14. Her hobbies are hiking, photography, and cooking exotic cuisines. " +
                    "She loves outdoor adventures and is an avid trail runner.",
        ),
        TextChunk(
            source = "bob.md",
            content =
                "Bob's birthday is July 22. He is passionate about chess, reading science fiction novels, " +
                    "and building mechanical keyboards. Bob also enjoys cycling on weekends.",
        ),
        TextChunk(
            source = "carol.md",
            content =
                "Carol celebrates her birthday on November 5. Her hobbies include painting watercolors, " +
                    "playing the guitar, and attending live concerts. She is also into yoga and meditation.",
        ),
        TextChunk(
            source = "dave.md",
            content =
                "Dave was born on January 30. He loves woodworking, craft beer brewing, and watching football. " +
                    "Dave is also a big fan of board games and hosts game nights every month.",
        ),
        TextChunk(
            source = "eve.md",
            content =
                "Eve's birthday is September 18. She enjoys scuba diving, traveling to tropical destinations, " +
                    "and learning new languages. Eve also likes baking sourdough bread and gardening.",
        ),
    )

class DocumentSearchTools(
    private val documentStorage: EmbeddingBasedDocumentStorage<TextChunk>,
) : ToolSet {
    @Tool
    @LLMDescription("Search for relevant documents about any topic (if exists). Returns the content of the most relevant documents.")
    suspend fun searchDocuments(
        @LLMDescription("Query to search relevant documents about")
        query: String,
        @LLMDescription("Maximum number of documents")
        count: Int,
    ): String {
        val relevantDocuments = documentStorage.mostRelevantDocuments(query, count = count).toList()

        if (relevantDocuments.isEmpty()) {
            return "No relevant documents found for the query: $query"
        }

        val result = StringBuilder("Found ${relevantDocuments.size} relevant documents:\n\n")
        relevantDocuments.forEachIndexed { index, document ->
            result.append("Document ${index + 1}: ${document.source}\n")
            result.append("Content: ${document.content}\n\n")
        }
        return result.toString()
    }
}

fun main() =
    runBlocking {
        val chatClient = OllamaClient("http://localhost:11434")
        val chatModel = LLModel(
            provider = LLMProvider.Ollama,
            id = "gpt-oss:20b",
            capabilities =
                listOf(
                    LLMCapability.Completion,
                    LLMCapability.Schema.JSON.Standard,
                    LLMCapability.Speculation,
                    LLMCapability.Temperature,
                    LLMCapability.ToolChoice,
                    LLMCapability.Tools,
                ),
            contextLength = 128_000,
        )
        chatClient.getModelOrNull(chatModel.id, pullIfMissing = true)
        chatClient.getModelOrNull(OllamaModels.Embeddings.NOMIC_EMBED_TEXT.id, pullIfMissing = true)
        val llmEmbedder = LLMEmbedder(chatClient, OllamaModels.Embeddings.NOMIC_EMBED_TEXT)
        val chunkEmbedder = TextChunkEmbedder(llmEmbedder)
        val documentStorage = EmbeddingBasedDocumentStorage(embedder = chunkEmbedder, storage = InMemoryVectorStorage())

        println("Indexing knowledge base (${knowledgeBase.size} documents)...")
        for (chunk in knowledgeBase) {
            documentStorage.store(chunk, Unit)
            println("  [+] ${chunk.source}")
        }
        println("Knowledge base ready.\n")

        val tools =
            ToolRegistry {
                tools(DocumentSearchTools(documentStorage).asTools())
            }

        val agent =
            AIAgent(
                promptExecutor = MultiLLMPromptExecutor(chatClient),
                strategy = chatAgentStrategy(),
                agentConfig =
                    AIAgentConfig(
                        prompt =
                            prompt("rag-agent") {
                                system(
                                    "You are a helpful assistant. When asked questions, use the searchDocuments tool " +
                                        "to find relevant information from the knowledge base. " +
                                        "Answer based on the found documents. " +
                                        "If the tool returns no relevant documents, say you don't have that information.",
                                )
                            },
                        model = chatModel,
                        maxAgentIterations = 20,
                    ),
                toolRegistry = tools,
            )

        println("=== RAG Мини-чат ===")
        println("Задавайте вопросы (введите 'quit' для выхода)\n")

        while (true) {
            print("Вы: ")
            val userInput = readlnOrNull()?.trim() ?: break
            if (userInput.isBlank()) continue
            if (userInput.lowercase() == "quit") break

            val response = agent.run(userInput)
            println("\nАссистент: $response\n")
        }

        println("До свидания!")
    }

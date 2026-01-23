package ai.advent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
import ai.koog.agents.core.dsl.extension.compressHistory
import ai.koog.agents.core.dsl.extension.requestLLM
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking
import java.util.UUID

// ANSI colors for terminal output
object Colors {
    const val RESET = "\u001B[0m"
    const val GREEN = "\u001B[32m"
    const val BLUE = "\u001B[34m"
    const val CYAN = "\u001B[36m"
    const val YELLOW = "\u001B[33m"
    const val MAGENTA = "\u001B[35m"
    const val GRAY = "\u001B[90m"
    const val BOLD = "\u001B[1m"
}

// Session metadata tracking
data class SessionMetadata(
    val sessionId: String = UUID.randomUUID().toString().take(8),
    var messageCount: Int = 0,
    var totalInputTokens: Int = 0,
    var totalOutputTokens: Int = 0,
    var compressionCount: Int = 0,
) {
    fun display() {
        println("\n${Colors.MAGENTA}=== Session Metadata ===${Colors.RESET}")
        println("${Colors.GRAY}Session ID: $sessionId${Colors.RESET}")
        println("${Colors.GRAY}Messages exchanged: $messageCount${Colors.RESET}")
        println("${Colors.GRAY}Total input tokens: $totalInputTokens${Colors.RESET}")
        println("${Colors.GRAY}Total output tokens: $totalOutputTokens${Colors.RESET}")
        println("${Colors.GRAY}Compressions performed: $compressionCount${Colors.RESET}")
        println()
    }

    fun displaySummary() {
        println("\n${Colors.CYAN}=== Session Summary ===${Colors.RESET}")
        println("${Colors.GRAY}Session ID: $sessionId${Colors.RESET}")
        println("${Colors.GRAY}Messages exchanged: $messageCount${Colors.RESET}")
        println("${Colors.GRAY}Total input tokens: $totalInputTokens${Colors.RESET}")
        println("${Colors.GRAY}Total output tokens: $totalOutputTokens${Colors.RESET}")
        println("${Colors.GRAY}Compressions performed: $compressionCount${Colors.RESET}")
        println("${Colors.CYAN}Goodbye!${Colors.RESET}")
    }
}

fun main() =
    runBlocking {
        // TinyLlama model with 2K context - used for token counting experiments
        // https://ollama.com/library/tinyllama
        val model =
            LLModel(
                provider = LLMProvider.Ollama,
                id = "tinyllama:1.1b",
                capabilities =
                    listOf(
                        LLMCapability.Temperature,
                        LLMCapability.Schema.JSON.Basic,
                    ),
                contextLength = 2 * 1024, // 2048 tokens
            )

        println("${Colors.BOLD}${Colors.CYAN}=== Koog Chat with History Compression ===${Colors.RESET}")
        println("${Colors.GRAY}Model: ${model.id}${Colors.RESET}")
        println(
            "${Colors.GRAY}Commands: /compress - Compress history, " +
                "/history - Show metadata, /quit - Exit${Colors.RESET}",
        )
        println()

        try {
            val ollamaClient = OllamaClient("http://localhost:11434")
            val executor = SingleLLMPromptExecutor(ollamaClient)

            // Verify model is available
            val modelInfo = ollamaClient.getModelOrNull(model.id)
            if (modelInfo == null) {
                println(
                    "${Colors.YELLOW}Warning: Model ${model.id} not found. " +
                        "Try: ollama pull ${model.id}${Colors.RESET}",
                )
                return@runBlocking
            }

            // Session tracking
            val metadata = SessionMetadata()

            println("${Colors.MAGENTA}Session ID: ${metadata.sessionId}${Colors.RESET}")
            println("${Colors.GRAY}${"─".repeat(60)}${Colors.RESET}")

            executor.use { exec ->
                // Create functional agent for chat with compression capability
                // Using correct parameter order: promptExecutor, llmModel, strategy, systemPrompt
                val chatAgent =
                    AIAgent<String, String>(
                        promptExecutor = exec,
                        llmModel = model,
                        strategy =
                            functionalStrategy { initialInput ->
                                var userInput = initialInput

                                while (true) {
                                    when {
                                        userInput.equals("/quit", ignoreCase = true) ||
                                            userInput.equals("/exit", ignoreCase = true) ||
                                            userInput.equals("/bye", ignoreCase = true) -> {
                                            break
                                        }

                                        userInput.equals("/history", ignoreCase = true) -> {
                                            metadata.display()
                                        }

                                        userInput.equals("/compress", ignoreCase = true) -> {
                                            println(
                                                "\n${Colors.YELLOW}[Compression] " +
                                                    "Starting history compression...${Colors.RESET}",
                                            )
                                            println(
                                                "${Colors.YELLOW}[Compression] " +
                                                    "Strategy: WholeHistory${Colors.RESET}",
                                            )
                                            println(
                                                "${Colors.YELLOW}[Compression] " +
                                                    "Summarizing conversation into TLDR...${Colors.RESET}",
                                            )

                                            // Perform history compression using Koog's built-in API
                                            // This uses the writeSession and replaceHistoryWithTLDR internally
                                            compressHistory(
                                                strategy = HistoryCompressionStrategy.WholeHistory,
                                                preserveMemory = true,
                                            )

                                            metadata.compressionCount++
                                            println(
                                                "${Colors.YELLOW}[Compression] History compressed! " +
                                                    "(Compression #${metadata.compressionCount})${Colors.RESET}",
                                            )
                                            println(
                                                "${Colors.YELLOW}[Compression] " +
                                                    "History now contains summarized context${Colors.RESET}",
                                            )
                                            println()
                                        }

                                        userInput.isNotEmpty() -> {
                                            metadata.messageCount++
                                            println(
                                                "${Colors.GRAY}[Session] Sending message " +
                                                    "#${metadata.messageCount}...${Colors.RESET}",
                                            )

                                            // Make LLM request (allowToolCalls = false for simple chat)
                                            val response = requestLLM(userInput, allowToolCalls = false)

                                            // Track token usage from response metadata
                                            val inputTokens = response.metaInfo.inputTokensCount ?: 0
                                            val outputTokens = response.metaInfo.outputTokensCount ?: 0
                                            val totalTokens = response.metaInfo.totalTokensCount ?: 0

                                            metadata.totalInputTokens += inputTokens
                                            metadata.totalOutputTokens += outputTokens

                                            println(
                                                "${Colors.GRAY}[Metadata] " +
                                                    "Input: $inputTokens | " +
                                                    "Output: $outputTokens | " +
                                                    "Total: $totalTokens tokens${Colors.RESET}",
                                            )

                                            println()
                                            println(
                                                "${Colors.BLUE}${Colors.BOLD}Assistant:${Colors.RESET} " +
                                                    response.content,
                                            )
                                            println()

                                            // Suggest compression if history is growing
                                            if (metadata.messageCount > 0 && metadata.messageCount % 5 == 0) {
                                                println(
                                                    "${Colors.YELLOW}[Hint] History is growing. " +
                                                        "Use /compress to reduce context size.${Colors.RESET}",
                                                )
                                                println()
                                            }
                                        }
                                    }

                                    // Read next user input
                                    print("${Colors.GREEN}You: ${Colors.RESET}")
                                    userInput = readlnOrNull()?.trim() ?: break
                                }

                                // Return session summary
                                "Session ended after ${metadata.messageCount} messages"
                            },
                        systemPrompt =
                            """
                            You are a helpful and friendly AI assistant.
                            Keep responses concise but informative.
                            You maintain context from previous messages in the conversation.
                            """.trimIndent(),
                    )

                // Start the chat
                println("${Colors.CYAN}Chat started. Type your message:${Colors.RESET}")
                println()
                print("${Colors.GREEN}You: ${Colors.RESET}")
                val firstInput = readlnOrNull()?.trim() ?: return@use

                if (firstInput.isNotEmpty()) {
                    chatAgent.run(firstInput)
                    metadata.displaySummary()
                }
            }
        } catch (e: Exception) {
            println("${Colors.YELLOW}Error: ${e.message}${Colors.RESET}")
            println("\nMake sure:")
            println("1. Ollama is running (try: ollama serve)")
            println("2. Model is installed (try: ollama pull glm-4.7-flash:q4_K_M)")
            e.printStackTrace()
        }
    }

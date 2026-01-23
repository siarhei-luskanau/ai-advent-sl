package ai.advent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.replaceHistoryWithTLDR
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.time.Clock

// Color codes for console output
object Colors {
    const val RESET = "\u001B[0m"
    const val RED = "\u001B[31m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val BLUE = "\u001B[34m"
    const val PURPLE = "\u001B[35m"
    const val CYAN = "\u001B[36m"
    const val GRAY = "\u001B[90m"
    const val BOLD = "\u001B[1m"
}

// Session statistics for tracking metadata
data class SessionStats(
    var userMessageCount: Int = 0,
    var assistantMessageCount: Int = 0,
    var compressionCount: Int = 0,
    var totalInputTokens: Long = 0,
    var totalOutputTokens: Long = 0,
    var llmCallCount: Int = 0,
    var messagesBeforeCompression: Int = 0,
    var messagesAfterCompression: Int = 0,
    val compressionHistory: MutableList<CompressionEvent> = mutableListOf(),
)

data class CompressionEvent(
    val timestamp: String,
    val messagesBefore: Int,
    val messagesAfter: Int,
    val summary: String,
)

// Chat history for the session
data class ChatMessage(
    val role: String, // "user", "assistant", "system", "summary"
    val content: String,
    val timestamp: String = Clock.System.now().toString(),
)

// Storage keys for agent state
val messageCountKey = createStorageKey<Int>("messageCount")
val compressionCountKey = createStorageKey<Int>("compressionCount")

/**
 * Creates a chat agent strategy using Koog's writeSession and replaceHistoryWithTLDR for history compression.
 *
 * The strategy defines a simple graph:
 * - A chat node that processes user input, gets LLM response, and handles compression internally
 */
fun createChatStrategy(compressionThreshold: Int) =
    strategy("chat-with-compression") {
        // Node: Process chat message, get response, and compress if needed
        val chatWithCompressionNode by node<String, String>("chat-with-compression") { userMessage ->
            // Append user message and get LLM response
            val response =
                llm.writeSession {
                    appendPrompt { user(userMessage) }
                    requestLLMWithoutTools()
                }

            // Track message count after getting response (+2 for user + assistant)
            val currentCount = (storage.get(messageCountKey) ?: 0) + 2
            storage.set(messageCountKey, currentCount)

            // Check if compression is needed
            if (currentCount > compressionThreshold) {
                // Get current message count before compression
                val countBefore = llm.readSession { prompt.messages.size }

                // Use Koog's writeSession with replaceHistoryWithTLDR for history compression
                llm.writeSession {
                    replaceHistoryWithTLDR()
                }

                // Get message count after compression
                val countAfter = llm.readSession { prompt.messages.size }

                // Track compression stats
                val compressionNum = (storage.get(compressionCountKey) ?: 0) + 1
                storage.set(compressionCountKey, compressionNum)

                // Reset message count after compression
                storage.set(messageCountKey, 2)

                println()
                println("${Colors.YELLOW}${Colors.BOLD}[!] HISTORY COMPRESSED (using replaceHistoryWithTLDR)${Colors.RESET}")
                println("${Colors.YELLOW}    Messages: $countBefore -> $countAfter${Colors.RESET}")
                println("${Colors.YELLOW}    Compression #$compressionNum${Colors.RESET}")
                println()
            }

            response.content
        }

        // Define the graph edges: start -> chat -> finish
        edge(nodeStart forwardTo chatWithCompressionNode)
        edge(chatWithCompressionNode forwardTo nodeFinish)
    }

/**
 * Chat session that uses AIAgent with writeSession and replaceHistoryWithTLDR for compression.
 */
class ChatSession(
    private val executor: SingleLLMPromptExecutor,
    private val model: LLModel,
    private val compressionThreshold: Int = 8,
    private val systemPrompt: String,
) {
    val stats = SessionStats()
    val sessionId = UUID.randomUUID().toString().take(8)
    val startTime = Clock.System.now().toString()

    // Create the agent with our compression strategy
    private val agent =
        AIAgent(
            promptExecutor = executor,
            strategy = createChatStrategy(compressionThreshold),
            agentConfig =
                AIAgentConfig(
                    prompt =
                        prompt("chat-session") {
                            system(systemPrompt)
                        },
                    model = model,
                    maxAgentIterations = 50,
                ),
        )

    // Track conversation for display purposes
    private val conversationHistory = mutableListOf<ChatMessage>()

    init {
        conversationHistory.add(ChatMessage("system", systemPrompt))
    }

    suspend fun chat(userInput: String): String {
        // Track user message
        conversationHistory.add(ChatMessage("user", userInput))
        stats.userMessageCount++
        stats.llmCallCount++

        // Run the agent with user input
        val result = agent.run(userInput)

        // Extract response
        val assistantResponse = result

        // Track assistant response
        conversationHistory.add(ChatMessage("assistant", assistantResponse))
        stats.assistantMessageCount++

        // Update compression count from agent storage if it changed
        // Note: In a full implementation, we'd access agent storage here

        return assistantResponse
    }

    fun getHistoryCount(): Int = conversationHistory.size

    fun getMessageCount(): Int = conversationHistory.count { it.role != "system" }

    fun getCurrentHistory(): List<ChatMessage> = conversationHistory.toList()
}

fun main() =
    runBlocking {
        println("${Colors.CYAN}${Colors.BOLD}+============================================================+${Colors.RESET}")
        println("${Colors.CYAN}${Colors.BOLD}|     Koog Chat Application with History Compression         |${Colors.RESET}")
        println("${Colors.CYAN}${Colors.BOLD}+============================================================+${Colors.RESET}")
        println()

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

        println("${Colors.GRAY}Connecting to Ollama at http://localhost:11434...${Colors.RESET}")
        println("${Colors.GRAY}Using model: ${model.id}${Colors.RESET}")
        println()

        try {
            val ollamaClient = OllamaClient("http://localhost:11434")
            val executor = SingleLLMPromptExecutor(ollamaClient)

            // Verify model is available
            val modelInfo = ollamaClient.getModelOrNull(model.id)
            if (modelInfo == null) {
                println("${Colors.RED}Model ${model.id} not found. Please run: ollama pull ${model.id}${Colors.RESET}")
                return@runBlocking
            }

            println("${Colors.GREEN}[OK] Connected to Ollama successfully${Colors.RESET}")
            println()

            // Compression threshold - compress after this many messages
            val compressionThreshold = 6

            // Create chat session
            val session =
                ChatSession(
                    executor = executor,
                    model = model,
                    compressionThreshold = compressionThreshold,
                    systemPrompt =
                        """You are a helpful AI assistant in a console chat application.
                |Be concise and helpful in your responses.
                |You can discuss any topic and remember our conversation context.
                |When the conversation history gets long, it will be automatically
                |compressed into a summary to maintain context efficiently.
                        """.trimMargin(),
                )

            // Display session info
            printSessionInfo(session.sessionId, session.startTime, model.id)

            println("${Colors.YELLOW}Commands:${Colors.RESET}")
            println("  ${Colors.CYAN}/stats${Colors.RESET}    - Show detailed session statistics")
            println("  ${Colors.CYAN}/history${Colors.RESET}  - Show current message count and history summary")
            println("  ${Colors.CYAN}/memory${Colors.RESET}   - Show memory and compression metadata")
            println("  ${Colors.CYAN}/quit${Colors.RESET}     - Exit the chat")
            println()
            println("${Colors.GRAY}History compression triggers after $compressionThreshold conversation messages${Colors.RESET}")
            println()
            printDivider()

            // Chat loop
            while (true) {
                print("${Colors.GREEN}You: ${Colors.RESET}")
                val input = readlnOrNull()?.trim() ?: break

                when {
                    input.isEmpty() -> {
                        continue
                    }

                    input == "/quit" || input == "/exit" -> {
                        println("${Colors.CYAN}Goodbye!${Colors.RESET}")
                        break
                    }

                    input == "/stats" -> {
                        printStats(session.stats, session.sessionId, session.startTime)
                        continue
                    }

                    input == "/history" -> {
                        printHistoryInfo(session)
                        continue
                    }

                    input == "/memory" -> {
                        printMemoryMetadata(session)
                        continue
                    }

                    input.startsWith("/") -> {
                        println("${Colors.RED}Unknown command: $input${Colors.RESET}")
                        continue
                    }
                }

                try {
                    val response = session.chat(input)

                    println("${Colors.BLUE}Assistant: ${Colors.RESET}$response")
                    println()

                    // Show brief stats after response
                    printBriefStats(session.stats, session.getMessageCount())
                } catch (e: Exception) {
                    println("${Colors.RED}Error: ${e.message}${Colors.RESET}")
                    e.printStackTrace()
                }
            }

            // Final stats
            println()
            printDivider()
            printStats(session.stats, session.sessionId, session.startTime)

            executor.close()
        } catch (e: Exception) {
            println("${Colors.RED}Error: ${e.message}${Colors.RESET}")
            println()
            println("${Colors.YELLOW}Make sure:${Colors.RESET}")
            println("1. Ollama is running (try: ollama serve)")
            println("2. Model is installed (try: ollama pull llama3.2)")
            e.printStackTrace()
        }
    }

fun printDivider() {
    println("${Colors.GRAY}------------------------------------------------------------${Colors.RESET}")
}

fun printSessionInfo(
    sessionId: String,
    startTime: String,
    modelName: String,
) {
    println("${Colors.PURPLE}+-- Session Info -------------------------------------------+${Colors.RESET}")
    println("${Colors.PURPLE}|${Colors.RESET} Session ID: ${Colors.CYAN}$sessionId${Colors.RESET}")
    println("${Colors.PURPLE}|${Colors.RESET} Started:    ${Colors.GRAY}$startTime${Colors.RESET}")
    println("${Colors.PURPLE}|${Colors.RESET} Model:      ${Colors.YELLOW}$modelName${Colors.RESET}")
    println("${Colors.PURPLE}+-----------------------------------------------------------+${Colors.RESET}")
    println()
}

fun printHistoryInfo(session: ChatSession) {
    println()
    println("${Colors.PURPLE}${Colors.BOLD}+-- History Info -------------------------------------------+${Colors.RESET}")
    println("${Colors.PURPLE}|${Colors.RESET} Total entries:     ${session.getHistoryCount()}")
    println("${Colors.PURPLE}|${Colors.RESET} Message count:     ${session.getMessageCount()}")
    println("${Colors.PURPLE}|${Colors.RESET}")
    println("${Colors.PURPLE}|${Colors.RESET} ${Colors.BOLD}Current History:${Colors.RESET}")

    for ((index, msg) in session.getCurrentHistory().withIndex()) {
        val roleColor =
            when (msg.role) {
                "user" -> Colors.GREEN
                "assistant" -> Colors.BLUE
                "system" -> Colors.GRAY
                "summary" -> Colors.YELLOW
                else -> Colors.RESET
            }
        val preview = msg.content.take(50).replace("\n", " ") + if (msg.content.length > 50) "..." else ""
        println("${Colors.PURPLE}|${Colors.RESET}   ${index + 1}. $roleColor[${msg.role}]${Colors.RESET} $preview")
    }
    println("${Colors.PURPLE}+-----------------------------------------------------------+${Colors.RESET}")
    println()
}

fun printMemoryMetadata(session: ChatSession) {
    println()
    println("${Colors.CYAN}${Colors.BOLD}+-- Memory & Compression Metadata --------------------------+${Colors.RESET}")
    println("${Colors.CYAN}|${Colors.RESET}")
    println("${Colors.CYAN}|${Colors.RESET} ${Colors.BOLD}Session Memory${Colors.RESET}")
    println("${Colors.CYAN}|${Colors.RESET}   Session ID:         ${session.sessionId}")
    println("${Colors.CYAN}|${Colors.RESET}   Start time:         ${session.startTime}")
    println("${Colors.CYAN}|${Colors.RESET}   History entries:    ${session.getHistoryCount()}")
    println("${Colors.CYAN}|${Colors.RESET}   Active messages:    ${session.getMessageCount()}")
    println("${Colors.CYAN}|${Colors.RESET}")
    println("${Colors.CYAN}|${Colors.RESET} ${Colors.BOLD}Compression Statistics${Colors.RESET}")
    println("${Colors.CYAN}|${Colors.RESET}   Total compressions: ${session.stats.compressionCount}")

    if (session.stats.compressionHistory.isNotEmpty()) {
        println("${Colors.CYAN}|${Colors.RESET}")
        println("${Colors.CYAN}|${Colors.RESET} ${Colors.BOLD}Compression History${Colors.RESET}")
        for ((idx, event) in session.stats.compressionHistory.withIndex()) {
            println("${Colors.CYAN}|${Colors.RESET}   ${idx + 1}. ${event.messagesBefore} -> ${event.messagesAfter} msgs")
            println("${Colors.CYAN}|${Colors.RESET}      Time: ${event.timestamp}")
            println("${Colors.CYAN}|${Colors.RESET}      Summary: ${event.summary}")
        }
    }

    println("${Colors.CYAN}|${Colors.RESET}")
    println("${Colors.CYAN}|${Colors.RESET} ${Colors.BOLD}Token Memory${Colors.RESET}")
    println("${Colors.CYAN}|${Colors.RESET}   Total input tokens:  ${session.stats.totalInputTokens}")
    println("${Colors.CYAN}|${Colors.RESET}   Total output tokens: ${session.stats.totalOutputTokens}")
    println("${Colors.CYAN}|${Colors.RESET}   Estimated savings:   ~${estimateTokenSavings(session.stats)} tokens")
    println("${Colors.CYAN}+-----------------------------------------------------------+${Colors.RESET}")
    println()
}

fun estimateTokenSavings(stats: SessionStats): Long {
    // Rough estimate: each compression saves about 70% of the compressed messages' tokens
    // Assuming average 100 tokens per message pair
    val avgTokensPerMessage = 100
    var totalSaved = 0L
    for (event in stats.compressionHistory) {
        val messagesCompressed = event.messagesBefore - event.messagesAfter
        totalSaved += (messagesCompressed * avgTokensPerMessage * 0.7).toLong()
    }
    return totalSaved
}

fun printBriefStats(
    stats: SessionStats,
    currentMessageCount: Int,
) {
    val tokenInfo =
        if (stats.totalInputTokens > 0 || stats.totalOutputTokens > 0) {
            " | Tokens: ${stats.totalInputTokens}/${stats.totalOutputTokens}"
        } else {
            ""
        }

    val compressionInfo =
        if (stats.compressionCount > 0) {
            " | Compressions: ${stats.compressionCount}"
        } else {
            ""
        }

    println("${Colors.GRAY}[Messages: $currentMessageCount | LLM calls: ${stats.llmCallCount}$tokenInfo$compressionInfo]${Colors.RESET}")
}

fun printStats(
    stats: SessionStats,
    sessionId: String,
    startTime: String,
) {
    println()
    println("${Colors.CYAN}${Colors.BOLD}+===========================================================+${Colors.RESET}")
    println("${Colors.CYAN}${Colors.BOLD}|                    SESSION STATISTICS                     |${Colors.RESET}")
    println("${Colors.CYAN}${Colors.BOLD}+===========================================================+${Colors.RESET}")
    println("${Colors.CYAN}|${Colors.RESET} Session ID:          ${Colors.YELLOW}$sessionId${Colors.RESET}")
    println("${Colors.CYAN}|${Colors.RESET} Started at:          $startTime")
    println("${Colors.CYAN}|${Colors.RESET}")
    println("${Colors.CYAN}|${Colors.RESET} ${Colors.BOLD}Message Metrics${Colors.RESET}")
    println("${Colors.CYAN}|${Colors.RESET}   User messages:     ${stats.userMessageCount}")
    println("${Colors.CYAN}|${Colors.RESET}   Assistant msgs:    ${stats.assistantMessageCount}")
    println("${Colors.CYAN}|${Colors.RESET}   LLM calls:         ${stats.llmCallCount}")
    println("${Colors.CYAN}|${Colors.RESET}")
    println("${Colors.CYAN}|${Colors.RESET} ${Colors.BOLD}Token Usage${Colors.RESET}")
    println("${Colors.CYAN}|${Colors.RESET}   Input tokens:      ${stats.totalInputTokens}")
    println("${Colors.CYAN}|${Colors.RESET}   Output tokens:     ${stats.totalOutputTokens}")
    println("${Colors.CYAN}|${Colors.RESET}   Total tokens:      ${stats.totalInputTokens + stats.totalOutputTokens}")
    println("${Colors.CYAN}|${Colors.RESET}")
    println("${Colors.CYAN}|${Colors.RESET} ${Colors.BOLD}Compression Statistics${Colors.RESET}")
    println("${Colors.CYAN}|${Colors.RESET}   Total compressions: ${stats.compressionCount}")
    if (stats.compressionCount > 0) {
        println(
            "${Colors.CYAN}|${Colors.RESET}   Last compression:   ${stats.messagesBeforeCompression} -> ${stats.messagesAfterCompression} messages",
        )
        println("${Colors.CYAN}|${Colors.RESET}   Est. tokens saved:  ~${estimateTokenSavings(stats)}")
    }
    println("${Colors.CYAN}${Colors.BOLD}+===========================================================+${Colors.RESET}")
    println()
}

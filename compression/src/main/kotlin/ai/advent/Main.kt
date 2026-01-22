package ai.advent

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

class ChatSession(
    private val executor: SingleLLMPromptExecutor,
    private val model: LLModel,
    private val compressionThreshold: Int = 8,
    private val systemPrompt: String,
) {
    private val history = mutableListOf<ChatMessage>()
    val stats = SessionStats()
    val sessionId = UUID.randomUUID().toString().take(8)
    val startTime = Clock.System.now().toString()

    init {
        history.add(ChatMessage("system", systemPrompt))
    }

    suspend fun chat(userInput: String): String {
        // Add user message to history
        history.add(ChatMessage("user", userInput))
        stats.userMessageCount++

        // Check if compression is needed before sending
        val shouldCompress = history.count { it.role != "system" } > compressionThreshold
        if (shouldCompress) {
            compressHistory()
        }

        // Build prompt from history
        val chatPrompt =
            prompt(id = "chat-$sessionId-${stats.llmCallCount}") {
                for (msg in history) {
                    when (msg.role) {
                        "system" -> system(msg.content)
                        "user" -> user { text(msg.content) }
                        "assistant" -> assistant(msg.content)
                        "summary" -> system("[Conversation Summary]\n${msg.content}")
                    }
                }
            }

        // Execute LLM call
        stats.llmCallCount++
        val response = executor.execute(prompt = chatPrompt, model = model).single()

        // Track token usage
        stats.totalInputTokens += response.metaInfo.inputTokensCount ?: 0
        stats.totalOutputTokens += response.metaInfo.outputTokensCount ?: 0

        // Extract response content - response is LLMResponse which contains the text
        val assistantResponse = response.content

        // Add assistant response to history
        history.add(ChatMessage("assistant", assistantResponse))
        stats.assistantMessageCount++

        return assistantResponse
    }

    private suspend fun compressHistory() {
        val messagesBefore = history.count { it.role != "system" }
        stats.messagesBeforeCompression = messagesBefore

        println()
        println("${Colors.YELLOW}${Colors.BOLD}[!] COMPRESSING HISTORY${Colors.RESET}")
        println("${Colors.YELLOW}    Current messages: $messagesBefore${Colors.RESET}")
        println("${Colors.YELLOW}    Generating summary...${Colors.RESET}")

        // Build conversation text for summarization (exclude system prompt)
        val conversationText =
            history
                .filter { it.role != "system" && it.role != "summary" }
                .joinToString("\n") { "${it.role.uppercase()}: ${it.content}" }

        // Create summarization prompt
        val summaryPrompt =
            prompt(id = "compress-$sessionId-${stats.compressionCount}") {
                system(
                    """You are a conversation summarizer. Your task is to create a concise summary
                |of the conversation that preserves all important information, context, and any
                |decisions or agreements made. The summary should allow the conversation to
                |continue naturally.
                |
                |Format: Write a clear, structured summary in 2-4 sentences.
                    """.trimMargin(),
                )
                user {
                    text("Please summarize this conversation:\n\n$conversationText")
                }
            }

        // Execute summarization
        stats.llmCallCount++
        val summaryResponse = executor.execute(prompt = summaryPrompt, model = model).single()
        stats.totalInputTokens += summaryResponse.metaInfo.inputTokensCount ?: 0
        stats.totalOutputTokens += summaryResponse.metaInfo.outputTokensCount ?: 0

        val summary = summaryResponse.content

        // Keep system prompt and replace conversation with summary
        val systemMessage = history.first { it.role == "system" }
        history.clear()
        history.add(systemMessage)
        history.add(ChatMessage("summary", summary))

        val messagesAfter = history.count { it.role != "system" }
        stats.messagesAfterCompression = messagesAfter
        stats.compressionCount++

        // Record compression event
        stats.compressionHistory.add(
            CompressionEvent(
                timestamp = Clock.System.now().toString(),
                messagesBefore = messagesBefore,
                messagesAfter = messagesAfter,
                summary = summary.take(100) + if (summary.length > 100) "..." else "",
            ),
        )

        printCompressionResult(messagesBefore, messagesAfter, summary)
    }

    private fun printCompressionResult(
        before: Int,
        after: Int,
        summary: String,
    ) {
        val reduction = if (before > 0) ((before - after).toFloat() / before * 100).toInt() else 0
        println()
        println("${Colors.YELLOW}${Colors.BOLD}[*] COMPRESSION COMPLETE #${stats.compressionCount}${Colors.RESET}")
        println("${Colors.YELLOW}    Messages: $before -> $after (reduced by $reduction%)${Colors.RESET}")
        println("${Colors.YELLOW}    Summary: ${summary.take(80)}...${Colors.RESET}")
        println()
    }

    fun getHistoryCount(): Int = history.size

    fun getMessageCount(): Int = history.count { it.role != "system" }

    fun getCurrentHistory(): List<ChatMessage> = history.toList()
}

fun main() =
    runBlocking {
        println("${Colors.CYAN}${Colors.BOLD}+============================================================+${Colors.RESET}")
        println("${Colors.CYAN}${Colors.BOLD}|     Koog Chat Application with History Compression         |${Colors.RESET}")
        println("${Colors.CYAN}${Colors.BOLD}+============================================================+${Colors.RESET}")
        println()

        // https://ollama.com/library/glm-4.7-flash
        val model =
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

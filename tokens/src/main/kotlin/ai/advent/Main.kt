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

/**
 * Data class to hold token counting results and response info
 */
data class PromptResult(
    val promptName: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val responseText: String,
    val contextLimit: Long,
    val isOverflow: Boolean,
)

/**
 * Generates a long text by repeating a pattern to create content of approximate token count.
 * Rough estimate: ~4 characters per token for English text.
 */
fun generateLongText(targetTokens: Int): String {
    val baseText =
        """
        The quick brown fox jumps over the lazy dog. This is a sample sentence used to generate
        a large amount of text for testing context window limits. Machine learning models have
        limited context windows that determine how much text they can process at once. When we
        exceed this limit, interesting behaviors can occur. The model might truncate input,
        produce degraded outputs, or fail entirely. Understanding these limits is crucial for
        building robust AI applications. Let us explore what happens when we push these boundaries.
        """.trimIndent()

    val charsPerToken = 4
    val targetChars = targetTokens * charsPerToken
    val builder = StringBuilder()

    while (builder.length < targetChars) {
        builder.append(baseText).append(" ")
    }

    return builder.toString().take(targetChars)
}

/**
 * Executes a prompt and collects token metrics
 */
suspend fun executeAndMeasure(
    executor: SingleLLMPromptExecutor,
    model: LLModel,
    promptName: String,
    userMessage: String,
): PromptResult {
    val prompt =
        prompt(id = UUID.randomUUID().toString()) {
            system("You are a helpful assistant. Respond concisely.")
            user { text(userMessage) }
        }

    val response = executor.execute(prompt = prompt, model = model).single()

    val inputTokens = response.metaInfo.inputTokensCount ?: 0
    val outputTokens = response.metaInfo.outputTokensCount ?: 0
    val totalTokens = response.metaInfo.totalTokensCount ?: 0

    return PromptResult(
        promptName = promptName,
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        totalTokens = totalTokens,
        responseText = response.content,
        contextLimit = model.contextLength,
        // Overflow detected when input exceeds OR equals context limit (truncation)
        isOverflow = inputTokens >= model.contextLength,
    )
}

/**
 * Formats and prints the result of a prompt execution
 */
fun printResult(result: PromptResult) {
    println("\n${"=".repeat(60)}")
    println("PROMPT: ${result.promptName}")
    println("=".repeat(60))
    println("Context Limit: ${result.contextLimit.toInt()} tokens")
    println("Input Tokens:  ${result.inputTokens}")
    println("Output Tokens: ${result.outputTokens}")
    println("Total Tokens:  ${result.totalTokens}")
    val overflowStatus =
        when {
            result.inputTokens > result.contextLimit -> "YES - EXCEEDED & TRUNCATED!"
            result.inputTokens == result.contextLimit.toInt() -> "YES - AT LIMIT (likely truncated)"
            else -> "No"
        }
    println("Overflow:      $overflowStatus")
    println("-".repeat(60))
    println("Response (first 500 chars):")
    println(result.responseText.take(500))
    if (result.responseText.length > 500) println("...")
    println("=".repeat(60))
}

/**
 * Uses the analyzer model to compare and analyze the results
 */
suspend fun analyzeResults(
    executor: SingleLLMPromptExecutor,
    analyzerModel: LLModel,
    shortResult: PromptResult,
    mediumResult: PromptResult,
    overflowResult: PromptResult,
) {
    println("\n")
    println("#".repeat(60))
    println("ANALYSIS: Using ${analyzerModel.id} to analyze context overflow behavior")
    println("#".repeat(60))

    val analysisPrompt =
        """
        Analyze the following token counting experiment results and explain how the model behavior
        changes when context size overflows:

        EXPERIMENT 1 - Short Prompt (within limits):
        - Input tokens: ${shortResult.inputTokens} / ${shortResult.contextLimit.toInt()} limit
        - Output tokens: ${shortResult.outputTokens}
        - Response quality: "${shortResult.responseText.take(200)}"

        EXPERIMENT 2 - Medium Prompt (near limits):
        - Input tokens: ${mediumResult.inputTokens} / ${mediumResult.contextLimit.toInt()} limit
        - Output tokens: ${mediumResult.outputTokens}
        - Response quality: "${mediumResult.responseText.take(200)}"

        EXPERIMENT 3 - Overflow Prompt (exceeds limits):
        - Input tokens: ${overflowResult.inputTokens} / ${overflowResult.contextLimit.toInt()} limit
        - Output tokens: ${overflowResult.outputTokens}
        - Context overflow: ${overflowResult.isOverflow}
        - Response quality: "${overflowResult.responseText.take(200)}"

        Please analyze:
        1. How does token count affect response quality?
        2. What happens when context limit is exceeded?
        3. What are best practices to avoid context overflow issues?
        """.trimIndent()

    val prompt =
        prompt(id = UUID.randomUUID().toString()) {
            system("You are an AI expert analyzing LLM behavior patterns. Be concise but thorough.")
            user { text(analysisPrompt) }
        }

    println("\nAnalyzing with ${analyzerModel.id}...")
    val analysisResponse = executor.execute(prompt = prompt, model = analyzerModel).single()

    println("\n--- ANALYSIS RESULTS ---")
    println("Analysis Input Tokens:  ${analysisResponse.metaInfo.inputTokensCount ?: 0}")
    println("Analysis Output Tokens: ${analysisResponse.metaInfo.outputTokensCount ?: 0}")
    println("-".repeat(60))
    println(analysisResponse.content)
    println("#".repeat(60))
}

fun main() =
    runBlocking {
        // TinyLlama model with 2K context - used for token counting experiments
        // https://ollama.com/library/tinyllama
        val tinyLlamaModel =
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

        // Qwen model for analyzing the results
        val qwenModel = OllamaModels.Alibaba.QWEN_3_06B

        try {
            println("Token Counting & Context Overflow Analysis")
            println("==========================================")
            println("Token Counter Model: ${tinyLlamaModel.id} (context: ${tinyLlamaModel.contextLength} tokens)")
            println("Analyzer Model: ${qwenModel.id}")
            println()

            // Create Ollama client and executor
            val ollamaClient = OllamaClient("http://localhost:11434")
            val executor = SingleLLMPromptExecutor(ollamaClient)

            // Verify models are available
            println("Checking model availability...")
            ollamaClient.getModelOrNull(tinyLlamaModel.id)
                ?: throw IllegalStateException("Model ${tinyLlamaModel.id} not found")
            ollamaClient.getModelOrNull(qwenModel.id)
                ?: throw IllegalStateException("Model ${qwenModel.id} not found")
            println("Both models available!\n")

            // ================================================================
            // EXPERIMENT 1: Short prompt (well within context limit)
            // ================================================================
            println("Running Experiment 1: Short prompt...")
            val shortPrompt = "What is 2 + 2? Answer briefly."
            val shortResult =
                executeAndMeasure(
                    executor = executor,
                    model = tinyLlamaModel,
                    promptName = "Short Prompt (~50 tokens)",
                    userMessage = shortPrompt,
                )
            printResult(shortResult)

            // ================================================================
            // EXPERIMENT 2: Medium prompt (approaching context limit)
            // ================================================================
            println("\nRunning Experiment 2: Medium prompt...")
            val mediumText = generateLongText(targetTokens = 1000) // ~1000 tokens
            val mediumPrompt =
                """
                Please summarize the key points from the following text in 2-3 sentences:

                $mediumText
                """.trimIndent()
            val mediumResult =
                executeAndMeasure(
                    executor = executor,
                    model = tinyLlamaModel,
                    promptName = "Medium Prompt (~1000 tokens)",
                    userMessage = mediumPrompt,
                )
            printResult(mediumResult)

            // ================================================================
            // EXPERIMENT 3: Overflow prompt (exceeds 2K context limit)
            // ================================================================
            println("\nRunning Experiment 3: Overflow prompt (exceeding ${tinyLlamaModel.contextLength} token limit)...")
            val overflowText = generateLongText(targetTokens = 3000) // ~3000 tokens - exceeds 2K limit
            val overflowPrompt =
                """
                Please analyze and summarize the following extensive text. Identify the main themes
                and provide a comprehensive summary:

                $overflowText
                """.trimIndent()

            val overflowResult =
                try {
                    executeAndMeasure(
                        executor = executor,
                        model = tinyLlamaModel,
                        promptName = "Overflow Prompt (~3000 tokens, exceeds 2K limit)",
                        userMessage = overflowPrompt,
                    )
                } catch (e: Exception) {
                    println("\n*** OVERFLOW ERROR CAUGHT ***")
                    println("Error type: ${e.javaClass.simpleName}")
                    println("Error message: ${e.message}")

                    // Create a result to capture the overflow scenario
                    PromptResult(
                        promptName = "Overflow Prompt (~3000 tokens, exceeds 2K limit)",
                        inputTokens = 3000, // Estimated
                        outputTokens = 0,
                        totalTokens = 3000,
                        responseText = "ERROR: ${e.message}",
                        contextLimit = tinyLlamaModel.contextLength,
                        isOverflow = true,
                    )
                }
            printResult(overflowResult)

            // ================================================================
            // ANALYSIS: Use Qwen to analyze the behavior differences
            // ================================================================
            analyzeResults(
                executor = executor,
                analyzerModel = qwenModel,
                shortResult = shortResult,
                mediumResult = mediumResult,
                overflowResult = overflowResult,
            )

            // Summary table
            println("\n")
            println("SUMMARY TABLE")
            println("=".repeat(80))
            println(
                "%-35s | %10s | %10s | %10s | %8s".format(
                    "Prompt",
                    "Input",
                    "Output",
                    "Total",
                    "Overflow",
                ),
            )
            println("-".repeat(80))
            listOf(shortResult, mediumResult, overflowResult).forEach { r ->
                println(
                    "%-35s | %10d | %10d | %10d | %8s".format(
                        r.promptName.take(35),
                        r.inputTokens,
                        r.outputTokens,
                        r.totalTokens,
                        if (r.isOverflow) "YES" else "No",
                    ),
                )
            }
            println("=".repeat(80))

            executor.close()
            println("\nExperiment completed successfully!")
        } catch (e: Exception) {
            println("\nError: ${e.message}")
            println("\nMake sure:")
            println("1. Ollama is running (try: ollama serve)")
            println("2. ${tinyLlamaModel.id} model is installed (try: ollama pull ${tinyLlamaModel.id})")
            println("3. ${qwenModel.id} model is installed (try: ollama pull ${qwenModel.id})")
            e.printStackTrace()
        }
    }

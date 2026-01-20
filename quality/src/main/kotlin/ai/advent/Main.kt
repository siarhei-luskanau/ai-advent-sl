package ai.advent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.OllamaModels
import kotlinx.coroutines.runBlocking
import kotlin.time.measureTimedValue

data class ModelResponse(
    val modelId: String,
    val response: String,
    val durationMs: Long,
    val estimatedTokens: Int,
)

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
            val executor = simpleOllamaAIExecutor(baseUrl = "http://localhost:11434")

            val llama2Agent =
                AIAgent(
                    promptExecutor = executor,
                    llmModel = llama2Model,
                )

            val glm47Agent =
                AIAgent(
                    promptExecutor = executor,
                    llmModel = glm47Model,
                )

            val qwen3Agent =
                AIAgent(
                    promptExecutor = executor,
                    llmModel = qwen3Model,
                )

            val query = "Объясни ход решения: 2 плюс 2 умножить на 2"

            println("=".repeat(80))
            println("СРАВНЕНИЕ КАЧЕСТВА ОТВЕТОВ МОДЕЛЕЙ")
            println("=".repeat(80))
            println("\nЗапрос: $query\n")

            // Execute query on llama2:13b
            println("-".repeat(80))
            println("Модель: ${llama2Model.id}")
            println("-".repeat(80))

            val llama2Result =
                measureTimedValue {
                    llama2Agent.run(query)
                }
            val llama2Response =
                ModelResponse(
                    modelId = llama2Model.id,
                    response = llama2Result.value,
                    durationMs = llama2Result.duration.inWholeMilliseconds,
                    estimatedTokens = estimateTokens(query) + estimateTokens(llama2Result.value),
                )

            println("Ответ:\n${llama2Response.response}")
            println("\nМетрики:")
            println("  Время ответа: ${llama2Response.durationMs} мс")
            println("  Токенов (оценка): ${llama2Response.estimatedTokens}")

            // Execute query on glm-4.7-flash
            println("\n" + "-".repeat(80))
            println("Модель: ${glm47Model.id}")
            println("-".repeat(80))

            val glm47Result =
                measureTimedValue {
                    glm47Agent.run(query)
                }
            val glm47Response =
                ModelResponse(
                    modelId = glm47Model.id,
                    response = glm47Result.value,
                    durationMs = glm47Result.duration.inWholeMilliseconds,
                    estimatedTokens = estimateTokens(query) + estimateTokens(glm47Result.value),
                )

            println("Ответ:\n${glm47Response.response}")
            println("\nМетрики:")
            println("  Время ответа: ${glm47Response.durationMs} мс")
            println("  Токенов (оценка): ${glm47Response.estimatedTokens}")

            // Compare quality using Qwen3 as judge
            println("\n" + "=".repeat(80))
            println("ОЦЕНКА КАЧЕСТВА (судья: ${qwen3Model.id})")
            println("=".repeat(80))

            val comparisonPrompt =
                buildComparisonPrompt(
                    query = query,
                    response1 = llama2Response,
                    response2 = glm47Response,
                )

            val qualityResult =
                measureTimedValue {
                    qwen3Agent.run(comparisonPrompt)
                }

            println("\nАнализ качества ответов:\n")
            println(qualityResult.value)

            // Summary table
            println("\n" + "=".repeat(80))
            println("ИТОГОВАЯ ТАБЛИЦА")
            println("=".repeat(80))
            println(
                String.format(
                    "%-25s | %-15s | %-15s | %-10s",
                    "Модель",
                    "Время (мс)",
                    "Токены",
                    "Скорость",
                ),
            )
            println("-".repeat(80))

            val llama2CompletionTokens = estimateTokens(llama2Response.response)
            val glm47CompletionTokens = estimateTokens(glm47Response.response)

            val llama2Speed =
                if (llama2Response.durationMs > 0) {
                    llama2CompletionTokens * 1000.0 / llama2Response.durationMs
                } else {
                    0.0
                }
            val glm47Speed =
                if (glm47Response.durationMs > 0) {
                    glm47CompletionTokens * 1000.0 / glm47Response.durationMs
                } else {
                    0.0
                }

            println(
                String.format(
                    "%-25s | %-15d | %-15d | %.1f tok/s",
                    llama2Response.modelId,
                    llama2Response.durationMs,
                    llama2Response.estimatedTokens,
                    llama2Speed,
                ),
            )
            println(
                String.format(
                    "%-25s | %-15d | %-15d | %.1f tok/s",
                    glm47Response.modelId,
                    glm47Response.durationMs,
                    glm47Response.estimatedTokens,
                    glm47Speed,
                ),
            )
            println("=".repeat(80))

            executor.close()
        } catch (e: Exception) {
            println("Ошибка: ${e.message}")
            println("\nУбедитесь что:")
            println("1. Ollama запущен (попробуйте: ollama serve)")
            println("2. Модель ${llama2Model.id} установлена (попробуйте: ollama pull ${llama2Model.id})")
            println("3. Модель ${glm47Model.id} установлена (попробуйте: ollama pull ${glm47Model.id})")
            println("4. Модель ${qwen3Model.id} установлена (попробуйте: ollama pull ${qwen3Model.id})")
            e.printStackTrace()
        }
    }

private fun estimateTokens(text: String): Int {
    // Rough estimation: ~3.5 characters per token for Russian text
    return (text.length / 3.5).toInt().coerceAtLeast(1)
}

private fun buildComparisonPrompt(
    query: String,
    response1: ModelResponse,
    response2: ModelResponse,
): String =
    """
        |Ты - эксперт по оценке качества ответов языковых моделей.
        |
        |Исходный вопрос: "$query"
        |
        |Ответ модели ${response1.modelId}:
        |---
        |${response1.response}
        |---
        |
        |Ответ модели ${response2.modelId}:
        |---
        |${response2.response}
        |---
        |
        |Проанализируй оба ответа по следующим критериям:
        |1. Корректность математического решения (правильный ли ответ и порядок операций)
        |2. Понятность объяснения
        |3. Полнота ответа
        |4. Ясность изложения
        |
        |Выдай структурированную оценку каждого ответа и определи победителя.
        |Ответ дай на русском языке.
    """.trimMargin()

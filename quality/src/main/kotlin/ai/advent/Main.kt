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
import kotlin.time.measureTimedValue

data class ModelResponse(
    val modelId: String,
    val response: String,
    val durationMs: Long,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
)

fun main() =
    runBlocking {
        val baseUrl = "http://localhost:11434"

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
            val llmClient = OllamaClient(baseUrl = baseUrl)
            val promptExecutor = SingleLLMPromptExecutor(llmClient)

            val query = "Объясни ход решения: 2 плюс 2 умножить на 2"

            println("=".repeat(80))
            println("СРАВНЕНИЕ КАЧЕСТВА ОТВЕТОВ МОДЕЛЕЙ")
            println("=".repeat(80))
            println("\nЗапрос: $query\n")

            // Execute query on llama2:13b
            println("-".repeat(80))
            println("Модель: ${llama2Model.id}")
            println("-".repeat(80))

            val llama2Prompt =
                prompt(id = UUID.randomUUID().toString()) {
                    user {
                        text(text = query)
                    }
                }

            val llama2Result =
                measureTimedValue {
                    promptExecutor.execute(prompt = llama2Prompt, model = llama2Model).single()
                }
            val llama2Response =
                ModelResponse(
                    modelId = llama2Model.id,
                    response = llama2Result.value.content,
                    durationMs = llama2Result.duration.inWholeMilliseconds,
                    inputTokens = llama2Result.value.metaInfo.inputTokensCount ?: 0,
                    outputTokens = llama2Result.value.metaInfo.outputTokensCount ?: 0,
                    totalTokens = llama2Result.value.metaInfo.totalTokensCount ?: 0,
                )

            println("Ответ:\n${llama2Response.response}")
            println("\nМетрики:")
            println("  Время ответа: ${llama2Response.durationMs} мс")
            println("  Входных токенов: ${llama2Response.inputTokens}")
            println("  Выходных токенов: ${llama2Response.outputTokens}")
            println("  Всего токенов: ${llama2Response.totalTokens}")

            // Execute query on glm-4.7-flash
            println("\n" + "-".repeat(80))
            println("Модель: ${glm47Model.id}")
            println("-".repeat(80))

            val glm47Prompt =
                prompt(id = UUID.randomUUID().toString()) {
                    user {
                        text(text = query)
                    }
                }

            val glm47Result =
                measureTimedValue {
                    promptExecutor.execute(prompt = glm47Prompt, model = glm47Model).single()
                }
            val glm47Response =
                ModelResponse(
                    modelId = glm47Model.id,
                    response = glm47Result.value.content,
                    durationMs = glm47Result.duration.inWholeMilliseconds,
                    inputTokens = glm47Result.value.metaInfo.inputTokensCount ?: 0,
                    outputTokens = glm47Result.value.metaInfo.outputTokensCount ?: 0,
                    totalTokens = glm47Result.value.metaInfo.totalTokensCount ?: 0,
                )

            println("Ответ:\n${glm47Response.response}")
            println("\nМетрики:")
            println("  Время ответа: ${glm47Response.durationMs} мс")
            println("  Входных токенов: ${glm47Response.inputTokens}")
            println("  Выходных токенов: ${glm47Response.outputTokens}")
            println("  Всего токенов: ${glm47Response.totalTokens}")

            // Compare quality using Qwen3 as judge
            println("\n" + "=".repeat(80))
            println("ОЦЕНКА КАЧЕСТВА (судья: ${qwen3Model.id})")
            println("=".repeat(80))

            val comparisonPromptText =
                buildComparisonPrompt(
                    query = query,
                    response1 = llama2Response,
                    response2 = glm47Response,
                )

            val qwen3Prompt =
                prompt(id = UUID.randomUUID().toString()) {
                    user {
                        text(text = comparisonPromptText)
                    }
                }

            val qualityResult =
                measureTimedValue {
                    promptExecutor.execute(prompt = qwen3Prompt, model = qwen3Model).single()
                }

            println("\nАнализ качества ответов:\n")
            println(qualityResult.value.content)

            // Summary table
            println("\n" + "=".repeat(80))
            println("ИТОГОВАЯ ТАБЛИЦА")
            println("=".repeat(80))
            println(
                String.format(
                    "%-25s | %-15s | %-15s | %-15s | %-10s",
                    "Модель",
                    "Время (мс)",
                    "Вход. токены",
                    "Выход. токены",
                    "Скорость",
                ),
            )
            println("-".repeat(80))

            val llama2Speed =
                if (llama2Response.durationMs > 0) {
                    llama2Response.outputTokens * 1000.0 / llama2Response.durationMs
                } else {
                    0.0
                }
            val glm47Speed =
                if (glm47Response.durationMs > 0) {
                    glm47Response.outputTokens * 1000.0 / glm47Response.durationMs
                } else {
                    0.0
                }

            println(
                String.format(
                    "%-25s | %-15d | %-15d | %-15d | %.1f tok/s",
                    llama2Response.modelId,
                    llama2Response.durationMs,
                    llama2Response.inputTokens,
                    llama2Response.outputTokens,
                    llama2Speed,
                ),
            )
            println(
                String.format(
                    "%-25s | %-15d | %-15d | %-15d | %.1f tok/s",
                    glm47Response.modelId,
                    glm47Response.durationMs,
                    glm47Response.inputTokens,
                    glm47Response.outputTokens,
                    glm47Speed,
                ),
            )
            println("=".repeat(80))

            llmClient.close()
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

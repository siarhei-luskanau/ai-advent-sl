package ai.advent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.OllamaModels
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

data class TemperatureResult(
    val temperature: Double,
    val response: String,
)

fun main() =
    runBlocking {
        val ollamaModel = OllamaModels.Alibaba.QWEN_3_06B
        val temperatures = listOf(0.0, 0.7, 1.2)
        val query = "Объясни как пользоваться Claude Code наиболее результативно. Ответ должен быть не более 200 слов."

        println("=".repeat(80))
        println("Temperature Comparison Experiment")
        println("Model: ${ollamaModel.id}")
        println("Query: $query")
        println("=".repeat(80))

        try {
            val ollamaClient = OllamaClient("http://localhost:11434")
            ollamaClient.getModelOrNull(ollamaModel.id)
                ?: throw Exception("Model ${ollamaModel.id} not found")

            val executor = SingleLLMPromptExecutor(ollamaClient)

            // Run 3 agents with different temperatures in parallel
            val results =
                temperatures
                    .map { temp ->
                        async {
                            println("\n[Starting agent with temperature=$temp]")
                            val agent =
                                AIAgent(
                                    promptExecutor = executor,
                                    llmModel = ollamaModel,
                                    systemPrompt = "Ты полезный помощник. Отвечай на русском языке кратко и по существу.",
                                    temperature = temp,
                                )
                            val response = agent.run(query)
                            println("[Completed agent with temperature=$temp]")
                            TemperatureResult(temp, response)
                        }
                    }.awaitAll()

            // Display results from each temperature setting
            println("\n" + "=".repeat(80))
            println("RESULTS")
            println("=".repeat(80))

            results.forEach { result ->
                println("\n--- Temperature: ${result.temperature} ---")
                println(result.response)
                println("-".repeat(40))
            }

            // Run comparison agent
            println("\n" + "=".repeat(80))
            println("ANALYSIS BY COMPARISON AGENT")
            println("=".repeat(80))

            val comparisonPrompt = buildComparisonPrompt(results)

            val comparisonAgent =
                AIAgent(
                    promptExecutor = executor,
                    llmModel = ollamaModel,
                    systemPrompt =
                        """
                        Ты эксперт по анализу текстов и оценке качества ответов языковых моделей.
                        Проводи объективный анализ и давай конкретные рекомендации.
                        Отвечай на русском языке.
                        """.trimIndent(),
                    temperature = 0.3,
                )

            val analysisResult = comparisonAgent.run(comparisonPrompt)

            println("\n$analysisResult")
            println("\n" + "=".repeat(80))
        } catch (e: Exception) {
            println("Error: ${e.message}")
            println("\nMake sure:")
            println("1. Ollama is running (try: ollama serve)")
            println("2. ${ollamaModel.id} model is installed (try: ollama pull ${ollamaModel.id})")
            e.printStackTrace()
        }
    }

private fun buildComparisonPrompt(results: List<TemperatureResult>): String {
    val responsesText =
        results.joinToString("\n\n") { result ->
            """
            === Температура: ${result.temperature} ===
            ${result.response}
            """.trimIndent()
        }

    return """
        Проанализируй три ответа на один и тот же вопрос, сгенерированные с разными настройками температуры.

        $responsesText

        Проведи сравнительный анализ по следующим критериям:
        1. ТОЧНОСТЬ: Насколько ответ соответствует вопросу и содержит корректную информацию
        2. КРЕАТИВНОСТЬ: Насколько оригинально и интересно подан материал
        3. РАЗНООБРАЗИЕ: Насколько богат словарный запас и разнообразны конструкции
        4. СВЯЗНОСТЬ: Насколько логично и последовательно изложен ответ

        Затем сформулируй РЕКОМЕНДАЦИИ: для каких типов задач лучше подходит каждая настройка температуры:
        - Температура 0 (детерминированный режим)
        - Температура 0.7 (сбалансированный режим)
        - Температура 1.2 (креативный режим)

        Ответ должен быть структурированным и содержательным.
        """.trimIndent()
}

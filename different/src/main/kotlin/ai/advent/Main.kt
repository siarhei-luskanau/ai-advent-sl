package ai.advent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.OllamaModels
import kotlinx.coroutines.runBlocking

fun main() =
    runBlocking {
        val ollamaModel = OllamaModels.Alibaba.QWEN_3_06B
        val projectDescription = "AI chat product for different platforms"

        try {
            println("=".repeat(80))
            println("Expert Consultation for: $projectDescription")
            println("=".repeat(80))
            println()

            val ollamaClient = OllamaClient("http://localhost:11434")
            ollamaClient.getModelOrNull(ollamaModel.id)
                ?: throw IllegalStateException("Model ${ollamaModel.id} not found")

            val promptExecutor = SingleLLMPromptExecutor(ollamaClient)

            // Step 1: Get list of experts
            println("Step 1: Identifying relevant experts...")
            println("-".repeat(80))

            val expertsAgent =
                AIAgent(
                    promptExecutor = promptExecutor,
                    llmModel = ollamaModel,
                    systemPrompt =
                        """You are a helpful assistant that identifies domain experts.
                        |Provide a concise list of exactly 5 different expert roles that would be valuable
                        |for planning a software product. Format: one expert role per line, numbered 1-5.
                        |Only output the numbered list, nothing else.
                        """.trimMargin(),
                )

            val expertsResponse =
                expertsAgent.run(
                    "List 5 different expert roles needed to plan and estimate '$projectDescription'. " +
                        "Include technical, business, and user experience perspectives.",
                )

            println("Identified Experts:")
            println(expertsResponse)
            println()

            // Parse experts from response
            val experts = parseExperts(expertsResponse)
            println("Parsed ${experts.size} experts: $experts")
            println()

            // Step 2: Get plan and estimation from each expert
            println("Step 2: Gathering expert opinions...")
            println("-".repeat(80))

            val expertResponses = mutableMapOf<String, String>()

            for ((index, expert) in experts.withIndex()) {
                println()
                println("Consulting Expert ${index + 1}: $expert")
                println("-".repeat(40))

                val expertAgent =
                    AIAgent(
                        promptExecutor = promptExecutor,
                        llmModel = ollamaModel,
                        systemPrompt =
                            """You are a $expert with deep expertise in your field.
                            |Provide practical, actionable advice from your professional perspective.
                            |Be concise but thorough. Focus on your area of expertise.
                            """.trimMargin(),
                    )

                val expertPlan =
                    expertAgent.run(
                        """As a $expert, provide your professional plan and estimation for building:
                        |'$projectDescription'
                        |
                        |Include:
                        |1. Key considerations from your expertise area
                        |2. Main tasks/phases you recommend
                        |3. Potential risks and challenges
                        |4. Resource requirements
                        |5. Timeline estimation (in weeks/months)
                        |
                        |Be specific and practical.
                        """.trimMargin(),
                    )

                expertResponses[expert] = expertPlan
                println(expertPlan)
            }

            // Step 3: Compare expert responses
            println()
            println("=".repeat(80))
            println("Step 3: Comparing Expert Responses")
            println("=".repeat(80))

            val comparisonAgent =
                AIAgent(
                    promptExecutor = promptExecutor,
                    llmModel = ollamaModel,
                    systemPrompt =
                        """You are a skilled analyst who synthesizes multiple expert opinions.
                        |Identify common themes, differences, and create actionable insights.
                        """.trimMargin(),
                )

            val allExpertInputs =
                expertResponses.entries.joinToString("\n\n") { (expert, response) ->
                    "=== $expert ===\n$response"
                }

            val comparison =
                comparisonAgent.run(
                    """Analyze and compare these expert opinions for '$projectDescription':
                    |
                    |$allExpertInputs
                    |
                    |Provide:
                    |1. Common themes across experts
                    |2. Key differences in perspectives
                    |3. Areas of consensus
                    |4. Conflicting recommendations
                    """.trimMargin(),
                )

            println(comparison)

            // Step 4: Create final consolidated plan
            println()
            println("=".repeat(80))
            println("Step 4: Final Consolidated Plan and Estimation")
            println("=".repeat(80))

            val finalPlanAgent =
                AIAgent(
                    promptExecutor = promptExecutor,
                    llmModel = ollamaModel,
                    systemPrompt =
                        """You are a senior project manager and technical architect.
                        |Create comprehensive, realistic project plans based on expert input.
                        |Be practical and actionable.
                        """.trimMargin(),
                )

            val finalPlan =
                finalPlanAgent.run(
                    """Based on all expert opinions below, create a FINAL consolidated plan for:
                    |'$projectDescription'
                    |
                    |Expert Opinions:
                    |$allExpertInputs
                    |
                    |Create a comprehensive plan including:
                    |1. Executive Summary
                    |2. Project Phases with milestones
                    |3. Team composition and roles needed
                    |4. Technology stack recommendations
                    |5. Risk mitigation strategies
                    |6. Budget considerations
                    |7. Timeline with phases (provide realistic estimates)
                    |8. Success metrics and KPIs
                    |
                    |Make this actionable and ready for stakeholder presentation.
                    """.trimMargin(),
                )

            println(finalPlan)

            println()
            println("=".repeat(80))
            println("Consultation Complete!")
            println("=".repeat(80))
        } catch (e: Exception) {
            println("Error: ${e.message}")
            println("\nMake sure:")
            println("1. Ollama is running (try: ollama serve)")
            println("2. ${ollamaModel.id} model is installed (try: ollama pull ${ollamaModel.id})")
            e.printStackTrace()
        }
    }

fun parseExperts(response: String): List<String> {
    // Parse numbered list of experts from response
    val lines = response.lines()
    val experts = mutableListOf<String>()

    for (line in lines) {
        val trimmed = line.trim()
        // Match patterns like "1. Expert Name" or "1) Expert Name" or just "Expert Name"
        val match = Regex("""^\d+[.)]\s*(.+)$""").find(trimmed)
        if (match != null) {
            experts.add(match.groupValues[1].trim())
        } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && experts.size < 5) {
            // Fallback: add non-empty lines that aren't headers
            val cleanedLine = trimmed.removePrefix("-").removePrefix("*").trim()
            if (cleanedLine.isNotEmpty() && cleanedLine.length > 3) {
                experts.add(cleanedLine)
            }
        }
    }

    return experts
}

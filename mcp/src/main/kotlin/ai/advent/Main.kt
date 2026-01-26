package ai.advent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.mcp.defaultStdioTransport
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking

fun main() =
    runBlocking {
        println("=".repeat(70))
        println("MCP Server Tool Discovery Application")
        println("Using Koog library with local Ollama client")
        println("=".repeat(70))
        println()

        // Configure Ollama model
        val ollamaModel =
            LLModel(
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

        // Initialize Ollama client
        val ollamaClient = OllamaClient("http://localhost:11434")
        ollamaClient.getModelOrNull(ollamaModel.id)

        println()
        println("=".repeat(70))
        println("Discovering MCP Server Tools")
        println("=".repeat(70))
        println()

        val userDir = System.getProperty("user.dir")
        val mcpCommand = listOf("npx", "-y", "@modelcontextprotocol/server-filesystem", userDir)
        println("-".repeat(70))
        println("MCP Server: ${mcpCommand[2]}")
        println("Command: ${mcpCommand.joinToString(" ")}")
        println()

        try {
            // Start the MCP server process
            val process = ProcessBuilder(mcpCommand).start()

            // Create stdio transport and tool registry
            val transport = McpToolRegistryProvider.defaultStdioTransport(process)
            val toolRegistry = McpToolRegistryProvider.fromTransport(transport = transport)

            toolRegistry.tools.forEach { tool ->
                // Get parameter info from descriptor
                val descriptor = tool.descriptor
                println("  Tool: ${tool.name}")
                println("     Description: ${descriptor.description}")
                val requiredParams = descriptor.requiredParameters
                val optionalParams = descriptor.optionalParameters

                if (requiredParams.isNotEmpty() || optionalParams.isNotEmpty()) {
                    println("     Parameters:")
                    for (param in requiredParams) {
                        println("       - ${param.name}: ${param.type} (required)")
                        if (param.description.isNotBlank()) {
                            println("         ${param.description}")
                        }
                    }
                    for (param in optionalParams) {
                        println("       - ${param.name}: ${param.type} (optional)")
                        if (param.description.isNotBlank()) {
                            println("         ${param.description}")
                        }
                    }
                }
                println()
            }

            // Create the runner
            val agent =
                AIAgent(
                    promptExecutor = SingleLLMPromptExecutor(ollamaClient),
                    llmModel = ollamaModel,
                    toolRegistry = toolRegistry,
                )
            val request = "Count number of files and folders in $userDir . Exclude build folder. "
            println(request)
            val output = agent.run(request + "You can only call tools. Get it by calling tools.")
            println(output)
            // Close the transport and process
            transport.close()
            process.destroyForcibly()
        } catch (e: Exception) {
            println("  Error connecting to ${mcpCommand[2]}: ${e.message}")
            println("    Make sure the MCP server package is available via npm")
            println()
        }

        println("=".repeat(70))
        println("Tool Discovery Complete")
        println("=".repeat(70))
    }

package ai.advent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.rag.base.mostRelevantDocuments
import ai.koog.rag.vector.EmbeddingBasedDocumentStorage

class DocumentSearchToolSet(
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

data class TextChunk(
    val source: String,
    val content: String,
)

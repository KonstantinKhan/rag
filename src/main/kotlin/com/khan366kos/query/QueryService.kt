package com.khan366kos.query

import com.khan366kos.database.ChunkWithEmbedding
import com.khan366kos.database.DatabaseService
import com.khan366kos.ollama.OllamaClient
import com.khan366kos.utils.VectorUtils
import org.slf4j.LoggerFactory

/**
 * Service for querying the RAG database using similarity search
 */
class QueryService(private val ollamaClient: OllamaClient) {
    private val logger = LoggerFactory.getLogger(QueryService::class.java)

    /**
     * Search for chunks similar to the query
     * @param query The search query text
     * @param topK Number of top results to return (default: 5)
     * @return List of search results with similarity scores
     */
    suspend fun search(query: String, topK: Int = 5): List<SearchResult> {
        logger.info("Searching for: $query")

        // Generate embedding for the query
        val queryEmbedding = ollamaClient.generateEmbedding(query)
        val queryVector = VectorUtils.byteArrayToFloatArray(queryEmbedding)

        // Retrieve all chunks from database
        val allChunks = DatabaseService.getAllChunksWithEmbeddings()
        logger.info("Retrieved ${allChunks.size} chunks from database")

        if (allChunks.isEmpty()) {
            logger.warn("No chunks found in database")
            return emptyList()
        }

        // Calculate similarity for each chunk
        val results = allChunks.map { chunk ->
            val chunkVector = VectorUtils.byteArrayToFloatArray(chunk.embedding)
            val similarity = VectorUtils.cosineSimilarity(queryVector, chunkVector)

            SearchResult(
                chunk = chunk,
                similarity = similarity
            )
        }

        // Sort by similarity (highest first) and take top K
        return results
            .sortedByDescending { it.similarity }
            .take(topK)
    }
}

/**
 * Search result with similarity score
 */
data class SearchResult(
    val chunk: ChunkWithEmbedding,
    val similarity: Double
) {
    fun formatResult(index: Int): String {
        return buildString {
            appendLine("─".repeat(80))
            appendLine("Result #$index | Similarity: ${"%.4f".format(similarity)}")
            appendLine("File: ${chunk.fileName}")
            appendLine("Location: ${chunk.filePath}:${chunk.startPosition}-${chunk.endPosition}")
            appendLine("Chunk #${chunk.chunkIndex}")
            appendLine()
            appendLine(chunk.chunkText.trim())
            appendLine("─".repeat(80))
        }
    }
}
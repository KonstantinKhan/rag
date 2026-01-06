package com.khan366kos.query

import com.khan366kos.database.ChunkWithEmbedding
import com.khan366kos.database.DatabaseService
import com.khan366kos.ollama.OllamaClient
import com.khan366kos.utils.VectorUtils
import org.slf4j.LoggerFactory

/**
 * Service for querying the RAG database using similarity search
 */
class QueryService(
    private val ollamaClient: OllamaClient,
    private val rerankerService: RerankerService = RerankerService(),
) {
    private val logger = LoggerFactory.getLogger(QueryService::class.java)

    /**
     * Search for chunks similar to the query
     * @param query The search query text
     * @param topK Number of top results to return (default: 5)
     * @param useReranker Whether to use reranking (default: false)
     * @return List of search results with similarity scores
     */
    suspend fun search(query: String, topK: Int = 5, useReranker: Boolean = false): List<SearchResult> {
        logger.info("Searching for: $query, useReranker: $useReranker")

        // Generate embedding for the query
        val queryEmbedding = ollamaClient.generateEmbedding(query)
        val queryVector = VectorUtils.byteArrayToFloatArray(queryEmbedding)

        // Check if the query vector is empty
        if (queryVector.isEmpty()) {
            logger.error("Query embedding resulted in an empty vector. Cannot perform similarity search.")
            return emptyList()
        }

        // Retrieve all chunks from database
        val allChunks = DatabaseService.getAllChunksWithEmbeddings()
        logger.info("Retrieved ${allChunks.size} chunks from database")

        if (allChunks.isEmpty()) {
            logger.warn("No chunks found in database")
            return emptyList()
        }

        // Calculate similarity for each chunk
        val results = allChunks.mapNotNull { chunk ->
            try {
                val chunkVector = VectorUtils.byteArrayToFloatArray(chunk.embedding)
                val similarity = VectorUtils.cosineSimilarity(queryVector, chunkVector)

                SearchResult(
                    chunk = chunk,
                    similarity = similarity,
                    isReranked = false  // Initial similarity-based results are not reranked
                )
            } catch (e: Exception) {
                logger.warn("Failed to calculate similarity for chunk ${chunk.chunkId}: ${e.message}")
                null // Skip this chunk if similarity calculation fails
            }
        }

        // Sort by similarity (highest first)
        val sortedResults = results.sortedByDescending { it.similarity }

        // If reranking is enabled and we have a reranker service, use it
        return if (useReranker && rerankerService != null) {
            println("block if")
            rerankResults(query, sortedResults.take(topK * 2), topK) // Get more initial results for reranking
        } else {
            println("block else")
            // Otherwise, return top K results as before
            sortedResults.take(topK)
        }
    }

    private suspend fun rerankResults(query: String, initialResults: List<SearchResult>, topK: Int): List<SearchResult> {
        logger.info("Reranking ${initialResults.size} results for query: $query")

        try {
            // Extract document texts for reranking
            val documents = initialResults.map { it.chunk.chunkText }

            // Call the reranker service
            val rerankedResponse = rerankerService!!.rerank(query, documents, topK)

            // Map the reranked results back to SearchResult objects
            val rerankedResults = rerankedResponse.results.map { rerankResult ->
                val originalResult = initialResults[rerankResult.index]
                SearchResult(
                    chunk = originalResult.chunk,
                    similarity = rerankResult.score, // Use the reranker's score instead of similarity
                    isReranked = true  // Mark as reranked
                )
            }

            logger.info("Reranking completed, returning ${rerankedResults.size} results")
            return rerankedResults
        } catch (e: Exception) {
            logger.error("Reranking failed: ${e.message}", e)
            // Fallback to original similarity-based results
            return initialResults.take(topK)
        }
    }
}

/**
 * Search result with similarity score
 */
data class SearchResult(
    val chunk: ChunkWithEmbedding,
    val similarity: Double,
    val isReranked: Boolean = false
) {
    fun formatResult(index: Int): String {
        return buildString {
            appendLine("─".repeat(80))
            if (isReranked) {
                appendLine("Result #$index | Reranked Score: ${"%.4f".format(similarity)}")
                appendLine("Note: This result was re-ranked for relevance")
            } else {
                appendLine("Result #$index | Similarity: ${"%.4f".format(similarity)}")
            }
            appendLine("File: ${chunk.fileName}")
            appendLine("Location: ${chunk.filePath}:${chunk.startPosition}-${chunk.endPosition}")
            appendLine("Chunk #${chunk.chunkIndex}")
            appendLine()
            appendLine(chunk.chunkText.trim())
            appendLine("─".repeat(80))
        }
    }
}
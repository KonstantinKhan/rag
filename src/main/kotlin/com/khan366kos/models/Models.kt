package com.khan366kos.models

import kotlinx.serialization.Serializable

/**
 * Represents a markdown file with its metadata and content
 */
data class MarkdownFile(
    val absolutePath: String,
    val fileName: String,
    val modifiedDate: Long,
    val content: String
)

/**
 * Represents a text chunk with position tracking
 */
data class TextChunk(
    val index: Int,
    val text: String,
    val startPosition: Int,
    val endPosition: Int
)

/**
 * Request payload for Ollama embeddings API
 */
@Serializable
data class EmbeddingRequest(
    val model: String,
    val prompt: String
)

/**
 * Response from Ollama embeddings API
 */
@Serializable
data class EmbeddingResponse(
    val embedding: List<Float>
)
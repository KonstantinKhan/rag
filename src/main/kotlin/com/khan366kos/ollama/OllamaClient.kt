package com.khan366kos.ollama

import com.khan366kos.models.EmbeddingRequest
import com.khan366kos.models.EmbeddingResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Client for interacting with Ollama embeddings API
 * @param baseUrl Base URL for Ollama API (default: http://localhost:11434)
 * @param model Model name to use for embeddings (default: nomic-embed-text)
 */
class OllamaClient(
    private val baseUrl: String = "http://localhost:11434",
    private val model: String = "nomic-embed-text"
) {
    private val logger = LoggerFactory.getLogger(OllamaClient::class.java)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
        }
    }

    /**
     * Generate embedding for a text string
     * Includes retry logic with exponential backoff
     * @param text Text to generate embedding for
     * @param retries Number of retry attempts (default: 3)
     * @return ByteArray representation of the embedding (768 floats as bytes)
     */
    suspend fun generateEmbedding(text: String, retries: Int = 3): ByteArray {
        repeat(retries) { attempt ->
            try {
                val response = client.post("$baseUrl/api/embeddings") {
                    contentType(ContentType.Application.Json)
                    setBody(EmbeddingRequest(model = model, prompt = text))
                }

                val embeddingResponse = response.body<EmbeddingResponse>()
                return floatListToByteArray(embeddingResponse.embedding)

            } catch (e: Exception) {
                logger.warn("Attempt ${attempt + 1}/$retries failed: ${e.message}")
                if (attempt == retries - 1) {
                    logger.error("Failed to generate embedding after $retries attempts", e)
                    throw e
                }
                // Exponential backoff
                delay(1000L * (attempt + 1))
            }
        }
        throw RuntimeException("Failed to generate embedding after $retries attempts")
    }

    /**
     * Convert a list of floats to a ByteArray for BLOB storage
     * Uses little-endian byte order
     * @param floats List of float values (expected: 768 for nomic-embed-text)
     * @return ByteArray of size floats.size * 4
     */
    private fun floatListToByteArray(floats: List<Float>): ByteArray {
        val buffer = ByteBuffer.allocate(floats.size * 4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        floats.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    /**
     * Close the HTTP client
     */
    fun close() {
        client.close()
        logger.info("Ollama client closed")
    }
}
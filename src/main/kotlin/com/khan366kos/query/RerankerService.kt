package com.khan366kos.query

import com.khan366kos.models.RerankRequest
import com.khan366kos.models.RerankResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class RerankerService(
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = io.ktor.client.plugins.logging.Logger.DEFAULT
            level = io.ktor.client.plugins.logging.LogLevel.INFO
        }
    },
    private val baseUrl: String = "http://localhost:8002"
) {
    private val logger = LoggerFactory.getLogger(RerankerService::class.java)

    suspend fun rerank(query: String, documents: List<String>, topK: Int? = null): RerankResponse {
        logger.info("Sending rerank request for query: $query with ${documents.size} documents")

        // Validate input parameters
        if (query.isBlank()) {
            throw RerankException("Query cannot be blank")
        }
        if (documents.isEmpty()) {
            throw RerankException("Documents list cannot be empty")
        }

        try {
            val response: RerankResponse = httpClient.post("$baseUrl/rerank") {
                contentType(ContentType.Application.Json)
                setBody(RerankRequest(query, documents, topK))
            }.body()

            logger.info("Received rerank response with ${response.results.size} results")
            return response
        } catch (e: Exception) {
            logger.error("Error during rerank request: ${e.message}", e)
            throw RerankException("Failed to rerank documents: ${e.message}", e)
        }
    }

    suspend fun rerankTopK(query: String, documents: List<String>, topK: Int): List<String> {
        if (topK <= 0) {
            throw RerankException("topK must be greater than 0")
        }

        val response = rerank(query, documents, topK)
        return response.results.map { it.document }
    }

    fun close() {
        httpClient.close()
    }
}

class RerankException(message: String, cause: Throwable? = null) : Exception(message, cause)
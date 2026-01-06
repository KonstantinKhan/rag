package com.khan366kos.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RerankRequest(
    val query: String,
    val documents: List<String>,
    val top_k: Int? = null
)

@Serializable
data class RerankResponse(
    val query: String,
    val results: List<RerankResult>,
    @SerialName("processing_time_ms") val processingTimeMs: Double,
    val device: String
)

@Serializable
data class RerankResult(
    val index: Int,
    val document: String,
    val score: Double,
    val rank: Int
)
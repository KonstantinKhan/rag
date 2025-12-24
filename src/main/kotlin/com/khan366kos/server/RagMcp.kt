package com.khan366kos.server

import com.khan366kos.database.DatabaseService
import com.khan366kos.ollama.OllamaClient
import com.khan366kos.query.QueryService
import io.ktor.server.application.Application
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.random.Random

fun configureServer(queryService: QueryService): Server {
    val server = Server(
        Implementation(
            name = "RAG mcp server",
            version = "0.0.1"
        ),
        ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
            )
        )
    )

    server.addTool(
        name = "rag_data",
        description = "Tool to get additional data",
    ) { callToolRequest ->
        println(callToolRequest.arguments)
        var content = StringBuilder()

        val arguments = callToolRequest.arguments
        val query = arguments?.get("query")?.toString() ?: ""
        val useReranker = arguments?.get("use_reranker")?.toString()?.toBoolean() ?: false

        val chunkCount = DatabaseService.getChunkCount()
        if (chunkCount == 0L) {
            println("Database is empty. Please ingest some documents first using the Embeddings option.")
            content =
                content.append("Database is empty. Please ingest some documents first using the Embeddings option.")
        }

        println("Database contains $chunkCount chunks from ${DatabaseService.getDocumentCount()} documents")

        val results = queryService.search(query, if (useReranker) 1 else 5, useReranker)

        if (results.isEmpty()) {
            println("No results found.")
            content = content.append("No results found.")
        } else if (useReranker) {
            content.append(results.first().chunk.chunkText.trim())
        } else {
            val random = (0..<results.size).random()
            content.append(results[random].chunk.chunkText.trim())
        }

        CallToolResult(
            content = listOf(TextContent(content.toString()))
        )
    }

    return server
}

fun Application.configureMcpServer(queryService: QueryService) {
    mcp {
        return@mcp configureServer(queryService)
    }
}
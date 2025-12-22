package com.khan366kos

import com.khan366kos.chunker.TextChunker
import com.khan366kos.database.DatabaseService
import com.khan366kos.ollama.OllamaClient
import com.khan366kos.query.QueryService
import com.khan366kos.scanner.FileScanner
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("Main")

fun main() = runBlocking {
    logger.info("=== RAG System CLI ===")

    // Initialize database
    val dbPath = "./rag_database.db"
    try {
        DatabaseService.initialize(dbPath)
        logger.info("Database initialized at: $dbPath")
    } catch (e: Exception) {
        logger.error("Failed to initialize database: ${e.message}", e)
        exitProcess(1)
    }

    // Main command loop
    val ollamaClient = OllamaClient()
    try {
        while (true) {
            showMainMenu()
            when (readLine()?.trim()?.lowercase()) {
                "1", "embeddings", "e" -> runEmbeddings(ollamaClient)
                "2", "query", "q" -> runQuery(ollamaClient)
                "3", "stats", "s" -> showStats()
                "4", "exit", "x" -> {
                    println("\nExiting... Goodbye!")
                    break
                }
                else -> println("Invalid option. Please try again.\n")
            }
        }
    } finally {
        ollamaClient.close()
        logger.info("Application closed")
    }
}

fun showMainMenu() {
    println("\n" + "═".repeat(60))
    println("  RAG System - Main Menu")
    println("═".repeat(60))
    println("  1. [E]mbeddings  - Ingest markdown files into database")
    println("  2. [Q]uery       - Search for similar content")
    println("  3. [S]tats       - Show database statistics")
    println("  4. E[x]it        - Quit application")
    println("═".repeat(60))
    print("Select an option: ")
}

suspend fun runEmbeddings(ollamaClient: OllamaClient) {
    println("\n=== Embeddings Mode ===")

    // Get folder path from user
    println("Enter the folder path to scan for markdown files:")
    val folderPath = readLine()?.trim()

    if (folderPath.isNullOrBlank()) {
        println("Error: Folder path cannot be empty")
        return
    }

    // Validate folder
    val folder = File(folderPath)
    if (!folder.exists()) {
        println("Error: Folder '$folderPath' does not exist")
        return
    }

    if (!folder.isDirectory) {
        println("Error: '$folderPath' is not a directory")
        return
    }

    logger.info("Starting ingestion from: ${folder.absolutePath}")
    println("\nScanning for markdown files...")

    // Initialize components
    val fileScanner = FileScanner()
    val textChunker = TextChunker(chunkSize = 512, overlap = 50)

    try {
        // Scan for markdown files
        val mdFiles = fileScanner.scanDirectory(folder)

        if (mdFiles.isEmpty()) {
            println("No markdown files found in the specified directory")
            return
        }

        println("Found ${mdFiles.size} markdown file(s)\n")
        logger.info("Processing ${mdFiles.size} markdown files")

        // Process each file
        mdFiles.forEachIndexed { index, file ->
            val fileNumber = index + 1
            logger.info("[$fileNumber/${mdFiles.size}] Processing: ${file.fileName}")
            println("[$fileNumber/${mdFiles.size}] Processing: ${file.fileName}")

            try {
                // Chunk the text
                val chunks = textChunker.chunk(file.content)
                logger.info("  Created ${chunks.size} chunks")
                println("  - Created ${chunks.size} chunk(s)")

                // Save document to database
                val documentId = DatabaseService.saveDocument(file)
                logger.debug("  Document saved with ID: $documentId")

                // Generate embeddings and save chunks
                println("  - Generating embeddings...")
                chunks.forEachIndexed { chunkIdx, chunk ->
                    try {
                        val embedding = ollamaClient.generateEmbedding(chunk.text)
                        DatabaseService.saveChunk(documentId, chunk, embedding)

                        // Progress indicator for large files
                        if ((chunkIdx + 1) % 10 == 0) {
                            logger.info("    Processed ${chunkIdx + 1}/${chunks.size} chunks")
                            println("    Progress: ${chunkIdx + 1}/${chunks.size} chunks")
                        }
                    } catch (e: Exception) {
                        logger.error("    Failed to process chunk $chunkIdx: ${e.message}", e)
                        println("    Warning: Failed to process chunk $chunkIdx: ${e.message}")
                    }
                }

                logger.info("  Completed: ${file.fileName}")
                println("  ✓ Completed\n")

            } catch (e: Exception) {
                logger.error("  Failed to process file ${file.fileName}: ${e.message}", e)
                println("  ✗ Error: ${e.message}\n")
            }
        }

        println("=== Ingestion Complete ===")
        logger.info("Ingestion completed successfully!")
        println("Processed ${mdFiles.size} files")

    } catch (e: Exception) {
        logger.error("Ingestion failed: ${e.message}", e)
        println("Error during ingestion: ${e.message}")
    }
}

suspend fun runQuery(ollamaClient: OllamaClient) {
    println("\n=== Query Mode ===")

    // Check if database has data
    val chunkCount = DatabaseService.getChunkCount()
    if (chunkCount == 0L) {
        println("Database is empty. Please ingest some documents first using the Embeddings option.")
        return
    }

    println("Database contains $chunkCount chunks from ${DatabaseService.getDocumentCount()} documents")

    // Get query from user
    println("\nEnter your search query (or 'back' to return to main menu):")
    val query = readLine()?.trim()

    if (query.isNullOrBlank()) {
        println("Error: Query cannot be empty")
        return
    }

    if (query.lowercase() == "back") {
        return
    }

    // Get number of results
    println("How many results? (default: 5)")
    val topK = readLine()?.trim()?.toIntOrNull() ?: 5

    println("\nSearching for: \"$query\"")
    println("Please wait...\n")

    try {
        val queryService = QueryService(ollamaClient)
        val results = queryService.search(query, topK)

        if (results.isEmpty()) {
            println("No results found.")
            return
        }

        println("\n" + "═".repeat(80))
        println("  Found ${results.size} results")
        println("═".repeat(80))

        results.forEachIndexed { index, result ->
            println(result.formatResult(index + 1))
        }

    } catch (e: Exception) {
        logger.error("Query failed: ${e.message}", e)
        println("Error during query: ${e.message}")
    }
}

fun showStats() {
    println("\n=== Database Statistics ===")
    try {
        val docCount = DatabaseService.getDocumentCount()
        val chunkCount = DatabaseService.getChunkCount()

        println("Documents: $docCount")
        println("Chunks: $chunkCount")

        if (docCount > 0) {
            val avgChunksPerDoc = chunkCount.toDouble() / docCount
            println("Average chunks per document: ${"%.2f".format(avgChunksPerDoc)}")
        }

        println("Database file: ./rag_database.db")
    } catch (e: Exception) {
        logger.error("Failed to get stats: ${e.message}", e)
        println("Error getting statistics: ${e.message}")
    }
}
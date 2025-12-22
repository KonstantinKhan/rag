package com.khan366kos.database

import com.khan366kos.models.MarkdownFile
import com.khan366kos.models.TextChunk
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Service for managing database operations
 */
object DatabaseService {
    private val logger = LoggerFactory.getLogger(DatabaseService::class.java)

    /**
     * Initialize database connection and create schema
     */
    fun initialize(dbPath: String) {
        Database.connect(
            url = "jdbc:sqlite:$dbPath",
            driver = "org.sqlite.JDBC"
        )

        // Create database schema
        transaction {
            SchemaUtils.create(Documents, Chunks)
            logger.info("Database schema created/verified at: $dbPath")
        }
    }

    /**
     * Save a markdown file to the database
     * If file already exists (by path), delete old record and chunks
     * Returns the new document ID
     */
    fun saveDocument(file: MarkdownFile): Int = transaction {
        // Check if document already exists
        Documents.selectAll().where { Documents.filePath eq file.absolutePath }
            .firstOrNull()?.let { existingDoc ->
                val docId = existingDoc[Documents.id]
                // Delete associated chunks (CASCADE should handle this, but being explicit)
                Chunks.deleteWhere { documentId eq docId }
                Documents.deleteWhere { id eq docId }
                logger.info("Deleted existing document and chunks: ${file.fileName}")
            }

        // Insert new document
        Documents.insert {
            it[filePath] = file.absolutePath
            it[fileName] = file.fileName
            it[modifiedDate] = file.modifiedDate
            it[createdAt] = System.currentTimeMillis()
        } get Documents.id
    }

    /**
     * Save a chunk with its embedding to the database
     */
    fun saveChunk(documentId: Int, chunk: TextChunk, embedding: ByteArray) = transaction {
        Chunks.insert {
            it[Chunks.documentId] = documentId
            it[chunkIndex] = chunk.index
            it[chunkText] = chunk.text
            it[startPosition] = chunk.startPosition
            it[endPosition] = chunk.endPosition
            it[Chunks.embedding] = ExposedBlob(embedding)
            it[createdAt] = System.currentTimeMillis()
        }
    }

    /**
     * Save multiple chunks with embeddings in a batch operation
     * More efficient for large numbers of chunks
     */
    fun saveChunksBatch(documentId: Int, chunks: List<Pair<TextChunk, ByteArray>>) = transaction {
        Chunks.batchInsert(chunks) { (chunk, embedding) ->
            this[Chunks.documentId] = documentId
            this[Chunks.chunkIndex] = chunk.index
            this[Chunks.chunkText] = chunk.text
            this[Chunks.startPosition] = chunk.startPosition
            this[Chunks.endPosition] = chunk.endPosition
            this[Chunks.embedding] = ExposedBlob(embedding)
            this[Chunks.createdAt] = System.currentTimeMillis()
        }
    }

    /**
     * Retrieve all chunks with their embeddings and document information
     * Used for similarity search
     */
    fun getAllChunksWithEmbeddings(): List<ChunkWithEmbedding> = transaction {
        (Chunks innerJoin Documents)
            .selectAll()
            .map { row ->
                ChunkWithEmbedding(
                    chunkId = row[Chunks.id],
                    documentId = row[Documents.id],
                    fileName = row[Documents.fileName],
                    filePath = row[Documents.filePath],
                    chunkIndex = row[Chunks.chunkIndex],
                    chunkText = row[Chunks.chunkText],
                    startPosition = row[Chunks.startPosition],
                    endPosition = row[Chunks.endPosition],
                    embedding = row[Chunks.embedding].bytes
                )
            }
    }

    /**
     * Get count of documents in the database
     */
    fun getDocumentCount(): Long = transaction {
        Documents.selectAll().count()
    }

    /**
     * Get count of chunks in the database
     */
    fun getChunkCount(): Long = transaction {
        Chunks.selectAll().count()
    }
}

/**
 * Data class representing a chunk with its embedding and document metadata
 */
data class ChunkWithEmbedding(
    val chunkId: Int,
    val documentId: Int,
    val fileName: String,
    val filePath: String,
    val chunkIndex: Int,
    val chunkText: String,
    val startPosition: Int,
    val endPosition: Int,
    val embedding: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChunkWithEmbedding

        if (chunkId != other.chunkId) return false

        return true
    }

    override fun hashCode(): Int {
        return chunkId
    }
}
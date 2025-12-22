package com.khan366kos.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

/**
 * Database table for storing document metadata
 */
object Documents : Table("documents") {
    val id = integer("id").autoIncrement()
    val filePath = varchar("file_path", 1024)
    val fileName = varchar("file_name", 512)
    val modifiedDate = long("modified_date")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = true, filePath)
    }
}

/**
 * Database table for storing text chunks with embeddings
 */
object Chunks : Table("chunks") {
    val id = integer("id").autoIncrement()
    val documentId = integer("document_id").references(Documents.id, onDelete = ReferenceOption.CASCADE)
    val chunkIndex = integer("chunk_index")
    val chunkText = text("chunk_text")
    val startPosition = integer("start_position")
    val endPosition = integer("end_position")
    val embedding = blob("embedding")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, documentId)
    }
}
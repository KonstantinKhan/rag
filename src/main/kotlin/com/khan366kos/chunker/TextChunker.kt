package com.khan366kos.chunker

import com.khan366kos.models.TextChunk

/**
 * Chunker for splitting text into overlapping chunks
 * @param chunkSize Size of each chunk in characters
 * @param overlap Number of characters to overlap between chunks
 */
class TextChunker(
    private val chunkSize: Int = 512,
    private val overlap: Int = 50
) {
    private val step = chunkSize - overlap

    /**
     * Split text into overlapping chunks
     * Returns a list of TextChunk objects with position tracking
     */
    fun chunk(text: String): List<TextChunk> {
        if (text.isEmpty()) return emptyList()

        val chunks = mutableListOf<TextChunk>()
        var index = 0
        var position = 0

        while (position < text.length) {
            val end = minOf(position + chunkSize, text.length)
            val chunkText = text.substring(position, end)

            chunks.add(
                TextChunk(
                    index = index,
                    text = chunkText,
                    startPosition = position,
                    endPosition = end
                )
            )

            index++
            position += step

            // Break if we've processed the end
            if (end == text.length) break
        }

        return chunks
    }
}
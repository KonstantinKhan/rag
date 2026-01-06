package com.khan366kos.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Utility functions for vector operations
 */
object VectorUtils {

    /**
     * Convert ByteArray (BLOB from database) back to FloatArray
     * Assumes little-endian byte order
     */
    fun byteArrayToFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        val floats = FloatArray(bytes.size / 4)
        for (i in floats.indices) {
            floats[i] = buffer.getFloat()
        }
        return floats
    }

    /**
     * Calculate cosine similarity between two vectors
     * Returns a value between -1 and 1, where 1 means identical vectors
     */
    fun cosineSimilarity(vector1: FloatArray, vector2: FloatArray): Double {
        // Check if either vector is empty
        if (vector1.isEmpty() || vector2.isEmpty()) {
            println("Warning: One or both vectors are empty. Returning 0.0 similarity.")
            return 0.0
        }

        // Check if vectors have the same dimension
        if (vector1.size != vector2.size) {
            println("Warning: Vectors have different dimensions (${vector1.size} vs ${vector2.size}). Aligning dimensions by truncating to smaller size.")
        }

        // Align vectors to the same dimension by truncating to the smaller size
        val alignedVector1: FloatArray
        val alignedVector2: FloatArray

        when {
            vector1.size == vector2.size -> {
                // Same size, no alignment needed
                alignedVector1 = vector1
                alignedVector2 = vector2
            }
            vector1.size > vector2.size -> {
                // vector1 is larger, truncate it to match vector2's size
                alignedVector1 = vector1.copyOfRange(0, vector2.size)
                alignedVector2 = vector2
            }
            else -> { // vector2.size > vector1.size
                // vector2 is larger, truncate it to match vector1's size
                alignedVector1 = vector1
                alignedVector2 = vector2.copyOfRange(0, vector1.size)
            }
        }

        var dotProduct = 0.0
        var magnitude1 = 0.0
        var magnitude2 = 0.0

        for (i in alignedVector1.indices) {
            dotProduct += alignedVector1[i] * alignedVector2[i]
            magnitude1 += alignedVector1[i] * alignedVector1[i]
            magnitude2 += alignedVector2[i] * alignedVector2[i]
        }

        magnitude1 = sqrt(magnitude1)
        magnitude2 = sqrt(magnitude2)

        return if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            0.0
        } else {
            dotProduct / (magnitude1 * magnitude2)
        }
    }
}

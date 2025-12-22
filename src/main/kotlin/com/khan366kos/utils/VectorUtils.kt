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
        require(vector1.size == vector2.size) {
            "Vectors must have the same dimension"
        }

        var dotProduct = 0.0
        var magnitude1 = 0.0
        var magnitude2 = 0.0

        for (i in vector1.indices) {
            dotProduct += vector1[i] * vector2[i]
            magnitude1 += vector1[i] * vector1[i]
            magnitude2 += vector2[i] * vector2[i]
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

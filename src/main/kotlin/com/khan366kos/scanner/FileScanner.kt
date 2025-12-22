package com.khan366kos.scanner

import com.khan366kos.models.MarkdownFile
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Scanner for recursively finding and reading markdown files
 */
class FileScanner {
    private val logger = LoggerFactory.getLogger(FileScanner::class.java)

    /**
     * Recursively scan a directory for markdown files
     * Returns a list of MarkdownFile objects with content and metadata
     */
    fun scanDirectory(folder: File): List<MarkdownFile> {
        logger.info("Scanning directory: ${folder.absolutePath}")

        val mdFiles = folder.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() == "md" }
            .mapNotNull { file ->
                try {
                    MarkdownFile(
                        absolutePath = file.absolutePath,
                        fileName = file.name,
                        modifiedDate = file.lastModified(),
                        content = file.readText(Charsets.UTF_8)
                    )
                } catch (e: Exception) {
                    logger.warn("Failed to read file ${file.absolutePath}: ${e.message}")
                    null
                }
            }
            .toList()

        logger.info("Found ${mdFiles.size} markdown files")
        return mdFiles
    }
}
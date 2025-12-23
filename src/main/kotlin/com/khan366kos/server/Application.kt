package com.khan366kos.server

import com.khan366kos.database.DatabaseService
import com.khan366kos.ollama.OllamaClient
import com.khan366kos.query.QueryService
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.netty.EngineMain
import jdk.jfr.internal.Logger.log


fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    // Initialize database connection
    val dbPath = "./rag_database.db"
    DatabaseService.initialize(dbPath)
    log.info("Database initialized at: $dbPath")

    val ollamaClient = OllamaClient()
    val queryService = QueryService(ollamaClient)

    configureSerialization()
    configureHTTP()
    configureMcpServer(queryService)
}
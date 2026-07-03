package io.lanzof.ingestor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IngestorApplication

fun main(args: Array<String>) {
    runApplication<IngestorApplication>(*args)
}
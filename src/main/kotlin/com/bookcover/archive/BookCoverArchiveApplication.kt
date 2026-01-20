package com.bookcover.archive

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan("com.bookcover.archive.config")
class BookCoverArchiveApplication

fun main(args: Array<String>) {
    runApplication<BookCoverArchiveApplication>(*args)
}

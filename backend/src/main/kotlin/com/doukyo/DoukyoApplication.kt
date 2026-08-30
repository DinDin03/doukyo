package com.doukyo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// @SpringBootApplication = the app's root. It turns on component scanning (find our
// @Controller/@Service beans), auto-configuration (wire up the web server, GraphQL,
// etc. from what's on the classpath), and marks this as a config class.
@SpringBootApplication
class DoukyoApplication

fun main(args: Array<String>) {
    runApplication<DoukyoApplication>(*args)
}

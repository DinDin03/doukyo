package com.doukyo.health
import java.time.LocalTime

import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

// A GraphQL resolver. In Spring for GraphQL, a @Controller holds the functions that
// "resolve" schema fields. @QueryMapping binds this function to the `ping` field on
// the Query type (name matches by default), so `query { ping }` runs ping() and
// returns its result under "ping".
//
// This is the whole request/response loop in miniature (design doc §8):
//   client sends `{ ping }`  ->  Spring routes to ping()  ->  returns "pong".
@Controller
class PingController {

    @QueryMapping
    fun ping(): String = "pong at ${LocalTime.now()}"

    @QueryMapping
    fun greeting(): String = "Welcome to the share house ${LocalTime.now()}"

    @QueryMapping
    fun hello(): String = "Hello World"
}

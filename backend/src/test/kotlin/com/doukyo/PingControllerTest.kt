package com.doukyo

import com.doukyo.health.PingController
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.test.tester.GraphQlTester

// @GraphQlTest boots ONLY the GraphQL layer (fast slice test) and wires up a
// GraphQlTester we can fire queries at — no HTTP, no database.
@GraphQlTest(PingController::class)
class PingControllerTest(@Autowired val graphQlTester: GraphQlTester) {

    @Test
    fun `ping returns pong`() {
        graphQlTester
            .document("{ ping }")
            .execute()
            .path("ping")
            .entity(String::class.java)
            .isEqualTo("pong")
    }
}

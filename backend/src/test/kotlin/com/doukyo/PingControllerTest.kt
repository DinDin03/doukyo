package com.doukyo

import com.doukyo.health.PingController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest
import org.springframework.graphql.test.tester.GraphQlTester

// @GraphQlTest boots ONLY the GraphQL layer — no HTTP, no database.
@GraphQlTest(PingController::class)
class PingControllerTest(@Autowired val graphQlTester: GraphQlTester) {

    @Test
    fun `ping answers`() {
        val body = graphQlTester.document("{ ping }").execute()
            .path("ping").entity(String::class.java).get()
        // The response carries a timestamp, so assert the shape, not the exact text.
        assertThat(body).startsWith("pong")
    }

    @Test
    fun `greeting and hello answer`() {
        assertThat(graphQlTester.document("{ greeting }").execute()
            .path("greeting").entity(String::class.java).get()).contains("share house")
        assertThat(graphQlTester.document("{ hello }").execute()
            .path("hello").entity(String::class.java).get()).isEqualTo("Hello World")
    }
}

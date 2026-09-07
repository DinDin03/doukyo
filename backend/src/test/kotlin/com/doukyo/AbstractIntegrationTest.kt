package com.doukyo

import com.doukyo.user.User
import com.doukyo.user.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.sql.DriverManager

// Integration tests run against a REAL Postgres with the REAL Flyway migrations —
// never H2. The migrations use Postgres-specific syntax (TIMESTAMPTZ, partial
// indexes, GENERATED AS IDENTITY), and this codebase deliberately pushes its
// guarantees into DB constraints, so mocking the repository would only test the
// weaker application-level check.
//
// It reuses the docker-compose Postgres on 5433 rather than Testcontainers: Docker
// Engine 29 reports MinAPIVersion 1.40 and rejects the API version Testcontainers
// hardcodes in its probe (v1.32), which breaks container startup on every release
// up to 1.21.3. Switch back to Testcontainers once that is fixed upstream.
//
// Requires `docker compose up -d` first. The test DATABASE is created automatically,
// separate from dev data, and truncated between tests.
@SpringBootTest
abstract class AbstractIntegrationTest {

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    protected lateinit var userRepository: UserRepository

    // A persisted user with no credentials. Auth tests build users through
    // AuthService instead, so they exercise hashing.
    protected fun newUser(name: String): User =
        userRepository.save(User(name = name, email = "${name.lowercase()}@test.app"))

    // Wiped between tests rather than rolled back: the services manage their own
    // transactions, and ChatService publishes on afterCommit — a test-owned
    // transaction that always rolled back would silently skip that path.
    @BeforeEach
    fun resetDatabase() {
        jdbc.execute(
            "TRUNCATE messages, message_reads, expense_shares, expenses, " +
                "refresh_tokens, oauth_accounts, memberships, households, users " +
                "RESTART IDENTITY CASCADE",
        )
    }

    companion object {
        private const val HOST = "localhost:5433"
        private const val USER = "doukyo"
        private const val PASSWORD = "doukyo"
        private const val TEST_DB = "doukyo_test"

        init {
            try {
                DriverManager.getConnection("jdbc:postgresql://$HOST/postgres", USER, PASSWORD).use { conn ->
                    val exists = conn.createStatement()
                        .executeQuery("SELECT 1 FROM pg_database WHERE datname = '$TEST_DB'")
                        .next()
                    if (!exists) conn.createStatement().execute("CREATE DATABASE $TEST_DB")
                }
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Integration tests need the dev Postgres on $HOST. Run `docker compose up -d` first.",
                    e,
                )
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { "jdbc:postgresql://$HOST/$TEST_DB" }
            registry.add("spring.datasource.username") { USER }
            registry.add("spring.datasource.password") { PASSWORD }
        }
    }
}

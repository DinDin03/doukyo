package com.doukyo

import com.doukyo.auth.EmailSender
import com.doukyo.user.User
import com.doukyo.user.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
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
@org.springframework.context.annotation.Import(TestEmailConfig::class)
abstract class AbstractIntegrationTest {

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    protected lateinit var userRepository: UserRepository

    // A real fake, not a Mockito mock: ArgumentCaptor.capture() and eq() both
    // return null, which Kotlin's non-null parameter checks reject. Recording the
    // calls directly is shorter and reads better than working around that.
    @Autowired
    protected lateinit var emailSender: RecordingEmailSender

    // The emailed code is stored hashed and never returned, so this is the only
    // way a test can complete a sign-up.
    protected fun codeSentTo(email: String): String =
        emailSender.codes[email.trim().lowercase()]
            ?: error("No sign-up code was emailed to $email")

    // A persisted user with no credentials. Auth tests build users through
    // AuthService instead, so they exercise hashing.
    protected fun newUser(name: String): User =
        userRepository.save(User(name = name, email = "${name.lowercase()}@test.app"))

    // Wiped between tests rather than rolled back: the services manage their own
    // transactions, and ChatService publishes on afterCommit — a test-owned
    // transaction that always rolled back would silently skip that path.
    @BeforeEach
    fun resetEmails() = emailSender.clear()

    // The table list is read from the database rather than hardcoded. A literal
    // list silently rots the moment a migration adds a table — pending_signups
    // was missed exactly that way, and stale rows then leaked between tests.
    @BeforeEach
    fun resetDatabase() {
        val tables = jdbc.queryForList(
            "SELECT tablename FROM pg_tables WHERE schemaname = 'public' " +
                "AND tablename <> 'flyway_schema_history'",
            String::class.java,
        )
        if (tables.isNotEmpty()) {
            jdbc.execute("TRUNCATE ${tables.joinToString()} RESTART IDENTITY CASCADE")
        }
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

class RecordingEmailSender : EmailSender {
    val codes = mutableMapOf<String, String>()
    var sendCount = 0
    val existingAccountNotices = mutableListOf<String>()

    override fun sendSignUpCode(email: String, code: String) {
        codes[email] = code
        sendCount += 1
    }

    override fun sendSignUpAttemptOnExistingAccount(email: String) {
        existingAccountNotices += email
    }

    fun clear() {
        codes.clear()
        existingAccountNotices.clear()
        sendCount = 0
    }
}

// @Primary so it wins over the production LoggingEmailSender. Declared here so
// every integration test shares one Spring context rather than spinning up a new
// one per test class.
@TestConfiguration
class TestEmailConfig {
    @Bean
    @Primary
    fun recordingEmailSender() = RecordingEmailSender()
}

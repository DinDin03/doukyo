plugins {
    // Kotlin compiler for the JVM.
    kotlin("jvm") version "1.9.25"
    // Makes Kotlin classes `open` where Spring needs to proxy them (Spring uses
    // CGLIB subclassing for @Configuration, @Transactional, etc.). Kotlin classes
    // are `final` by default, so without this Spring can't create proxies.
    kotlin("plugin.spring") version "1.9.25"
    // Generates the no-arg constructor JPA/Hibernate needs to instantiate @Entity
    // classes via reflection (Kotlin classes with ctor params lack one otherwise).
    kotlin("plugin.jpa") version "1.9.25"
    // Brings in the Spring Boot Gradle tasks (bootRun, bootJar) and BOM alignment.
    id("org.springframework.boot") version "3.3.5"
    // Lets us declare Spring dependencies without version numbers — the BOM picks
    // versions known to work together. (Dependency management done right.)
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.doukyo"
version = "0.0.1-SNAPSHOT"

// Build against Java 21 regardless of the machine's default `java`. Gradle's
// toolchain finds a matching JDK, so the build is reproducible across machines.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // --- Web + GraphQL ---
    // Spring MVC: the HTTP server layer (embedded Tomcat, controllers, JSON).
    implementation("org.springframework.boot:spring-boot-starter-web")
    // Spring for GraphQL: the single /graphql endpoint, schema wiring, @QueryMapping,
    // and (in dev) the GraphiQL in-browser query IDE. Built on graphql-java.
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    // Bean Validation (@NotNull, @Positive, ...) — we'll lean on this for input rules.
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // --- Kotlin support ---
    // Teaches Jackson (JSON) how to construct Kotlin classes (no-arg constructors,
    // nullability, data classes).
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    // Kotlin reflection — Spring and Jackson need it at runtime.
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // --- Database (Phase 0b) ---
    // Spring Data JPA: Hibernate ORM + the repository abstraction (save/findById/...).
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // The PostgreSQL JDBC driver — needed only at runtime, not to compile against.
    runtimeOnly("org.postgresql:postgresql")
    // Flyway: versioned SQL schema migrations. Since Flyway 10, PostgreSQL support
    // lives in a separate module that must be declared explicitly.
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // --- Security & auth ---
    // Spring Security: the servlet filter chain, authentication plumbing, and
    // BCryptPasswordEncoder for hashing passwords.
    implementation("org.springframework.boot:spring-boot-starter-security")
    // JWT: create and verify signed tokens. Code against -api; the -impl and
    // -jackson artifacts are pulled in only at runtime.
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // --- Testing ---
    // JUnit 5, MockMvc, AssertJ, Mockito — the standard Spring test stack.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Helpers for testing GraphQL queries (GraphQlTester).
    testImplementation("org.springframework.graphql:spring-graphql-test")
    // JUnit 5 launcher on the test runtime classpath.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        // Treat Spring's nullability annotations as Kotlin nullability, and fail on
        // warnings so problems surface early.
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

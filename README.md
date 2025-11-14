# xemantic-neo4j-demo

**Demo showcasing the [xemantic-neo4j-kotlin-driver](https://github.com/xemantic/xemantic-neo4j-kotlin-driver) - Kotlin coroutine extensions for Neo4j**

## What This Demonstrates

This project is a comprehensive demonstration of building Neo4j-backed REST APIs using the [xemantic-neo4j-kotlin-driver](https://github.com/xemantic/xemantic-neo4j-kotlin-driver) library, which provides:

- **Fully async/non-blocking operations** - Coroutines everywhere, no blocking calls
- **Idiomatic Kotlin patterns** - Suspend functions instead of `CompletionStage` callbacks
- **Resource-safe session management** - `.use { }` blocks prevent connection pool exhaustion
- **Flow-based result streaming** - Memory-efficient processing of large datasets
- **Type-safe data access** - Kotlin type system + Neo4j value conversion

## Quick Example: Complete Async API in One File

Here's [`SequenceApi.kt`](src/main/kotlin/sequences/SequenceApi.kt) showing all key features - a complete server with streaming endpoints using coroutines throughout:

```kotlin
fun Application.sequenceApi() {

    val neo4j: Neo4jOperations by dependencies

    routing {

        // Streaming large result sets with Flow
        get("/sequences/{count}") {
            val count = call.parameters["count"]!!.toInt()
            call.respondTextWriter(
                contentType = ContentType.Text.Plain
            ) {
                neo4j.flow(
                    query = $$"UNWIND range(1, $count) AS n RETURN n",
                    parameters = mapOf("count" to count)
                ).collect {
                    val n = it["n"].asInt()
                    write("$n")
                    if (n < count) write("\n")
                    flush()
                }
            }
        }

    }

}
```

**Key Features Demonstrated:**
- `neo4j.read { tx -> }` - Coroutine-based read transactions (no callbacks)
- `neo4j.flow(query, parameters)` - Returns Kotlin `Flow<Record>` for streaming
- `.collect { }` - Process records incrementally without loading all into memory
- `respondTextWriter` + `flush()` - Non-blocking HTTP streaming to client
- Parameterized queries with `$$` string interpolation

This example depends on [Ktor](https://ktor.io/) and demonstrates how to set up the library as a Ktor module. For comprehensive Ktor documentation, see the [official Ktor documentation](https://ktor.io/docs/).

### Running the Demo

First, build the executable JAR:

```bash
./gradlew uberjar
```

Then set the required environment variables and run:

```bash
export NEO4J_URI="neo4j://localhost:7687"
export NEO4J_USER="neo4j"
export NEO4J_PASSWORD="your-password"

java -jar build/libs/xemantic-neo4j-demo-0.1.0-SNAPSHOT-uberjar.jar
```

The server will start on port 8080 (configurable in `application.yaml`).

### Setting Up as a Ktor Module

Here's how to start a minimal server with the library:

```kotlin
fun main() {

    val neo4jUri = System.getenv("NEO4J_URI")
    val neo4jUser = System.getenv("NEO4J_USER")
    val neo4jPassword = System.getenv("NEO4J_PASSWORD")

    val driver = GraphDatabase.driver(
        neo4jUri,
        AuthTokens.basic(neo4jUser, neo4jPassword)
    ).apply {
        verifyConnectivity()
    }

    // this should be no greater than driver's max session limit witch is 100 by default
    val dispatcher = Dispatchers.IO.limitedParallelism(90)

    val neo4jOperations = DispatchedNeo4jOperations(
        driver, dispatcher
    )

    embeddedServer(Netty, port = 8080) {
        dependencies.provide<Neo4jOperations> { neo4jOperations }
        sequenceApi()
    }.start(wait = true)

}
```

## What the Library Provides

Built on the [xemantic-neo4j-kotlin-driver](https://github.com/xemantic/xemantic-neo4j-kotlin-driver), this demo showcases:

### Coroutine-Based Database Operations

- **Suspend functions** instead of `CompletionStage` callbacks for cleaner async code
- Natural integration with Kotlin's coroutine ecosystem

### Session Management with Resource Safety

- `driver.coroutineSession()` for creating coroutine-friendly sessions
- `.use { }` extension for automatic resource cleanup (prevents connection pool exhaustion)

### Transaction Execution

- `session.executeRead { tx -> }` for read transactions
- `session.executeWrite { tx -> }` for write transactions
- Support for transaction configuration (timeouts, metadata, database selection)

### Flow-Based Result Streaming
- `result.records()` returns Kotlin `Flow<Record>` for efficient streaming
- Demonstrated in `/sequences/{count}` endpoint with incremental response writing
- Memory-efficient processing of large result sets

### Type-Safe Data Access
- Neo4j value conversion using `.asString()`, `.asInt()`, etc.
- Kotlinx serialization for REST API models
- Integration with Ktor's content negotiation

## Running the Demo

```bash
# Build the project
./gradlew build

# Run tests (uses embedded Neo4j via neo4j-harness)
./gradlew test

# Create an executable fat JAR
./gradlew uberjar
```

### Complete API Endpoints

The demo includes a full CRUD API for managing persons and relationships:

**Health Check:**
- `GET /health` - Database connectivity check with Neo4j temporal type demonstration
  - Returns `200 OK` with `{"status": "healthy", "timestamp": "..."}` when database is accessible
  - Returns `503 Service Unavailable` with error details if database is unreachable
  - Demonstrates Neo4j `datetime()` function and `.asInstant()` conversion to `kotlin.time.Instant`

**Simple Examples:**
- `GET /sequences/{count}` - Stream numbers from Neo4j (demonstrates Flow-based streaming)

**CRUD Operations:**
- `POST /people` - Create a person node
- `GET /people` - List all persons
- `GET /people/{id}` - Get a specific person
- `POST /people/{id}/knows/{otherId}` - Create a KNOWS relationship
- `GET /people/{id}/friends` - Get friends via graph traversal

### Key Implementation Files

- [`HealthCheckApi.kt`](src/main/kotlin/HealthCheckApi.kt) - Health check endpoint with temporal type handling
- [`SequenceApi.kt`](src/main/kotlin/sequences/SequenceApi.kt) - Complete server example in one file
- [`Neo4jDependency.kt`](src/main/kotlin/Neo4jDependency.kt) - Neo4j driver setup using Ktor's DI
- [`Server.kt`](src/main/kotlin/Server.kt) - Full REST API with CRUD operations
- [`PeopleRepository.kt`](src/main/kotlin/people/PeopleRepository.kt) - Repository pattern with coroutine operations
- [`PeopleApiTest.kt`](src/test/kotlin/PeopleApiTest.kt) - Tests using embedded Neo4j

## Bonus: Vibe-Coding with AI

Beyond demonstrating the library, this project is architected for **AI-driven development** using Test-Driven Development:

### Why This Works for AI Development

- **Test-First Foundation**: `xemantic-kotlin-test` DSL creates readable specs AI can understand
- **Embedded Testing**: `neo4j-harness` provides instant feedback without external dependencies
- **Type Safety**: Kotlin + Power Assert plugin = crystal-clear error messages
- **Clear Patterns**: Consistent architecture (DI, layers, resource management) AI can follow
- **Self-Documenting**: [CLAUDE.md](CLAUDE.md) guides AI behavior and coding standards

### AI-Assisted Development Workflow

With tools like Claude Code, you can:

1. **Describe what you want** in natural language
2. **AI writes tests** that capture your requirements
3. **AI implements** functionality to satisfy those tests
4. **AI iterates** autonomously - runs `./gradlew test`, interprets failures, refines code
5. **You get working features** with test coverage and documentation

### Example Session

```
You: "I need an endpoint to find mutual friends between two people"

AI: [Writes test case in PeopleApiTest.kt]
    [Implements GET /people/{id}/mutual-friends/{otherId}]
    [Runs tests, fixes issues, iterates]
    [Reports: ✓ All tests passing]
```

The combination of:
- Coroutine-based Neo4j driver (clear async patterns)
- Embedded test infrastructure (fast feedback)
- Type-safe DSLs (readable code)

...makes this codebase particularly well-suited for autonomous AI development.

## Using This as a Blueprint

This project is designed as a **template for AI-driven Neo4j API development**. The [CLAUDE.md](CLAUDE.md) file provides comprehensive guidance for AI agents working with this codebase.

### For AI Agents (Claude Code, etc.)

When using this project as a blueprint, AI agents should follow this workflow:

**1. Test-First Development**
- Write comprehensive test cases in `PeopleApiTest.kt` (or new test files) before implementation
- Use `xemantic-kotlin-test` DSL (`should`, `have`) for readable, expressive assertions
- Leverage embedded Neo4j (`neo4j-harness`) for instant feedback without external dependencies

**2. Follow Existing Patterns**
- Study test structure: given/when/then, `@AfterEach` cleanup, `peopleApiApp()` setup
- Use `Neo4jOperations` interface for all database operations
- Apply layered architecture: API → Repository → Neo4j

**3. Autonomous Iteration**
- Run `./gradlew test` frequently to verify correctness
- Interpret test failures (Power Assert plugin provides detailed error messages)
- Refine implementation until all tests pass
- Iterate autonomously without human intervention

**4. Code Quality Standards** (from `CLAUDE.md`)
- Use `neo4j.read { }` for queries, `neo4j.write { }` for mutations, `neo4j.flow()` for streaming
- Use parameterized queries with `$$` string interpolation in query strings
- Use `.toObject<T>()` for deserializing Neo4j nodes/relationships to data classes
- Import `.asInstant()` explicitly: `import com.xemantic.neo4j.driver.asInstant`
- Use `@Serializable` data classes for all request/response models
- Maintain consistent error handling (e.g., 404 for missing resources)

**5. Testing Patterns** (from `CLAUDE.md`)
- Use `TestNeo4j.driver()` for embedded Neo4j driver instance
- Create `DispatchedNeo4jOperations` for test setup (e.g., populating test data)
- Clean database between tests: `driver.cleanDatabase()` in `@AfterEach`
- Use `testApplication { }` DSL for HTTP endpoint testing
- Test timestamps with tolerance: `have(createdAt > (now - 10.seconds))`

### For Human Developers

**Using as a starting point:**

1. Fork this project as a template
2. Study the patterns: `SequenceApi.kt` for quick examples, full CRUD in `PeopleApi.kt`
3. Adapt the models and repositories to your domain
4. Use the test infrastructure to validate your implementation
5. (Optional) Let AI help via the vibe-coding workflow above

**Key architectural patterns:**
- **DI Pattern**: Ktor's built-in DI with programmatic providers (`neo4jDriver()`, `neo4jSupport()`, `peopleRepository()`)
- **Repository Pattern**: `PeopleRepository.kt` demonstrates read/write/flow operations
- **HTTP Streaming**: `respondStreaming()` helper converts `Flow<T>` to streaming JSON array responses
- **Dispatcher Control**: `DispatchedNeo4jOperations` wraps driver with custom dispatcher to prevent connection pool exhaustion

**What you get:**
- Production-ready async/non-blocking Neo4j integration
- Comprehensive test coverage with embedded Neo4j
- Clean architecture with DI and resource management
- Ready for AI-assisted feature development
- AI agent guidance via `CLAUDE.md`

## License

**[Apache-2.0](LICENSE)** - Apache License 2.0

**SPDX-License-Identifier:** `Apache-2.0`

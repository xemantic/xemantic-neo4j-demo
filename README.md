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

        // Simple read operation with suspend function
        get("/hello-world") {
            val message = neo4j.read { tx ->
                tx.run(
                    """RETURN "Hello World""""
                ).single()[0].asString()
            }
            call.respond(message)
        }

        // Streaming large result sets with Flow
        get("/sequences/{count}") {
            val count = call.parameters["count"]!!.toInt()
            call.respondTextWriter(
                contentType = ContentType.Text.Plain
            ) {
                flush()
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

TODO it should describe that it depends on ktor and it is a ktor module, which can be started like this:


```kotlin
fun main() {

    val neo4jUri = ""
    val neo4jUser = ""
    val neo4jPassword = ""

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

- `GET /sequences/{count}` - Stream numbers from Neo4j (demonstrates Flow-based streaming)
- `POST /people` - Create a person node
- `GET /people` - List all persons
- `GET /people/{id}` - Get a specific person
- `POST /people/{id}/knows/{otherId}` - Create a KNOWS relationship
- `GET /people/{id}/friends` - Get friends via graph traversal

### Key Implementation Files

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
- **Self-Documenting**: `CLAUDE.md` guides AI behavior and coding standards

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

**For your own Neo4j API:**

1. Fork this project as a starting point
2. Study the patterns: `SequenceApi.kt` for quick examples, full CRUD in `PeopleApi.kt`
3. Adapt the models and repositories to your domain
4. Use the test infrastructure to validate your implementation
5. (Optional) Let AI help via the vibe-coding workflow above

**What you get:**
- Production-ready async/non-blocking Neo4j integration
- Comprehensive test coverage with embedded Neo4j
- Clean architecture with DI and resource management
- Ready for AI-assisted feature development

## License

**[Apache-2.0](LICENSE)** - Apache License 2.0

**SPDX-License-Identifier:** `Apache-2.0`

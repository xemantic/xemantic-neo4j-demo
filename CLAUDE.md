# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Demonstration of the [xemantic-neo4j-kotlin-driver](https://github.com/xemantic/xemantic-neo4j-kotlin-driver) library through a working Neo4j-backed REST API.**

**Primary Purpose:** Demonstrate the [xemantic-neo4j-kotlin-driver](https://github.com/xemantic/xemantic-neo4j-kotlin-driver) library, which provides Kotlin coroutine extensions for the Neo4j Java driver, through practical examples.

**Secondary Purpose:** Serve as a blueprint for vibe-coding custom Neo4j APIs with AI - enabling autonomous AI-driven development where natural language specifications are transformed into working features through Test-Driven Development.

### What "Vibe-Coding" Means Here

Users describe what they want in natural language. You (the AI agent):
1. Write comprehensive test cases that capture their requirements
2. Implement the functionality to satisfy those tests
3. Run tests, interpret failures, refine implementation
4. Iterate until all tests pass

This approach enables rapid, autonomous feature development where natural language specifications are transformed into working code through test-driven development.

### Your Role as an AI Agent

When working in this codebase, you should:
- **Prefer test-first approach**: Always write tests before implementation
- **Use existing patterns**: Study the existing test and implementation patterns
- **Iterate autonomously**: Run `./gradlew test` frequently and interpret results
- **Maintain quality**: Follow the code quality standards defined below
- **Leverage the stack**: Use the test DSL, embedded Neo4j, and type system to your advantage

**License:** Apache-2.0

## Build System

Gradle with Kotlin DSL. Key targets:
- Kotlin 2.2 with language level 2.2
- Java 21 target
- Progressive mode and extra warnings enabled

### Common Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "Neo4jKtorServerTest"

# Run a specific test method
./gradlew test --tests "Neo4jKtorServerTest.should create a person"

# Create fat JAR (uberjar)
./gradlew uberjar

# Check for dependency updates
./gradlew dependencyUpdates

# Clean build
./gradlew clean
```

## Architecture

### Dependency Injection Pattern

The application uses Ktor's built-in DI (`io.ktor:ktor-server-di`) with a programmatic provider approach:

1. **Neo4j Driver Provider** (`neo4jDriver()` in `Neo4j.kt`): Function using `@Property` annotation to inject `Neo4jConfig` from `application.yaml`
2. **Neo4j Support Setup** (`neo4jSupport()` in `Neo4j.kt`): Extension function that:
   - Creates a custom coroutine dispatcher limited to `maxConcurrentSessions` parallelism
   - Provides `Neo4jOperations` by wrapping the driver with `DispatchedNeo4jOperations`
3. **Repository Setup** (`peopleRepository()` in `PeopleRepository.kt`): Extension function that provides the repository implementation
4. **Module Function** (`Application.server()` in `Server.kt`): Main application module that assembles all components

This pattern allows modular composition where each layer (driver, operations, repository, API) is set up independently and registered with the DI container.

### Neo4j Integration

The demo showcases the `xemantic-neo4j-kotlin-driver` library through the `Neo4jOperations` interface:

**Core API Patterns (via `Neo4jOperations`):**
- `neo4j.read { tx -> }`: Read transaction blocks with suspend functions
- `neo4j.write { tx -> }`: Write transaction blocks for data mutations
- `neo4j.flow(query, parameters)`: Returns Kotlin `Flow<Record>` for memory-efficient streaming

**Important:** The library uses `DispatchedNeo4jOperations` which wraps the Neo4j driver with a custom dispatcher to control parallelism and prevent connection pool exhaustion. This handles session management automatically.

**Transaction Configuration:** The library supports configuration objects for timeouts, metadata, and database selection (demonstrated in the code via function parameters).

**Type Conversion:**
- Standard Neo4j value methods: `.asString()`, `.asInt()`, `.asList()` for converting to Kotlin types
- `.toObject<T>()`: Extension function for automatic deserialization to data classes
- `.asInstant()`: Converts Neo4j temporal types to `kotlin.time.Instant` - **MUST be explicitly imported:** `import com.xemantic.neo4j.driver.asInstant`
- `.singleOrNull()`: Returns single result or null if empty

**Temporal Types:** Use Neo4j's `datetime()` function for timestamps (not the legacy `timestamp()` function). The `datetime()` function creates proper temporal types with timezone support and rich functionality. The library's `.asInstant()` method seamlessly converts Neo4j datetime values to `kotlin.time.Instant`.

**Parameterized Queries:** Use `$$` string interpolation for Cypher query parameters (e.g., `$$"MATCH (p:Person {id: $id})"`), then provide actual values in the `parameters` map.

Key files:
- `Neo4j.kt`: Neo4j driver provider, dispatcher setup, and `Neo4jOperations` configuration
- `Server.kt`: Main application entry point assembling all modules
- `sequences/SequenceApi.kt`: Simple examples demonstrating streaming and basic queries
- `people/PeopleRepository.kt`: Repository pattern with read/write/flow operations
- `people/PeopleApi.kt`: REST API endpoints delegating to repository
- `people/Model.kt`: Data classes with Kotlinx serialization for REST requests/responses
- `Neo4jResultHttpStreaming.kt`: Helper for streaming Flow results as JSON arrays over HTTP
- `application.yaml`: Configuration including Neo4j connection details

### Server Structure

Ktor server demonstrating library features through REST endpoints:

**Simple Examples** (`sequences/SequenceApi.kt`):
- `GET /hello-world` - Basic read operation returning a string from Neo4j
- `GET /sequences/{count}` - Streaming demo using `neo4j.flow()` with `respondTextWriter` for incremental response
- Shows memory-efficient processing of large datasets without loading all into memory
- Example: `/sequences/100000` streams 100k records

**CRUD Operations** (`people/PeopleApi.kt` with `people/PeopleRepository.kt`):
- `POST /people` - Create a person node (demonstrates `neo4j.write { }`)
- `GET /people` - List all persons (demonstrates `neo4j.flow()` streaming to HTTP via `respondStreaming()`)
- `GET /people/{id}` - Get a specific person (demonstrates `neo4j.read { }` with `.singleOrNull()`)
- `POST /people/{id}/knows/{otherId}` - Create a KNOWS relationship (demonstrates graph mutations)
- `GET /people/{id}/friends` - Get friends via graph traversal (demonstrates parameterized queries and streaming)

**Layered Architecture:**
- API layer (`PeopleApi.kt`) handles HTTP concerns and delegates to repository
- Repository layer (`PeopleRepository.kt`) handles Neo4j queries using `Neo4jOperations`
- `Neo4jOperations` automatically manages sessions and connection pooling via `DispatchedNeo4jOperations`

**HTTP Streaming Helper:**
The `respondStreaming()` extension function (`Neo4jResultHttpStreaming.kt`) converts a `Flow<T>` into a streaming JSON array HTTP response:
- Uses `respondTextWriter` for incremental response writing
- Serializes each item and flushes immediately for true streaming
- Properly formats JSON array with commas between elements

### Testing

Tests use:
- `ktor-server-test-host`: Ktor's testing framework with `testApplication { }` DSL
- `neo4j-harness`: Embedded Neo4j instance (managed by `TestNeo4j` singleton in `TestNeo4j.kt`)
- `xemantic-kotlin-test`: Enhanced assertion library with `should` and `have` DSL for fluent, readable assertions
- Power Assert plugin configured for `com.xemantic.kotlin.test.assert` and `com.xemantic.kotlin.test.have`

**Test Infrastructure:**
- `TestNeo4j` object provides embedded Neo4j instance with `.driver()` method
- `Driver.cleanDatabase()` extension function for cleanup between tests
- `Application.testNeo4jDriver()` extension registers test driver with DI container
- Tests create `DispatchedNeo4jOperations` for test setup (e.g., `neo4j.populate()`)

**Test Structure:**
- Given/when/then pattern for clarity
- `@AfterEach` cleans database between tests for isolation
- `peopleApiApp()` helper sets up full application stack for HTTP endpoint testing
- Uses `kotlin.time.Clock.System.now()` for timestamp assertions with tolerance (e.g., `> (now - 10.seconds)`)

## Configuration Files

- `application.yaml`: Development configuration (connection details, port 8080, auto-reload)
- `application-deployment.yaml`: Likely production configuration (not tracked in git)
- `logback.xml` and `logback-test.xml`: Logging configuration in `src/main/resources` and `src/test/resources`

## Dependency Management

Uses Gradle version catalog (`gradle/libs.versions.toml`). The `dependencyUpdates` task is configured to reject unstable versions (alpha, beta, rc).

## Vibe-Coding Workflow (AI-Assisted Development)

This project is architected specifically for fully autonomous AI development. When users provide natural language specifications, follow this workflow:

1. **Write tests first** - Create comprehensive test cases in `PeopleApiTest.kt` (or new test file) that define the expected behavior
2. **Use the test DSL** - Leverage `xemantic-kotlin-test` DSL (`should`, `have`) for readable, expressive assertions
3. **Follow existing patterns** - Study existing tests for structure: given/when/then, embedded Neo4j setup, client configuration
4. **Run tests** - Execute `./gradlew test` to verify the tests fail appropriately (red phase)
5. **Implement functionality** - Following the layered architecture:
   - Create/update data classes in `Model.kt` with `@Serializable` annotation
   - Add repository methods in `PeopleRepository.kt` using `Neo4jOperations`
   - Add API endpoints in `PeopleApi.kt` delegating to repository
   - Register new components with DI if needed (following `peopleRepository()` pattern)
6. **Iterate** - Run tests again and refine implementation until all tests pass (green phase)
7. **Refactor** - Improve code quality while maintaining test coverage

### Test-First Benefits for AI Agents

- **Clear specifications**: Tests define exact expected behavior, reducing ambiguity
- **Instant feedback**: Embedded Neo4j and Ktor test host provide immediate validation
- **Error clarity**: Power Assert plugin shows detailed assertion failures
- **Autonomous iteration**: AI agents can run tests and fix issues without human intervention
- **Regression safety**: Existing tests ensure new changes don't break functionality

### Autonomous Development Workflow

AI agents follow this iterative cycle:

1. Read natural language requirements
2. Create test cases that capture those requirements
3. Implement features to satisfy tests
4. Run `./gradlew test` to verify correctness
5. Iterate on implementation based on test results
6. Refactor while maintaining green tests

This workflow enables continuous improvement and rapid feature delivery.

### Code Quality Standards

When implementing features:
- Use `Neo4jOperations` interface (injected via DI) for all database operations
- Use `neo4j.read { }` for queries, `neo4j.write { }` for mutations, `neo4j.flow()` for streaming
- Use parameterized queries with `$$` string interpolation in query strings, then pass actual values in `parameters` map
- Use `.toObject<T>()` for deserializing Neo4j nodes/relationships to data classes
- Import `.asInstant()` explicitly when working with temporal types: `import com.xemantic.neo4j.driver.asInstant`
- Follow Kotlin idioms: data classes, extension functions, coroutines
- Maintain consistent error handling (e.g., 404 for missing resources)
- Add appropriate HTTP status codes (200 OK, 201 Created, 404 Not Found)
- Use `@Serializable` data classes for all request/response models

### Testing Patterns

The test suite demonstrates patterns AI agents should follow:
- `@AfterEach` for test isolation (clean database between tests using `driver.cleanDatabase()`)
- Use `TestNeo4j.driver()` for obtaining embedded Neo4j driver instance
- Create `DispatchedNeo4jOperations` for test setup operations (e.g., populating test data)
- Use `testApplication { }` DSL for HTTP endpoint testing
- Create `peopleApiApp()` helper function to set up full application with all layers
- Given/when/then structure for clarity
- `xemantic-kotlin-test` DSL for fluent assertions: `response should { have(status == HttpStatusCode.OK) }`
- Use `body<T>()` to deserialize HTTP response bodies for assertions
- Test timestamps with tolerance: `have(createdAt > (now - 10.seconds))`
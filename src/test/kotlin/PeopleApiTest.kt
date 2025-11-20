/*
 * Copyright 2025 Kazimierz Pogoda / Xemantic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xemantic.neo4j.demo

import com.xemantic.kotlin.test.coroutines.should
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.sameAsJson
import com.xemantic.neo4j.demo.people.CreatePersonRequest
import com.xemantic.neo4j.demo.people.Person
import com.xemantic.neo4j.demo.people.Relationship
import com.xemantic.neo4j.demo.people.peopleApi
import com.xemantic.neo4j.demo.people.peopleRepository
import com.xemantic.neo4j.driver.Neo4jOperations
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.testing.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class PeopleApiTest {

    @AfterEach
    fun cleanDatabase() {
        TestNeo4j.cleanDatabase()
    }

    // first we assemble the environment and modules defining our app in test
    fun ApplicationTestBuilder.peopleApiApp() {
        application {
            serverContentNegotiation()
            dependencies.provide<Neo4jOperations> {
                TestNeo4j.operations
            }
            peopleRepository()
            peopleApi()
        }
        client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
    }

    @Test
    fun `should get a person and return JSON`() = testApplication {
    // given
    peopleApiApp()

    TestNeo4j.populate("""
        CREATE (p:Person {
          id: 'alice',
          name: 'Alice',
          age: 30,
          createdAt: datetime('2025-01-15T10:30:00Z'),
          updatedAt: datetime('2025-01-15T10:30:00Z')
        })
    """.trimIndent())

    // when
    val response = client.get("/people/alice")

    // then
    response should {
        have(status == HttpStatusCode.OK)
        have(contentType() == ContentType.Application.Json.withCharset(Charsets.UTF_8))
        bodyAsText() sameAsJson """
            {
              "id": "alice",
              "name": "Alice",
              "age": 30,
              "createdAt": "2025-01-15T10:30:00Z",
              "updatedAt": "2025-01-15T10:30:00Z"
            }
        """.trimIndent()
    }
}

    @Test
    fun `should get a person and deserialize to Person instance`() = testApplication {
        // given
        peopleApiApp()

        TestNeo4j.populate("""
            CREATE (p:Person {
              id: 'alice',
              name: 'Alice',
              age: 30,
              createdAt: datetime('2025-01-15T10:30:00Z'),
              updatedAt: datetime('2025-01-15T10:30:00Z')
            })
        """.trimIndent())

        // when
        val response = client.get("/people/alice")

        // then
        response should {
            have(status == HttpStatusCode.OK)
            have(contentType() == ContentType.Application.Json.withCharset(Charsets.UTF_8))
            body<Person>() should {
                have(id == "alice")
                have(name == "Alice")
                have(age == 30)
                have(createdAt == Instant.parse("2025-01-15T10:30:00Z"))
                have(updatedAt == Instant.parse("2025-01-15T10:30:00Z"))
            }
        }
    }

    @Test
    fun `should list all people`() = testApplication {
        // given
        peopleApiApp()

        TestNeo4j.populate("""
            CREATE (p1:Person {
              id: 'alice',
              name: 'Alice',
              age: 30,
              createdAt: datetime('2025-01-15T10:30:00Z'),
              updatedAt: datetime('2025-01-15T10:30:00Z')
            })
            CREATE (p2:Person {
              id: 'bob',
              name: 'Bob',
              age: 25,
              createdAt: datetime('2025-01-15T11:00:00Z'),
              updatedAt: datetime('2025-01-15T11:00:00Z')
            })
        """.trimIndent())

        // when
        val response = client.get("/people")

        // then
        response should {
            have(status == HttpStatusCode.OK)
            have(contentType() == ContentType.Application.Json.withCharset(Charsets.UTF_8))
            bodyAsText() sameAsJson """
                [
                  {
                    "id": "alice",
                    "name": "Alice",
                    "age": 30,
                    "createdAt": "2025-01-15T10:30:00Z",
                    "updatedAt": "2025-01-15T10:30:00Z"
                  },
                  {
                    "id": "bob",
                    "name": "Bob",
                    "age": 25,
                    "createdAt": "2025-01-15T11:00:00Z",
                    "updatedAt": "2025-01-15T11:00:00Z"
                  }
                ]
            """.trimIndent()
        }
    }

    @Test
    fun `should return 404 for non-existent person`() = testApplication {
        // given
        peopleApiApp()

        // when
        val response = client.get("/people/nonexistent")

        // then
        response should {
            have(status == HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `should create a KNOWS relationship and return JSON`() = testApplication {
        // given
        peopleApiApp()

        TestNeo4j.populate("""
            CREATE (p1:Person {
              id: 'alice',
              name: 'Alice',
              age: 30,
              createdAt: datetime('2025-01-15T10:30:00Z'),
              updatedAt: datetime('2025-01-15T10:30:00Z')
            })
            CREATE (p2:Person {
              id: 'bob',
              name: 'Bob',
              age: 25,
              createdAt: datetime('2025-01-15T11:00:00Z'),
              updatedAt: datetime('2025-01-15T11:00:00Z')
            })
        """.trimIndent())

        // when
        val response = client.post("/people/alice/knows/bob")

        // then
        response should {
            have(status == HttpStatusCode.Created)
            have(contentType() == ContentType.Application.Json.withCharset(Charsets.UTF_8))
            val relationship = body<Relationship>()
            bodyAsText() sameAsJson """
                {
                  "type": "KNOWS",
                  "from": "alice",
                  "to": "bob",
                  "createdAt": "${relationship.createdAt}"
                }
            """.trimIndent()
        }
    }

    @Test
    fun `should create a KNOWS relationship and deserialize to Relationship instance`() = testApplication {
        // given
        peopleApiApp()

        TestNeo4j.populate("""
            CREATE (p1:Person {
              id: 'alice',
              name: 'Alice',
              age: 30,
              createdAt: datetime('2025-01-15T10:30:00Z'),
              updatedAt: datetime('2025-01-15T10:30:00Z')
            })
            CREATE (p2:Person {
              id: 'bob',
              name: 'Bob',
              age: 25,
              createdAt: datetime('2025-01-15T11:00:00Z'),
              updatedAt: datetime('2025-01-15T11:00:00Z')
            })
        """.trimIndent())

        // when
        val response = client.post("/people/alice/knows/bob")

        // then
        response should {
            have(status == HttpStatusCode.Created)
            have(contentType() == ContentType.Application.Json.withCharset(Charsets.UTF_8))
            val relationship = body<Relationship>()
            bodyAsText() sameAsJson """
                {
                  "type": "KNOWS",
                  "from": "alice",
                  "to": "bob",
                  "createdAt": "${relationship.createdAt}"
                }
            """.trimIndent()
        }
    }

    @Test
    fun `should get friends of a person`() = testApplication {
        // given
        peopleApiApp()

        TestNeo4j.populate("""
            CREATE (p1:Person {
              id: 'alice',
              name: 'Alice',
              age: 30,
              createdAt: datetime('2025-01-15T10:30:00Z'),
              updatedAt: datetime('2025-01-15T10:30:00Z')
            })
            CREATE (p2:Person {
              id: 'bob',
              name: 'Bob',
              age: 25,
              createdAt: datetime('2025-01-15T11:00:00Z'),
              updatedAt: datetime('2025-01-15T11:00:00Z')
            })
            CREATE (p3:Person {
              id: 'charlie',
              name: 'Charlie',
              age: 35,
              createdAt: datetime('2025-01-15T11:30:00Z'),
              updatedAt: datetime('2025-01-15T11:30:00Z')
            })
            CREATE (p1)-[:KNOWS]->(p2)
            CREATE (p1)-[:KNOWS]->(p3)
        """.trimIndent())

        // when
        val response = client.get("/people/alice/friends")

        // then
        response should {
            have(status == HttpStatusCode.OK)
            have(contentType() == ContentType.Application.Json.withCharset(Charsets.UTF_8))
            bodyAsText() sameAsJson """
                [
                  {
                    "id": "bob",
                    "name": "Bob",
                    "age": 25,
                    "createdAt": "2025-01-15T11:00:00Z",
                    "updatedAt": "2025-01-15T11:00:00Z"
                  },
                  {
                    "id": "charlie",
                    "name": "Charlie",
                    "age": 35,
                    "createdAt": "2025-01-15T11:30:00Z",
                    "updatedAt": "2025-01-15T11:30:00Z"
                  }
                ]
            """.trimIndent()
        }
    }

    @Test
    fun `should create a person and return JSON`() = testApplication {
        // given
        peopleApiApp()

        // when
        val response = client.post("/people") {
            contentType(ContentType.Application.Json)
            setBody(CreatePersonRequest(
                id = "alice",
                name = "Alice",
                age = 30
            ))
        }

        // then
        response should {
            have(status == HttpStatusCode.Created)
            have(contentType() == ContentType.Application.Json.withCharset(Charsets.UTF_8))
            val person = body<Person>()
            bodyAsText() sameAsJson """
                {
                  "id": "alice",
                  "name": "Alice",
                  "age": 30,
                  "createdAt": "${person.createdAt}",
                  "updatedAt": "${person.updatedAt}"
                }
            """.trimIndent()
        }
    }

    @Test
    fun `should create a person without age and return JSON`() = testApplication {
        // given
        peopleApiApp()

        // when
        val response = client.post("/people") {
            contentType(ContentType.Application.Json)
            setBody(CreatePersonRequest(
                id = "bob",
                name = "Bob"
            ))
        }

        // then
        response should {
            have(status == HttpStatusCode.Created)
            have(contentType() == ContentType.Application.Json.withCharset(Charsets.UTF_8))
            val person = body<Person>()
            bodyAsText() sameAsJson """
                {
                  "id": "bob",
                  "name": "Bob",
                  "createdAt": "${person.createdAt}",
                  "updatedAt": "${person.updatedAt}"
                }
            """.trimIndent()
        }
    }

    @Test
    fun `should fail when creating person with duplicate ID`() = testApplication {
        // given
        peopleApiApp()

        TestNeo4j.populate("""
            CREATE (p:Person {
              id: 'alice',
              name: 'Alice',
              age: 30,
              createdAt: datetime('2025-01-15T10:30:00Z'),
              updatedAt: datetime('2025-01-15T10:30:00Z')
            })
        """.trimIndent())

        // when
        val response = client.post("/people") {
            contentType(ContentType.Application.Json)
            setBody(CreatePersonRequest(
                id = "alice",
                name = "Another Alice",
                age = 25
            ))
        }

        // then
        response should {
            have(status == HttpStatusCode.Conflict)
            have(contentType() == ContentType.Application.Json.withCharset(Charsets.UTF_8))
            bodyAsText() sameAsJson """
                {
                  "error": "Person with this ID already exists"
                }
            """.trimIndent()
        }
    }

    @Test
    fun `should fail when creating relationship with non-existent person`() = testApplication {
        // given
        peopleApiApp()

        TestNeo4j.populate("""
            CREATE (p:Person {
              id: 'alice',
              name: 'Alice',
              age: 30,
              createdAt: datetime('2025-01-15T10:30:00Z'),
              updatedAt: datetime('2025-01-15T10:30:00Z')
            })
        """.trimIndent())

        // when
        val response = client.post("/people/alice/knows/nonexistent")

        // then
        response should {
            have(status == HttpStatusCode.NotFound)
            have(contentType() == ContentType.Application.Json.withCharset(Charsets.UTF_8))
            bodyAsText() sameAsJson """
                {
                  "error": "One or both persons not found"
                }
            """.trimIndent()
        }
    }

}

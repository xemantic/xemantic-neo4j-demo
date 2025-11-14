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
import com.xemantic.neo4j.demo.people.Person
import com.xemantic.neo4j.demo.people.Relationship
import com.xemantic.neo4j.demo.people.peopleApi
import com.xemantic.neo4j.demo.people.peopleRepository
import com.xemantic.neo4j.driver.DispatchedNeo4jOperations
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.*
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class PeopleApiTest {

    private val driver = TestNeo4j.driver()

    private val neo4j = DispatchedNeo4jOperations(
        driver = driver,
        dispatcher = Dispatchers.IO.limitedParallelism(90)
    )

    @AfterEach
    fun cleanDatabase() {
        driver.cleanDatabase()
    }

    // first we assemble the environment and modules defining our app in test
    fun ApplicationTestBuilder.peopleApiApp() {
        environment {
            config = MapApplicationConfig(
                "neo4j.maxConcurrentSessions" to "50"
            )
        }
        application {
            install(ContentNegotiation) {
                json()
            }
            testNeo4jDriver()
            neo4jSupport()
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
    fun `should create a person`() = testApplication {
        // given
        peopleApiApp()
        val now = Clock.System.now()

        // when
        val response = client.post("/people") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "id": "alice",
                    "name": "Alice",
                    "age": 30
                }
            """.trimIndent())
        }

        // then
        response should {
            have(status == HttpStatusCode.Created)
            body<Person>() should {
                have(id == "alice")
                have(name == "Alice")
                have(age == 30)
                have(createdAt > (now - 10.seconds))
                have(updatedAt > (now - 10.seconds))
            }
        }
    }

    @Test
    fun `should list all people`() = testApplication {
        // given
        peopleApiApp()
        val now = Clock.System.now()

        neo4j.populate("""
            CREATE (p1:Person {id: 'alice', name: 'Alice', age: 30, createdAt: datetime(), updatedAt: datetime()})
            CREATE (p2:Person {id: 'bob', name: 'Bob', age: 25, createdAt: datetime(), updatedAt: datetime()})
        """.trimIndent())

        // when
        val response = client.get("/people")

        // then
        response should {
            have(status == HttpStatusCode.OK)
            body<List<Person>>() should {
                have(size == 2)
                get(0) should {
                    have(id == "alice")
                    have(name == "Alice")
                    have(age == 30)
                    have(createdAt > (now - 10.seconds))
                    have(updatedAt > (now - 10.seconds))
                }
                get(1) should {
                    have(id == "bob")
                    have(name == "Bob")
                    have(age == 25)
                    have(createdAt > (now - 10.seconds))
                    have(updatedAt > (now - 10.seconds))
                }
            }
        }
    }

    @Test
    fun `should get a specific person`() = testApplication {
        // given
        peopleApiApp()
        val now = Clock.System.now()

        neo4j.populate("""
            CREATE (p1:Person {id: 'alice', name: 'Alice', age: 30, createdAt: datetime(), updatedAt: datetime()})
        """.trimIndent())

        // when
        val response = client.get("/people/alice")

        // then
        response should {
            have(status == HttpStatusCode.OK)
            body<Person>() should {
                have(id == "alice")
                have(name == "Alice")
                have(age == 30)
                have(createdAt > (now - 10.seconds))
                have(updatedAt > (now - 10.seconds))
            }
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
    fun `should create a KNOWS relationship between persons`() = testApplication {
        // given
        peopleApiApp()
        val now = Clock.System.now()

        neo4j.populate("""
            CREATE (p1:Person {id: 'alice', name: 'Alice', age: 30, createdAt: datetime(), updatedAt: datetime()})
            CREATE (p2:Person {id: 'bob', name: 'Bob', age: 25, createdAt: datetime(), updatedAt: datetime()})
        """.trimIndent())

        // when
        val response = client.post("/people/alice/knows/bob")

        // then
        response should {
            have(status == HttpStatusCode.Created)
            body<Relationship>() should {
                have(type == "KNOWS")
                have(from == "alice")
                have(to == "bob")
                have(createdAt > (now - 10.seconds))
            }
        }
    }

    @Test
    fun `should get friends of a person`() = testApplication {
        // given
        val now = Clock.System.now()
        peopleApiApp()

        neo4j.populate("""
            CREATE (p1:Person {id: 'alice', name: 'Alice', age: 30, createdAt: datetime(), updatedAt: datetime()})
            CREATE (p2:Person {id: 'bob', name: 'Bob', age: 25, createdAt: datetime(), updatedAt: datetime()})
            CREATE (p3:Person {id: 'charlie', name: 'Charlie', age: 35, createdAt: datetime(), updatedAt: datetime()})
            CREATE (p1)-[:KNOWS]->(p2)
            CREATE (p1)-[:KNOWS]->(p3)
        """.trimIndent())

        // when
        val response = client.get("/people/alice/friends")

        // then
        response should {
            have(status == HttpStatusCode.OK)
            body<List<Person>>() should {
                have(size == 2)
                get(0) should {
                    have(id == "bob")
                    have(name == "Bob")
                    have(age == 25)
                    have(createdAt > (now - 10.seconds))
                    have(updatedAt > (now - 10.seconds))
                }
                get(1) should {
                    have(id == "charlie")
                    have(name == "Charlie")
                    have(age == 35)
                    have(createdAt > (now - 10.seconds))
                    have(updatedAt > (now - 10.seconds))
                }
            }
        }
    }

}

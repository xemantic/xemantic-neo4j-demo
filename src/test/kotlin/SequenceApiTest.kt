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
import com.xemantic.kotlin.test.sameAs
import com.xemantic.neo4j.demo.sequences.sequenceApi
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.*
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SequenceApiTest {

    // first we assemble the environment and modules defining our app in test
    fun ApplicationTestBuilder.sequenceApiApp() {
        environment {
            config = MapApplicationConfig(
                "neo4j.maxConcurrentSessions" to "90"
            )
        }
        application {
            install(ContentNegotiation) {
                json()
            }
            testNeo4jDriver()
            neo4jSupport()
            sequenceApi()
        }
        client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
    }

    @Test
    fun `should receive hello message`() = testApplication {
        // given
        sequenceApiApp()

        // when
        val response = client.get("/hello-world")

        // then
        response should {
            have(status == HttpStatusCode.OK)
            have(contentType() == ContentType.Text.Plain.withCharset(Charsets.UTF_8))
            body<String>() sameAs "Hello World"
        }
    }

    @Test
    fun `should stream sequence 1 from neo4j`() = testApplication {
        // given
        sequenceApiApp()

        // when
        val response = client.get("/sequences/1")

        // then
        response should {
            have(status == HttpStatusCode.OK)
            body<String>() sameAs """
                1
            """.trimIndent()
        }
    }

    @Test
    fun `should stream sequence 10 from neo4j`() = testApplication {
        // given
        sequenceApiApp()

        // when
        val response = client.get("/sequences/10")

        // then
        response should {
            have(status == HttpStatusCode.OK)
            body<String>() sameAs """
                1
                2
                3
                4
                5
                6
                7
                8
                9
                10
            """.trimIndent()
        }
    }

    @Test
    fun `should stream sequence 100000 from neo4j and receive it as a stream of lines`() = testApplication {
        // given
        sequenceApiApp()

        // when
        val response = client.prepareGet("/sequences/100000").execute { response ->
            val channel = response.bodyAsChannel()
            var last: String? = null
            while (!channel.isClosedForRead) {
                last = channel.readUTF8Line()
            }
            last
        }

        // then
        response should {
            have(response == "100000")
        }
    }

    @Test
    fun `should get current date from database using asInstant`() = testApplication {
        // given
        sequenceApiApp()
        val now = Clock.System.now()

        // when
        val response = client.get("/current-date")

        // then
        response should {
            have(status == HttpStatusCode.OK)
            have(contentType()?.contentType == ContentType.Application.Json.contentType)
            body<Map<String, Instant>>() should {
                val instant = get("currentDate")
                have(instant != null)
                have(instant!! > (now - 10.seconds))
                have(instant < (now + 10.seconds))
            }
        }
    }

    @Test
    fun `should handle 150 concurrent client requests without exhausting connection pool`() = testApplication {
        // given
        sequenceApiApp()
        val sequenceCount = 10000
        val numberOfRequests = 150

        // when
        val responses = coroutineScope {
            (1..numberOfRequests).map { index ->
                async(Dispatchers.IO) {
                    // println("Request started: $index")
                    client.prepareGet("/sequences/$sequenceCount").execute { response ->
                        val channel = response.bodyAsChannel()
                        var last: String? = null
                        while (!channel.isClosedForRead) {
                            last = channel.readUTF8Line()
                        }
                        // println("Request finished: $index")
                        last
                    }
                }
            }.awaitAll()
        }

        // then
        responses.forEach { response ->
            assert(response == "10000")
        }
    }

}

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
import com.xemantic.neo4j.driver.Neo4jOperations
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class HealthCheckApiTest {

    fun ApplicationTestBuilder.healthCheckApp() {
        application {
            serverContentNegotiation()
            dependencies.provide<Neo4jOperations> {
                TestNeo4j.operations
            }
            healthCheckApi()
        }
        client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
    }

    @Test
    fun `should return healthy status with timestamp when database is accessible`() = testApplication {
        // given
        healthCheckApp()
        val now = Clock.System.now()

        // when
        val response = client.get("/health")

        // then
        response should {
            have(status == HttpStatusCode.OK)
            have(contentType() == ContentType.Application.Json.withCharset(Charsets.UTF_8))
            body<Map<String, String>>() should {
                have(get("status") == "healthy")
                val timestampString = get("timestamp")
                have(timestampString != null)
                val timestamp = Instant.parse(timestampString!!)
                have(timestamp > (now - 30.seconds))
                have(timestamp < (now + 30.seconds))
            }
        }
    }

}
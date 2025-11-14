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

import com.xemantic.neo4j.driver.Neo4jOperations
import com.xemantic.neo4j.driver.asInstant
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.healthCheckApi() {

    val neo4j: Neo4jOperations by dependencies

    routing {

        get("/health") {
            try {
                val (isHealthy, timestamp) = neo4j.read { tx ->
                    val record = tx.run("RETURN 1 AS check, datetime() AS timestamp").single()
                    val isHealthy = record["check"].asInt() == 1
                    val timestamp = record["timestamp"].asInstant()
                    isHealthy to timestamp
                }
                if (isHealthy) {
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "status" to "healthy",
                            "timestamp" to timestamp.toString()
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        mapOf("status" to "unhealthy")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    mapOf(
                        "status" to "unhealthy",
                        "error" to (e.message ?: "Unknown error")
                    )
                )
            }
        }

    }

}
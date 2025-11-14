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

import com.xemantic.neo4j.driver.DispatchedNeo4jOperations
import com.xemantic.neo4j.driver.Neo4jOperations
import io.ktor.server.application.Application
import io.ktor.server.config.property
import io.ktor.server.plugins.di.annotations.Property
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase

/**
 * Maps properties from `application.yaml`.
 */
@Serializable
data class Neo4jConfig(
    val uri: String,
    val user: String,
    val password: String,
    val maxConcurrentSessions: Int
)

fun neo4jDriver(
    @Property("neo4j") config: Neo4jConfig
): Driver = GraphDatabase.driver(
    config.uri,
    AuthTokens.basic(config.user, config.password)
).apply {
    verifyConnectivity()
}

fun Application.neo4jSupport() {
    val maxConcurrentSessions = property<Int>("neo4j.maxConcurrentSessions")
    dependencies.key<CoroutineDispatcher>("neo4j") {
        provide {
            Dispatchers.IO.limitedParallelism(
                parallelism = maxConcurrentSessions,
                name = "neo4j"
            )
        }
    }
    dependencies.provide<Neo4jOperations> {
        DispatchedNeo4jOperations(
            driver = resolve<Driver>(),
            dispatcher = resolve<CoroutineDispatcher>("neo4j")
        )
    }
}

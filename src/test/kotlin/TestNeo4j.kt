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
import org.intellij.lang.annotations.Language
import org.neo4j.configuration.connectors.BoltConnector
import org.neo4j.configuration.connectors.HttpConnector
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.neo4j.harness.Neo4j
import org.neo4j.harness.internal.InProcessNeo4jBuilder

object TestNeo4j {

    init {
        initializeLogging()
    }

    private val db: Neo4j by lazy {
        InProcessNeo4jBuilder()
            .withDisabledServer()
            .withConfig(HttpConnector.enabled, false)
            .withConfig(BoltConnector.enabled, true)
            .build()
    }

    private val config: Neo4jConfig by lazy {
        Neo4jConfig(
            uri = db.boltURI().toString(),
            user = "",
            password = "",
            maxConcurrentSessions = 90
        )
    }

    private val driver: Driver by lazy {
        GraphDatabase.driver(
            config.uri,
            AuthTokens.none()
        ).apply {
            applyMigrations(
                driver = this
            )
        }
    }

    val operations: Neo4jOperations by lazy {
        neo4jOperations(
            driver = driver,
            config = config
        )
    }

    suspend fun populate(
        @Language("cypher") query: String
    ) {
        operations.populate(query)
    }

    fun cleanDatabase() {
        driver.executableQuery(
            "MATCH (n) DETACH DELETE n"
        ).execute()
    }

}

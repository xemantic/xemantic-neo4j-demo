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

import ac.simons.neo4j.migrations.core.Migrations
import ac.simons.neo4j.migrations.core.MigrationsConfig
import com.xemantic.neo4j.driver.DispatchedNeo4jOperations
import com.xemantic.neo4j.driver.Neo4jOperations
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
    config: Neo4jConfig
): Driver = GraphDatabase.driver(
    config.uri,
    AuthTokens.basic(config.user, config.password)
).apply {
    verifyConnectivity()
    // Apply migrations after connectivity is verified but before driver is used
    applyMigrations(driver = this)
}

fun neo4jOperations(
    driver: Driver,
    config: Neo4jConfig
): Neo4jOperations = DispatchedNeo4jOperations(
    driver = driver,
    dispatcher = Dispatchers.IO.limitedParallelism(
        parallelism = config.maxConcurrentSessions,
        name = "neo4j"
    )
)

/**
 * Applies Neo4j migrations.
 *
 * Migration location: `classpath:neo4j/migrations
 *
 * If any migration fails, an exception is thrown and the application will not start.
 *
 * @param driver The Neo4j driver with verified connectivity
 * @throws Exception if migrations fail
 */
fun applyMigrations(
    driver: Driver,
) {

    logger.info { "Applying migrations..." }

    val appMigrationsConfig = MigrationsConfig.builder()
        .withLocationsToScan("classpath:neo4j/migrations")
        .withTransactionMode(MigrationsConfig.TransactionMode.PER_STATEMENT)
        .build()

    val appMigrations = Migrations(appMigrationsConfig, driver)

    try {
        appMigrations.apply()
        logger.info { "Migrations applied" }
    } catch (e: Exception) {
        logger.error(e) { "Failed to apply application migrations" }
        throw e
    }

    logger.info { "All migrations applied successfully" }

}

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

import com.xemantic.neo4j.demo.people.peopleApi
import com.xemantic.neo4j.demo.people.peopleRepository
import com.xemantic.neo4j.demo.sequences.sequenceApi
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.config.property
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.di.dependencies
import kotlinx.serialization.json.Json
import org.neo4j.driver.Driver
import org.slf4j.bridge.SLF4JBridgeHandler

// NOTE if this file is renamed, the build.gradle.kts Main-Class attribute has to be adjusted
fun main(args: Array<String>) {
    initializeLogging()
    EngineMain.main(args)
}

fun Application.server() {

    serverContentNegotiation()

    val config = property<Neo4jConfig>("neo4j")

    dependencies {
        provide {
            neo4jDriver(config)
        }
        provide {
            neo4jOperations(
                driver = resolve<Driver>(),
                config = config
            )
        }
    }
    peopleRepository()
    peopleApi()
    sequenceApi()
    healthCheckApi()
}

fun Application.serverContentNegotiation() {
    // we need to return objects as JSON
    install(ContentNegotiation) {
        json(Json {
            explicitNulls = false
        })
    }
}

val logger = KotlinLogging.logger {}

/**
 * Initializes Java Util Logging to slf4j bridge.
 * This function should be called once, as early as possible during application startup.
 *
 * Note: Neo4j is using JUL internally.
 */
fun initializeLogging() {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
}

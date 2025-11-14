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
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.config.property
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.di.dependencies
import org.neo4j.driver.Driver

// NOTE if this file is renamed, the build.gradle.kts Main-Class attribute has to be adjusted
fun main(args: Array<String>) = EngineMain.main(args)

fun Application.server() {

    // we need to return objects as JSON
    install(ContentNegotiation) {
        json()
    }

    val config = property<Neo4jConfig>("neo4j")

    dependencies.provide<Driver> {
        neo4jDriver(config)
    }

    neo4jSupport()
    peopleRepository()
    peopleApi()
    sequenceApi()
    healthCheckApi()
}

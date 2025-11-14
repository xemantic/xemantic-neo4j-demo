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

package com.xemantic.neo4j.demo.sequences

import com.xemantic.neo4j.driver.DispatchedNeo4jOperations
import com.xemantic.neo4j.driver.Neo4jOperations
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.GraphDatabase
import kotlin.text.toInt

// an example how assemble everything together in a single file
fun Application.sequenceApi() {

    val neo4j: Neo4jOperations by dependencies

    routing {

        // Streaming large result sets with Flow
        get("/sequences/{count}") {
            val count = call.parameters["count"]!!.toInt()
            call.respondTextWriter(
                contentType = ContentType.Text.Plain
            ) {
                neo4j.flow(
                    query = $$"UNWIND range(1, $count) AS n RETURN n",
                    parameters = mapOf("count" to count)
                ).collect {
                    val n = it["n"].asInt()
                    write("$n")
                    if (n < count) write("\n")
                    flush()
                }
            }
        }

    }

}

fun main() {

    val neo4jUri = ""
    val neo4jUser = ""
    val neo4jPassword = ""

    val driver = GraphDatabase.driver(
        neo4jUri,
        AuthTokens.basic(neo4jUser, neo4jPassword)
    ).apply {
        verifyConnectivity()
    }

    // this should be no greater than driver's max session limit witch is 100 by default
    val dispatcher = Dispatchers.IO.limitedParallelism(90)

    val neo4jOperations = DispatchedNeo4jOperations(
        driver, dispatcher
    )

    embeddedServer(Netty, port = 8080) {
        dependencies.provide<Neo4jOperations> { neo4jOperations }
        sequenceApi()
    }.start(wait = true)

}

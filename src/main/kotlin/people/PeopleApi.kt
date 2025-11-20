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

package com.xemantic.neo4j.demo.people

import com.xemantic.neo4j.demo.respondStreaming
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.plugins.di.dependencies
import org.neo4j.driver.exceptions.ClientException
import org.neo4j.driver.exceptions.NoSuchRecordException

/**
 * The main application module.
 */
fun Application.peopleApi() {

    val peopleRepository: PeopleRepository by dependencies

    routing {

        // Create a person node
        post("/people") {
            val request = call.receive<CreatePersonRequest>()
            try {
                val person = peopleRepository.save(request)
                call.respond(HttpStatusCode.Created, person)
            } catch (e: ClientException) {
                // Check if it's a constraint violation (duplicate ID)
                when (e.code()) {
                    "Neo.ClientError.Schema.ConstraintValidationFailed",
                    "Neo.ClientError.Schema.ConstraintViolation" -> {
                        call.respond(
                            HttpStatusCode.Conflict,
                            mapOf("error" to "Person with this ID already exists")
                        )
                    }
                    else -> throw e
                }
            }
        }

        // List all persons
        get("/people") {
            respondStreaming(
                peopleRepository.list()
            )
        }

        // Get a specific person
        get("/people/{id}") {
            val id = call.parameters["id"]!!
            val person = peopleRepository.load(id)
            if (person != null) {
                call.respond(person)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Person not found")
                )
            }
        }

        // Create a KNOWS relationship between two persons
        post("/people/{id}/knows/{otherId}") {
            val id = call.parameters["id"]!!
            val otherId = call.parameters["otherId"]!!
            try {
                val relationship = peopleRepository.saveKnows(id, otherId)
                call.respond(HttpStatusCode.Created, relationship)
            } catch (e: NoSuchRecordException) {
                // One or both persons don't exist
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "One or both persons not found")
                )
            }
        }

        // Get friends (persons that this person knows)
        get("/people/{id}/friends") {
            val id = call.parameters["id"]!!
            respondStreaming(
                peopleRepository.listFriends(
                    personId = id
                )
            )
        }

    }

}

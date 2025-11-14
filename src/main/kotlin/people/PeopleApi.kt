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

/**
 * The main application module.
 */
fun Application.peopleApi() {

    val peopleRepository: PeopleRepository by dependencies

    routing {

        // Create a person node
        post("/people") {
            val request = call.receive<CreatePersonRequest>()
            val person = peopleRepository.save(request)
            call.respond(HttpStatusCode.Created, person)
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
            val relationship = peopleRepository.saveKnows(id, otherId)
            call.respond(HttpStatusCode.Created, relationship)
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

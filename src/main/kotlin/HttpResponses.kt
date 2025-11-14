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

import io.ktor.http.ContentType
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.RoutingContext
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

suspend inline fun <reified T> RoutingContext.respondStreaming(
    flow: Flow<T>
) {
    call.respondTextWriter(
        contentType = ContentType.Application.Json
    ) {
        write("[\n")
        flush()
        var first = true
        flow.collect { item ->
            if (!first) write(",\n")
            first = false
            write(Json.encodeToString<T>(item))
            flush()
        }
        write("\n]")
        flush()
    }
}

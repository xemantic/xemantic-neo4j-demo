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
import io.ktor.http.withCharset
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.RoutingContext
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
suspend inline fun <reified T> RoutingContext.respondStreaming(
    flow: Flow<T>
) {
    call.respondOutputStream(
        contentType = ContentType.Application.Json.withCharset(
            Charsets.UTF_8
        )
    ) {

        write("[\n")
        flush()

        var first = true

        try {
            flow.collect { item ->
                if (!first) write(",\n")
                flush()
                first = false
                Json.encodeToStream<T>(item, this)
                flush()
            }
        } catch (e: Exception) { // Kotlin is not logging this by default
            logger.error(e) {
                "Unexpected error while streaming the response"
            }
            throw e
        }

        write("\n]")
        flush()
    }

}

fun OutputStream.write(value: String) = write(
    value.toByteArray()
)

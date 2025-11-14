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

import com.xemantic.neo4j.driver.Neo4jOperations
import com.xemantic.neo4j.driver.singleOrNull
import com.xemantic.neo4j.driver.toObject
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun Application.peopleRepository() {
    dependencies.provide<PeopleRepository> {
        Neo4jPeopleRepository(
            neo4j = resolve<Neo4jOperations>()
        )
    }
}

interface PeopleRepository {

    suspend fun save(
        request: CreatePersonRequest
    ): Person

    suspend fun load(id: String): Person?

    fun list(): Flow<Person>

    suspend fun saveKnows(
        id: String,
        otherId: String
    ): Relationship

    fun listFriends(
        personId: String
    ): Flow<Person>

}

class Neo4jPeopleRepository(
    private val neo4j: Neo4jOperations
) : PeopleRepository {

    override suspend fun save(
        request: CreatePersonRequest
    ): Person = neo4j.write { tx ->
        tx.run(
            query = $$"""
                CREATE (p:Person {
                    id: $id,
                    name: $name,
                    age: $age,
                    createdAt: datetime(),
                    updatedAt: datetime()
                })
                RETURN p
            """.trimIndent(),
            parameters = mapOf(
                "id" to request.id,
                "name" to request.name,
                "age" to request.age
            )
        ).single()["p"].toObject<Person>()
    }

    override suspend fun load(
        id: String
    ): Person? = neo4j.read { tx ->
        tx.run(
            query = $$"""
                MATCH (p:Person {id: $id})
                RETURN p
            """.trimIndent(),
            parameters = mapOf("id" to id)
        ).singleOrNull()?.let {
            it["p"].toObject<Person>()
        }
    }

    override fun list(): Flow<Person> = neo4j.flow(
        "MATCH (p:Person) RETURN p ORDER BY p.id"
    ).map {
        it["p"].toObject<Person>()
    }

    override suspend fun saveKnows(
        id: String,
        otherId: String
    ): Relationship = neo4j.write { tx ->
        tx.run(
            query = $$"""
                MATCH (p1:Person {id: $id}), (p2:Person {id: $otherId})
                CREATE (p1)-[r:KNOWS {createdAt: datetime()}]->(p2)
                RETURN
                    type(r) AS type,
                    p1.id AS from,
                    p2.id AS to,
                    r.createdAt AS createdAt
            """.trimIndent(),
            parameters = mapOf(
                "id" to id,
                "otherId" to otherId
            )
        ).single().toObject<Relationship>()
    }

    override fun listFriends(
        personId: String
    ): Flow<Person> = neo4j.flow(
        query = $$"""
            MATCH (p:Person {id: $id})-[:KNOWS]->(friend:Person)
            RETURN friend
            ORDER BY friend.id
        """.trimIndent(),
        parameters = mapOf("id" to personId)
    ).map {
        it["friend"].toObject<Person>()
    }

}

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

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.power.assert)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.versions)
}

group = "com.xemantic.neo4j.demo"

val javaTarget = libs.versions.javaTarget.get()
val kotlinTarget = KotlinVersion.fromVersion(libs.versions.kotlinTarget.get())

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        apiVersion = kotlinTarget
        languageVersion = kotlinTarget
        jvmTarget = JvmTarget.fromTarget(javaTarget)
        freeCompilerArgs.addAll(
            "-Xjdk-release=$javaTarget"
        )
        optIn.addAll("kotlin.time.ExperimentalTime")
        extraWarnings = true
        progressiveMode = true
    }
    coreLibrariesVersion = libs.versions.kotlin.get()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = javaTarget.toInt()
}

dependencies {
    implementation(libs.neo4j.driver)
    implementation(libs.xemantic.neo4j.kotlin.driver)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.di)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)

    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.serialization.kotlinx.xml)

    implementation(libs.slf4j.jdk.platform.logging)
    implementation(libs.logback.classic)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.xemantic.kotlin.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.neo4j.harness) {
        // we are running tests involving both - the driver and neo4j instance itself
        // for this reason we need to keep only one logging bindings provider (logback)
        exclude(group = "org.neo4j",  module = "neo4j-slf4j-provider")
    }
}

tasks.register<Jar>("uberjar") {

    group = "build"
    description = "Creates a fat JAR with all dependencies"

    dependsOn("build")

    archiveClassifier.set("uber")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Set the main class for execution
    manifest {
        attributes(
            "Main-Class" to "com.xemantic.neo4j.demo.ServerKt"
        )
    }

    // Include compiled classes
    from(sourceSets.main.get().output)

    // Include all dependencies
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter {
            it.name.endsWith("jar")
        }.map {
            zipTree(it)
        }
    })
}

powerAssert {
    functions = listOf(
        "com.xemantic.kotlin.test.assert",
        "com.xemantic.kotlin.test.have"
    )
}

val unstableKeywords = listOf("alpha", "beta", "rc")

fun isNonStable(
    version: String
) = version.lowercase().let { normalizedVersion ->
    unstableKeywords.any {
        it in normalizedVersion
    }
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

tasks.test {

    useJUnitPlatform()

    testLogging {
        events(
            TestLogEvent.SKIPPED
        )
        showStackTraces = false
        showExceptions = false
        showCauses = false
    }

    afterTest(KotlinClosure2({ descriptor: TestDescriptor, result: TestResult ->
        if (result.resultType == TestResult.ResultType.FAILURE) {
            val testName = "${descriptor.className}.${descriptor.name}"

            logger.lifecycle("\n<test-failure test=\"$testName\">")
            logger.lifecycle("<message>")
            result.exceptions.forEach { exception ->
                val message = exception.message ?: exception.toString()
                logger.lifecycle(
                    message.removePrefix("kotlin.AssertionError: ")
                )
            }
            logger.lifecycle("</message>")

            result.exceptions.forEach { exception ->
                logger.lifecycle("<stacktrace>")
                exception.stackTrace.forEach { element ->
                    logger.lifecycle("  at $element")
                }
                logger.lifecycle("</stacktrace>")
            }

            logger.lifecycle("</test-failure>\n")
        }
    }))

}

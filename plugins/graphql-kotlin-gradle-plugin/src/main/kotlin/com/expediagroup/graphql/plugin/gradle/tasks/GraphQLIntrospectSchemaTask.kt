/*
 * Copyright 2020 Expedia, Inc
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

package com.expediagroup.graphql.plugin.gradle.tasks

import com.expediagroup.graphql.plugin.config.TimeoutConfig
import com.expediagroup.graphql.plugin.introspectSchema
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

internal const val INTROSPECT_SCHEMA_TASK_NAME: String = "graphqlIntrospectSchema"

/**
 * Task that executes GraphQL introspection query against specified endpoint and saves the underlying schema file.
 */
@Suppress("UnstableApiUsage")
open class GraphQLIntrospectSchemaTask : DefaultTask() {

    /**
     * Target GraphQL server endpoint that will be used to execute introspection queries.
     */
    @Input
    @Option(option = "endpoint", description = "target GraphQL endpoint")
    val endpoint: Property<String> = project.objects.property(String::class.java)

    /**
     * Optional HTTP headers to be specified on an introspection query.
     */
    @Input
    val headers: MapProperty<String, Any> = project.objects.mapProperty(String::class.java, Any::class.java)

    /**
     * Timeout configuration that specifies maximum amount of time (in milliseconds) to connect and execute introspection query before we cancel the request.
     * Defaults to Ktor CIO engine defaults (5 seconds for connect timeout and 15 seconds for read timeout).
     */
    @Input
    val timeoutConfig: Property<TimeoutConfig> = project.objects.property(TimeoutConfig::class.java)

    /**
     * Target GraphQL schema file to be generated.
     */
    @OutputFile
    val outputFile: RegularFileProperty = project.objects.fileProperty()

    init {
        group = "GraphQL"
        description = "Run introspection query against target GraphQL endpoint and save schema locally."

        headers.convention(emptyMap())
        timeoutConfig.convention(TimeoutConfig())
        outputFile.convention(project.layout.buildDirectory.file("schema.graphql"))
    }

    /**
     * Executes introspection query against specified endpoint and saves the resulting schema locally in the target output file.
     */
    @Suppress("EXPERIMENTAL_API_USAGE")
    @TaskAction
    fun introspectSchemaAction() {
        logger.debug("starting introspection task against ${endpoint.get()}")
        runBlocking {
            val schema = introspectSchema(endpoint = endpoint.get(), httpHeaders = headers.get(), timeoutConfig = timeoutConfig.get())
            val outputFile = outputFile.asFile.get()
            outputFile.writeText(schema)
        }
        logger.debug("successfully created GraphQL schema from introspection result")
    }
}

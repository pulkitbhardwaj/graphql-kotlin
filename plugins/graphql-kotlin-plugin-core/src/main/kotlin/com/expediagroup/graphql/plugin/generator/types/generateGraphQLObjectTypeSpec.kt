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

package com.expediagroup.graphql.plugin.generator.types

import com.expediagroup.graphql.plugin.generator.GraphQLClientGeneratorContext
import com.expediagroup.graphql.plugin.generator.exceptions.InvalidSelectionSetException
import com.expediagroup.graphql.plugin.generator.extensions.findFragmentDefinition
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import graphql.language.FragmentSpread
import graphql.language.ObjectTypeDefinition
import graphql.language.SelectionSet

/**
 * Generate [TypeSpec] data class from the GraphQL object definition based on the selection set.
 */
internal fun generateGraphQLObjectTypeSpec(
    context: GraphQLClientGeneratorContext,
    objectDefinition: ObjectTypeDefinition,
    selectionSet: SelectionSet?,
    objectNameOverride: String? = null
): TypeSpec {
    if (selectionSet == null || selectionSet.selections.isEmpty()) {
        throw InvalidSelectionSetException(objectDefinition.name, "object")
    }

    val typeName = objectNameOverride ?: objectDefinition.name
    val objectTypeSpecBuilder = TypeSpec.classBuilder(typeName)
    objectTypeSpecBuilder.modifiers.add(KModifier.DATA)
    objectDefinition.description?.content?.let { kdoc ->
        objectTypeSpecBuilder.addKdoc(kdoc)
    }

    val constructorBuilder = FunSpec.constructorBuilder()
    generatePropertySpecs(
        context = context,
        objectName = objectDefinition.name,
        selectionSet = selectionSet,
        fieldDefinitions = objectDefinition.fieldDefinitions
    ).forEach { propertySpec ->
        objectTypeSpecBuilder.addProperty(propertySpec)
        constructorBuilder.addParameter(propertySpec.name, propertySpec.type)
    }

    selectionSet.getSelectionsOfType(FragmentSpread::class.java)
        .forEach { fragment ->
            val fragmentDefinition = context.queryDocument
                .findFragmentDefinition(context, fragment.name, objectDefinition.name)
            generatePropertySpecs(
                context = context,
                objectName = objectDefinition.name,
                selectionSet = fragmentDefinition.selectionSet,
                fieldDefinitions = objectDefinition.fieldDefinitions
            ).forEach { propertySpec ->
                objectTypeSpecBuilder.addProperty(propertySpec)
                constructorBuilder.addParameter(propertySpec.name, propertySpec.type)
            }
        }

    objectTypeSpecBuilder.primaryConstructor(constructorBuilder.build())

    val objectTypeSpec = objectTypeSpecBuilder.build()
    context.typeSpecs[typeName] = objectTypeSpec
    return objectTypeSpec
}

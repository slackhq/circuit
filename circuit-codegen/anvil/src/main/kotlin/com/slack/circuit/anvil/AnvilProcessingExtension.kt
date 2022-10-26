/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit.anvil

import com.google.auto.service.AutoService
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSType
import com.slack.circuit.CircuitScope
import com.slack.circuit.codegen.CircuitProcessingExtension
import com.slack.circuit.codegen.FactoryType
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName

private val CIRCUIT_SCOPE_ANNOTATION = CircuitScope::class.java.canonicalName

@AutoService(CircuitProcessingExtension::class)
public class AnvilProcessingExtension : CircuitProcessingExtension {
  override fun process(spec: TypeSpec, ksAnnotated: KSAnnotated, type: FactoryType): TypeSpec {
    val circuitScopeAnnotation =
      ksAnnotated.annotations.firstOrNull() {
        it.annotationType.resolve().declaration.qualifiedName?.asString() ==
          CIRCUIT_SCOPE_ANNOTATION
      }
        ?: return spec
    val scope = (circuitScopeAnnotation.arguments.single().value as KSType).toTypeName()
    /**
     * @CircuitScope(AppScope::class) generates...
     *
     * @ContributesTo(AppScope::class) class HomeUiFactory @Inject constructor() : UiFactory
     */
    return spec
      .toBuilder()
      .addAnnotation(
        AnnotationSpec.builder(ContributesMultibinding::class).addMember("%T::class", scope).build()
      )
      .primaryConstructor(
        FunSpec.constructorBuilder().addAnnotation(ClassName("javax.inject", "Inject")).build()
      )
      .build()
  }
}

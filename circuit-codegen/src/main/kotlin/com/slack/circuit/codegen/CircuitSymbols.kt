// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName

/**
 * Runtime [KSType]s the processor matches parameters against.
 *
 * Circuit and SubCircuit runtimes are independent, so a consumer may have only one on its
 * classpath. Each type is loaded lazily and only accessed by its own [CodegenTarget]; accessing an
 * absent type is an [error], since the matching annotation guarantees its runtime is present.
 */
internal class CircuitSymbols private constructor(resolver: Resolver) {
  val modifier = resolver.requireKSType(CircuitNames.MODIFIER)

  // Circuit runtime types.
  private val circuitUiStateOrNull = resolver.loadOptionalKSType(CircuitNames.CIRCUIT_UI_STATE)
  private val screenOrNull = resolver.loadOptionalKSType(CircuitNames.SCREEN)
  private val navigatorOrNull = resolver.loadOptionalKSType(CircuitNames.NAVIGATOR)
  private val circuitContextOrNull = resolver.loadOptionalKSType(CircuitNames.CIRCUIT_CONTEXT)

  // SubCircuit runtime types.
  private val subCircuitUiStateOrNull =
    resolver.loadOptionalKSType(CircuitNames.SUB_CIRCUIT_UI_STATE)

  val circuitUiState: KSType
    get() = circuitUiStateOrNull.require(CircuitNames.CIRCUIT_UI_STATE)

  val screen: KSType
    get() = screenOrNull.require(CircuitNames.SCREEN)

  val navigator: KSType
    get() = navigatorOrNull.require(CircuitNames.NAVIGATOR)

  val circuitContext: KSType
    get() = circuitContextOrNull.require(CircuitNames.CIRCUIT_CONTEXT)

  val subCircuitUiState: KSType
    get() = subCircuitUiStateOrNull.require(CircuitNames.SUB_CIRCUIT_UI_STATE)

  private fun KSType?.require(name: ClassName): KSType =
    this ?: error("Could not find ${name.canonicalName} in classpath")

  private fun Resolver.requireKSType(name: ClassName): KSType =
    loadOptionalKSType(name) ?: error("Could not find ${name.canonicalName} in classpath")

  private fun Resolver.loadOptionalKSType(name: ClassName): KSType? {
    return getClassDeclarationByName(getKSNameFromString(name.canonicalName))?.asType(emptyList())
  }

  companion object {
    fun create(resolver: Resolver): CircuitSymbols? {
      // Modifier (from compose-ui) is the one type both the Circuit and SubCircuit runtimes share,
      // so it's the minimal precondition for generating anything. Bail cleanly if it's absent.
      // Runtime-specific types load lazily and are required only by their own CodegenTarget.
      val hasModifier =
        resolver.getClassDeclarationByName(
          resolver.getKSNameFromString(CircuitNames.MODIFIER.canonicalName)
        ) != null
      return if (hasModifier) CircuitSymbols(resolver) else null
    }
  }
}

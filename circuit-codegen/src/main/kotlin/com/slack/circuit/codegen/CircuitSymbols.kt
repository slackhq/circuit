// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName

/**
 * Holds the runtime [KSType]s the processor matches parameters against.
 *
 * Every type is loaded optionally: the processor handles both the `@CircuitInject`
 * ([com.slack.circuit.codegen.annotations.CircuitInject]) and `@SubCircuitInject` dialects, and a
 * consumer typically only has one runtime on its classpath. Circuit types are absent on a
 * SubCircuit-only classpath and vice versa, so accessing a type that wasn't found is an [error];
 * the annotation being present guarantees the matching runtime is available.
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
      @Suppress("SwallowedException")
      return try {
        CircuitSymbols(resolver)
      } catch (e: IllegalStateException) {
        null
      }
    }
  }
}

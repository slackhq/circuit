// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType

internal class CircuitSymbols private constructor(resolver: Resolver) {
  val modifier = resolver.loadKSType(CircuitNames.MODIFIER.canonicalName)
  val circuitUiState = resolver.loadKSType(CircuitNames.CIRCUIT_UI_STATE.canonicalName)
  val screen = resolver.loadKSType(CircuitNames.SCREEN.canonicalName)
  val navigator = resolver.loadKSType(CircuitNames.NAVIGATOR.canonicalName)
  val circuitContext = resolver.loadKSType(CircuitNames.CIRCUIT_CONTEXT.canonicalName)

  private fun Resolver.loadKSType(name: String): KSType =
    loadOptionalKSType(name) ?: error("Could not find $name in classpath")

  private fun Resolver.loadOptionalKSType(name: String?): KSType? {
    if (name == null) return null
    return getClassDeclarationByName(getKSNameFromString(name))?.asType(emptyList())
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

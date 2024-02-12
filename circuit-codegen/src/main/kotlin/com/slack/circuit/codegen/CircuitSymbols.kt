package com.slack.circuit.codegen

import com.google.devtools.ksp.processing.Resolver

internal class CircuitSymbols private constructor(resolver: Resolver) {
  val modifier = resolver.loadKSType(CircuitNames.MODIFIER.canonicalName)
  val circuitUiState = resolver.loadKSType(CircuitNames.CIRCUIT_UI_STATE.canonicalName)
  val screen = resolver.loadKSType(CircuitNames.SCREEN.canonicalName)
  val navigator = resolver.loadKSType(CircuitNames.NAVIGATOR.canonicalName)

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
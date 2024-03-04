// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen

import com.google.devtools.ksp.processing.Resolver

internal class CircuitSymbols private constructor(resolver: Resolver) {
  val modifier = resolver.loadKSType(CircuitNames.MODIFIER.canonicalName)
  val circuitUiState = resolver.loadKSType(CircuitNames.CIRCUIT_UI_STATE.canonicalName)
  val screen = resolver.loadKSType(CircuitNames.SCREEN.canonicalName)
  val navigator = resolver.loadKSType(CircuitNames.NAVIGATOR.canonicalName)
  val presenterFactory = resolver.loadKSType(CircuitNames.CIRCUIT_PRESENTER_FACTORY.canonicalName)
  val uiFactory = resolver.loadKSType(CircuitNames.CIRCUIT_UI_FACTORY.canonicalName)

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

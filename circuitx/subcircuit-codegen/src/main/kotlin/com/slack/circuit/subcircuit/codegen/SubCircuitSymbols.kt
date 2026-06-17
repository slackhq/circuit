// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit.codegen

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType

internal class SubCircuitSymbols private constructor(resolver: Resolver) {
  val modifier = resolver.loadKSType(SubCircuitNames.MODIFIER.canonicalName)
  val subCircuitUiState = resolver.loadKSType(SubCircuitNames.SUB_CIRCUIT_UI_STATE.canonicalName)
  val subScreen = resolver.loadKSType(SubCircuitNames.SUB_SCREEN.canonicalName)
  val subPresenter = resolver.loadKSType(SubCircuitNames.SUB_PRESENTER.canonicalName)
  val subUi = resolver.loadKSType(SubCircuitNames.SUB_UI.canonicalName)

  private fun Resolver.loadKSType(name: String): KSType =
    loadOptionalKSType(name) ?: error("Could not find $name in classpath")

  private fun Resolver.loadOptionalKSType(name: String?): KSType? {
    if (name == null) return null
    return getClassDeclarationByName(getKSNameFromString(name))?.asType(emptyList())
  }

  companion object {
    fun create(resolver: Resolver): SubCircuitSymbols? {
      @Suppress("SwallowedException")
      return try {
        SubCircuitSymbols(resolver)
      } catch (e: IllegalStateException) {
        null
      }
    }
  }
}

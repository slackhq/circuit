// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.KSApiFeatures
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.validate

internal object CircuitKspApi {
  // KSP2 reports a fixed kspVersion, so check for the API in the processor's classloader.
  private val supportsApiFeatures by lazy {
    try {
      Class.forName(
        "com.google.devtools.ksp.ApiFeatures",
        false,
        SymbolProcessor::class.java.classLoader,
      )
      true
    } catch (_: ClassNotFoundException) {
      false
    }
  }

  val features: ApiFeatures
    get() = FeatureAwareValidation.features

  @Suppress("DEPRECATION") // Older KSP versions only provide the legacy validation overload.
  fun validate(symbol: KSNode): Boolean =
    if (supportsApiFeatures) {
      FeatureAwareValidation.validate(symbol)
    } else {
      symbol.validate()
    }

  // Keep ApiFeatures initialization off the path used by older KSP versions.
  private object FeatureAwareValidation {
    val features: ApiFeatures =
      KSApiFeatures(enableBackingFields = true, enableContextParameters = true)

    fun validate(symbol: KSNode): Boolean = symbol.validate(apiFeatures = features)
  }
}

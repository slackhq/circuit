// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

@Composable internal expect fun PlatformCompositionLocals(content: @Composable () -> Unit)

/**
 * Provides the given [circuitConfig] as a [CompositionLocal] to all composables within [content].
 * Also adds any other composition locals that Circuit needs.
 */
@Composable
public fun CircuitCompositionLocals(circuitConfig: CircuitConfig, content: @Composable () -> Unit) {
  CompositionLocalProvider(
    LocalCircuitOwner provides circuitConfig,
  ) {
    PlatformCompositionLocals(content)
  }
}

internal val LocalCircuitContext = compositionLocalOf<CircuitContext?> { null }

public object LocalCircuitOwner {
  private val LocalCircuitConfig = staticCompositionLocalOf<CircuitConfig?> { null }

  /**
   * Returns current composition local value for the owner or errors if one has not been provided.
   */
  public val current: CircuitConfig
    @Composable get() = LocalCircuitConfig.current ?: error("No circuit available")

  /** Associates a [LocalCircuitConfig] key to a value in a call to [CompositionLocalProvider]. */
  public infix fun provides(circuitConfig: CircuitConfig): ProvidedValue<CircuitConfig?> {
    return LocalCircuitConfig.provides(circuitConfig)
  }
}

// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.NoOpRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.retained.lifecycleRetainedStateRegistry
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.InternalCircuitSaverApi
import com.slack.circuit.runtime.screen.ProvideCircuitSaver
import com.slack.circuit.runtime.screen.currentProvidedCircuitSaverOrNull

/**
 * Provides [circuit] and the other composition locals Circuit needs to all composables within
 * [content].
 *
 * This uses a saver configured on [Circuit] when one is present. Otherwise, it resolves a fallback
 * from `ProvideCircuitSaver` or [rememberDefaultCircuitSaver], then applies any saver transform
 * configured on [Circuit]. Use the overload that accepts a [CircuitSaver] to provide a different
 * final saver.
 */
@OptIn(InternalCircuitSaverApi::class)
@Composable
public fun CircuitCompositionLocals(
  circuit: Circuit,
  retainedStateRegistry: RetainedStateRegistry = lifecycleRetainedStateRegistry(),
  content: @Composable () -> Unit,
) {
  val fallbackSaver = currentProvidedCircuitSaverOrNull() ?: rememberDefaultCircuitSaver()
  val circuitSaver =
    circuit.circuitSaver
      ?: circuit.circuitSaverTransform?.let { transform ->
        remember(transform, fallbackSaver) { transform(fallbackSaver) }
      }
      ?: fallbackSaver
  CircuitCompositionLocals(
    circuit = circuit,
    circuitSaver = circuitSaver,
    retainedStateRegistry = retainedStateRegistry,
    content = content,
  )
}

/**
 * Provides [circuit], [circuitSaver], and the other composition locals Circuit needs to all
 * composables within [content].
 */
@Composable
public fun CircuitCompositionLocals(
  circuit: Circuit,
  circuitSaver: CircuitSaver,
  retainedStateRegistry: RetainedStateRegistry = lifecycleRetainedStateRegistry(),
  content: @Composable () -> Unit,
) {
  ProvideCircuitSaver(circuitSaver) {
    CompositionLocalProvider(
      LocalCircuit provides circuit,
      LocalRetainedStateRegistry provides retainedStateRegistry,
    ) {
      content()
    }
  }
}

internal val LocalCircuitContext = compositionLocalOf<CircuitContext?> { null }

/** CompositionLocal with a current [Circuit] instance. */
public val LocalCircuit: ProvidableCompositionLocal<Circuit?> = staticCompositionLocalOf { null }

private val EMPTY_CIRCUIT = Circuit.Builder().build()

/**
 * A composable function that provides no-op/empty Circuit composition locals that can be safely
 * used within a `@Preview` composable.
 *
 * Example:
 * ```kotlin
 * @Preview
 * fun ListItemPreview() {
 *   CircuitPreview {
 *     ListItem()
 *   }
 * }
 * ```
 */
@Composable
public fun CircuitPreview(content: @Composable () -> Unit) {
  CircuitCompositionLocals(
    circuit = EMPTY_CIRCUIT,
    circuitSaver = CircuitSaver.NoOp,
    retainedStateRegistry = NoOpRetainedStateRegistry,
    content = content,
  )
}

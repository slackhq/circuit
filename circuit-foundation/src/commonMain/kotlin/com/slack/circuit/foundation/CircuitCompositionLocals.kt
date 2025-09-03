// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.NoOpRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.retained.lifecycleRetainedStateRegistry
import com.slack.circuit.runtime.CircuitContext

/**
 * Provides the given [circuit] as a [CompositionLocal] to all composables within [content]. Also
 * adds any other composition locals that Circuit needs.
 */
@Composable
public fun CircuitCompositionLocals(
  circuit: Circuit,
  retainedStateRegistry: RetainedStateRegistry = lifecycleRetainedStateRegistry(),
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(
    LocalCircuit provides circuit,
    LocalRetainedStateRegistry provides retainedStateRegistry,
  ) {
    content()
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
    retainedStateRegistry = NoOpRetainedStateRegistry,
    content = content,
  )
}

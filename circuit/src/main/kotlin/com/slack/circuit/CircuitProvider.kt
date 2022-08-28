/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Provides the given [circuit] as a [CompositionLocal] to all composables within [content]. */
@Composable
fun CircuitProvider(circuit: Circuit, content: @Composable () -> Unit) {
  CompositionLocalProvider(
    LocalCircuitOwner provides circuit,
  ) {
    content()
  }
}

object LocalCircuitOwner {
  private val LocalCircuit = staticCompositionLocalOf<Circuit?> { null }

  /**
   * Returns current composition local value for the owner or errors if one has not been provided.
   */
  val current: Circuit
    @Composable get() = LocalCircuit.current ?: error("No circuit available")

  /** Associates a [LocalCircuit] key to a value in a call to [CompositionLocalProvider]. */
  infix fun provides(circuit: Circuit): ProvidedValue<Circuit?> {
    return LocalCircuit.provides(circuit)
  }
}

// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.currentComposer
import com.slack.circuit.runtime.InternalCircuitApi

/**
 * A slightly more efficient version of [withCompositionLocalProvider] that only accepts a single
 * [value].
 */
@Composable
@InternalCircuitApi
@OptIn(InternalComposeApi::class)
public fun <R> withCompositionLocalProvider(
  value: ProvidedValue<*>,
  content: @Composable () -> R,
): R {
  currentComposer.startProvider(value)
  return content().also { currentComposer.endProvider() }
}

/**
 * A composable function that provides a [ProvidedValue] to the composition and returns the value
 * returned by the [content] composable. This uses internal compose APIs to start and end providers
 * around the [content] composable to avoid backward snapshot writes.
 *
 * @param values The [ProvidedValues][ProvidedValue] to provide.
 * @param content The content to provide the value to.
 */
@Composable
@InternalCircuitApi
@OptIn(InternalComposeApi::class)
public fun <R> withCompositionLocalProvider(
  vararg values: ProvidedValue<*>,
  content: @Composable () -> R,
): R {
  currentComposer.startProviders(values)
  return content().also { currentComposer.endProviders() }
}

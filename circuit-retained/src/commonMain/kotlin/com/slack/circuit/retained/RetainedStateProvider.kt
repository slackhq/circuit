// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Provides a [RetainedStateRegistry] for the child [content] based on the specified [key]. Before
 * the provided registry is disposed, it calls [RetainedStateRegistry.saveValue] on the parent
 * registry to save the current value, allowing it to be restored on the next visit with the same
 * key.
 */
@Composable
public fun <T> RetainedStateProvider(key: String? = null, content: @Composable () -> T) {
  @OptIn(ExperimentalUuidApi::class)
  val finalKey = key ?: rememberRetained { Uuid.random().toString() }
  val canRetainChecker = LocalCanRetainChecker.current ?: CanRetainChecker.Always
  val parentRegistry = LocalRetainedStateRegistry.current
  val registry = rememberRetained(key = finalKey) { RetainedStateRegistry() }
  CompositionLocalProvider(
    LocalRetainedStateRegistry provides registry,
    LocalCanRetainChecker provides CanRetainChecker.Always,
  ) {
    content()
  }
  DisposableEffect(finalKey, registry) {
    onDispose {
      registry.saveAll()
      if (canRetainChecker.canRetain(registry)) {
        parentRegistry.saveValue(finalKey)
      }
    }
  }
}

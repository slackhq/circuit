// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.slack.circuit.retained.CanRetainChecker
import com.slack.circuit.retained.LocalCanRetainChecker
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateProvider
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.retained.rememberRetained

/** Copy of [RetainedStateProvider] to return content value */
@Composable
internal fun <T> withRetainedStateProvider(key: String, content: @Composable () -> T): T {
  val canRetainChecker = LocalCanRetainChecker.current ?: CanRetainChecker.Always
  val parentRegistry = LocalRetainedStateRegistry.current
  val registry = rememberRetained(key = key) { RetainedStateRegistry() }
  return withCompositionLocalProvider(
      LocalRetainedStateRegistry provides registry,
      LocalCanRetainChecker provides CanRetainChecker.Always,
    ) {
      content()
    }
    .also {
      DisposableEffect(key, registry) {
        onDispose {
          registry.saveAll()
          if (canRetainChecker.canRetain(registry)) {
            parentRegistry.saveValue(key)
          }
        }
      }
    }
}

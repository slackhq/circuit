// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.slack.circuit.foundation.internal.withCompositionLocalProvider
import com.slack.circuit.retained.CanRetainChecker
import com.slack.circuit.retained.LocalCanRetainChecker
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.retained.RetainedValueProvider
import com.slack.circuit.retained.rememberRetained

/** Copy of [RetainedStateHolder] to return content value */
internal interface RetainedStateHolder {

  @Composable fun <T> RetainedStateProvider(key: String, content: @Composable () -> T): T
}

/** Creates and remembers the instance of [RetainedStateHolder]. */
@Composable
internal fun rememberRetainedStateHolderWithReturn(): RetainedStateHolder {
  return rememberRetained { RetainedStateHolderImpl() }
}

private class RetainedStateHolderImpl : RetainedStateHolder, RetainedStateRegistry {

  private val registry = RetainedStateRegistry()

  @Composable
  override fun <T> RetainedStateProvider(key: String, content: @Composable (() -> T)): T {
    return withCompositionLocalProvider(LocalRetainedStateRegistry provides this) {
      val canRetainChecker = LocalCanRetainChecker.current ?: CanRetainChecker.Always
      val childRegistry = rememberRetained(key = key) { RetainedStateRegistry() }
      withCompositionLocalProvider(
          LocalRetainedStateRegistry provides childRegistry,
          LocalCanRetainChecker provides CanRetainChecker.Always,
        ) {
          content()
        }
        .also {
          DisposableEffect(key, childRegistry) {
            onDispose {
              childRegistry.saveAll()
              if (canRetainChecker.canRetain(this@RetainedStateHolderImpl)) {
                saveValue(key)
              }
            }
          }
        }
    }
  }

  override fun consumeValue(key: String): Any? {
    return registry.consumeValue(key)
  }

  override fun registerValue(
    key: String,
    valueProvider: RetainedValueProvider,
  ): RetainedStateRegistry.Entry {
    return registry.registerValue(key, valueProvider)
  }

  override fun saveAll() {
    registry.saveAll()
  }

  override fun saveValue(key: String) {
    registry.saveValue(key)
  }

  override fun forgetUnclaimedValues() {
    registry.forgetUnclaimedValues()
  }
}

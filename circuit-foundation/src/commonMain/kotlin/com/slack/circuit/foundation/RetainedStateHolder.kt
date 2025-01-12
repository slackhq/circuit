// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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

  fun removeState(key: String)
}

/** Creates and remembers the instance of [RetainedStateHolder]. */
@Composable
internal fun rememberRetainedStateHolderWithReturn(): RetainedStateHolder {
  return rememberRetained { RetainedStateHolderImpl() }
}

private class RetainedStateHolderImpl : RetainedStateHolder, RetainedStateRegistry {

  private val registry = RetainedStateRegistry()

  private val canRetainCheckers = mutableMapOf<String, EntryCanRetainChecker>()

  @Composable
  override fun <T> RetainedStateProvider(key: String, content: @Composable (() -> T)): T {
    return withCompositionLocalProvider(LocalRetainedStateRegistry provides registry) {
      val parentCanRetainChecker = LocalCanRetainChecker.current ?: CanRetainChecker.Always
      key(key) {
        val entryCanRetainChecker =
          remember(parentCanRetainChecker) { EntryCanRetainChecker(parentCanRetainChecker) }
        val childRegistry = rememberRetained(key = key) { RetainedStateRegistry() }
        withCompositionLocalProvider(
            LocalRetainedStateRegistry provides childRegistry,
            LocalCanRetainChecker provides CanRetainChecker.Always,
            content = content,
          )
          .also {
            DisposableEffect(Unit) {
              canRetainCheckers[key] = entryCanRetainChecker
              onDispose {
                childRegistry.saveAll()
                if (entryCanRetainChecker.canRetain(registry)) {
                  registry.saveValue(key)
                }
                canRetainCheckers -= key
              }
            }
          }
      }
    }
  }

  override fun removeState(key: String) {
    val canRetainChecker = canRetainCheckers[key]
    if (canRetainChecker != null) {
      canRetainChecker.shouldSave = false
    } else {
      registry.consumeValue(key)
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

  private class EntryCanRetainChecker(private val parentChecker: CanRetainChecker) :
    CanRetainChecker {

    var shouldSave = true

    override fun canRetain(registry: RetainedStateRegistry): Boolean =
      parentChecker.canRetain(registry) && shouldSave
  }
}

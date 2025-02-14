// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.slack.circuit.foundation.internal.withCompositionLocalProvider
import com.slack.circuit.retained.CanRetainChecker
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
internal fun rememberRetainedStateHolderWithReturn(
  canRetainChecker: CanRetainChecker = CanRetainChecker.Always
): RetainedStateHolder {
  val currentCanRetainChecker by rememberUpdatedState(canRetainChecker)
  return rememberRetained { RetainedStateHolderImpl { currentCanRetainChecker.canRetain() } }
}

private class RetainedStateHolderImpl(canRetainChecker: CanRetainChecker) :
  RetainedStateHolder, RetainedStateRegistry {

  private val registry = RetainedStateRegistry(canRetainChecker)

  private val entryCheckers = mutableMapOf<String, EntryCanRetainChecker>()

  @Composable
  override fun <T> RetainedStateProvider(key: String, content: @Composable (() -> T)): T {
    return withCompositionLocalProvider(LocalRetainedStateRegistry provides registry) {
      key(key) {
        val entryCanRetainChecker = remember { EntryCanRetainChecker() }
        val currentEntryCanRetainChecker by rememberUpdatedState(entryCanRetainChecker)
        val childRegistry =
          rememberRetained(key = key) {
            RetainedStateRegistry({ currentEntryCanRetainChecker.canRetain() })
          }
        withCompositionLocalProvider(
            LocalRetainedStateRegistry provides childRegistry,
            content = content,
          )
          .also {
            DisposableEffect(Unit) {
              entryCheckers[key] = entryCanRetainChecker
              onDispose {
                registry.saveValue(key)
                entryCheckers -= key
              }
            }
          }
      }
    }
  }

  override fun removeState(key: String) {
    val entry = entryCheckers[key]
    if (entry != null) {
      entry.shouldSave = false
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

  override fun saveAll(): Map<String, List<Any?>> {
    return registry.saveAll()
  }

  override fun saveValue(key: String) {
    registry.saveValue(key)
  }

  override fun forgetUnclaimedValues() {
    registry.forgetUnclaimedValues()
  }
}

private class EntryCanRetainChecker(var shouldSave: Boolean = true) : CanRetainChecker {

  override fun canRetain(): Boolean = shouldSave
}

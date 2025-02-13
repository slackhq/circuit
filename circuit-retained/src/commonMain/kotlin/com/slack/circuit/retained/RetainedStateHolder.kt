// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * A holder that provides a unique retainedStateRegistry for each subtree and retains all preserved
 * values. Each [RetainedStateProvider] maintains a unique retainedStateRegistry for each key,
 * allowing it to save and restore states.
 */
public interface RetainedStateHolder {

  /**
   * Provides a [RetainedStateRegistry] for the child [content] based on the specified [key]. Before
   * the provided registry is disposed, it calls [RetainedStateRegistry.saveValue] on the holder's
   * registry to save the current value, allowing it to be restored on the next visit with the same
   * key.
   */
  @Composable public fun RetainedStateProvider(key: String, content: @Composable () -> Unit)

  /** Removes the retained state associated with the passed [key]. */
  public fun removeState(key: String)
}

/** Creates and remembers the instance of [RetainedStateHolder]. */
@Composable
public fun rememberRetainedStateHolder(
  canRetainChecker: CanRetainChecker = CanRetainChecker.Always
): RetainedStateHolder {
  val currentCanRetainChecker by rememberUpdatedState(canRetainChecker)
  return rememberRetained { RetainedStateHolderImpl { currentCanRetainChecker.canRetain() } }
}

private class RetainedStateHolderImpl(canRetainChecker: CanRetainChecker) :
  RetainedStateHolder, RetainedStateRegistry {

  private val registry: RetainedStateRegistry = RetainedStateRegistry(canRetainChecker)

  private val entryCheckers = mutableMapOf<String, EntryCanRetainChecker>()

  @Composable
  override fun RetainedStateProvider(key: String, content: @Composable (() -> Unit)) {
    CompositionLocalProvider(LocalRetainedStateRegistry provides registry) {
      ReusableContent(key) {
        val entryCanRetainChecker = remember { EntryCanRetainChecker() }
        val currentEntryCanRetainChecker by rememberUpdatedState(entryCanRetainChecker)
        val childRegistry =
          rememberRetained(key = key) {
            RetainedStateRegistry({ currentEntryCanRetainChecker.canRetain() })
          }
        CompositionLocalProvider(
          LocalRetainedStateRegistry provides childRegistry,
          content = content,
        )
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

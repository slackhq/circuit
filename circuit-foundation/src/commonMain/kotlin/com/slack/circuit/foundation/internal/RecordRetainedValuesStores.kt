// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.internal

import androidx.compose.runtime.retain.ForgetfulRetainedValuesStore
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.RetainedValuesStore

/**
 * A registry of per-record [RetainedValuesStore]s backing first-party `retain {}` calls inside
 * navigation record content.
 *
 * This intentionally does not use `RetainedValuesStoreRegistry`/`ManagedRetainedValuesStore`. Those
 * gate saving exited values on the content-presence indicator having already flipped the store out
 * of its composed state, and that ordering inverts when record content leaves composition through
 * an animated decorator's `AnimatedContent` pane: the record's retained value holders are forgotten
 * before the indicator runs, so values are retired instead of saved.
 *
 * A record store instead keeps every exiting value unconditionally. Record content only leaves
 * composition when the record goes off-screen or the composition is torn down, and both cases want
 * retention. Unclaimed values are retired when the record's content next settles back into
 * composition, and everything is retired when the record leaves the nav stack ([clear]) or the
 * registry itself is retired ([dispose]).
 */
/** Retainable holder that disposes the registry when the retain system retires it. */
internal class RecordRetainedValuesStoresHolder : RetainObserver {
  val stores = RecordRetainedValuesStores()

  override fun onRetained() {}

  override fun onEnteredComposition() {}

  override fun onExitedComposition() {}

  override fun onRetired() = stores.dispose()

  override fun onUnused() = stores.dispose()
}

internal class RecordRetainedValuesStores {
  private val stores = mutableMapOf<String, RecordRetainedValuesStore>()
  private var isDisposed = false

  fun storeFor(key: String): RetainedValuesStore =
    if (isDisposed) {
      // Content can still request its store while the retained holder and its children tear down.
      ForgetfulRetainedValuesStore
    } else {
      stores.getOrPut(key) { RecordRetainedValuesStore() }
    }

  fun clear(key: String) {
    stores.remove(key)?.dispose()
  }

  fun dispose() {
    if (isDisposed) return
    isDisposed = true
    val storesToDispose = stores.values.toList()
    stores.clear()
    storesToDispose.forEach { it.dispose() }
  }
}

private class RecordRetainedValuesStore : RetainedValuesStore {
  // Multiple values can be saved under one key (like movable content copies); restored LIFO to
  // mirror ManagedRetainedValuesStore's SafeMultiValueMap semantics.
  private val exitedValues = mutableMapOf<Any, MutableList<Any?>>()
  // Tolerates transient double-installation during interrupted transitions, where a record's
  // content can briefly compose in two decorator panes.
  private var compositionCount = 0
  private var isDisposed = false

  override fun consumeExitedValueOrDefault(key: Any, defaultValue: Any?): Any? {
    if (isDisposed) return defaultValue
    val values = exitedValues[key] ?: return defaultValue
    val value = values.removeAt(values.lastIndex)
    if (values.isEmpty()) exitedValues.remove(key)
    return value
  }

  override fun saveExitingValue(key: Any, value: Any?) {
    if (isDisposed) {
      (value as? RetainObserver)?.onRetired()
    } else {
      exitedValues.getOrPut(key) { mutableListOf() }.add(value)
    }
  }

  override fun onContentEnteredComposition() {
    if (isDisposed) return
    // Fires at frame end, after re-entering content has already consumed the values it claims.
    // Anything left is no longer referenced by the record's content.
    if (compositionCount++ == 0) {
      retireExitedValues()
    }
  }

  override fun onContentExitComposition() {
    if (isDisposed) return
    if (compositionCount > 0) compositionCount--
  }

  fun dispose() {
    if (isDisposed) return
    isDisposed = true
    compositionCount = 0
    retireExitedValues()
  }

  private fun retireExitedValues() {
    if (exitedValues.isEmpty()) return
    val values = exitedValues.values.flatten()
    exitedValues.clear()
    values.forEach { (it as? RetainObserver)?.onRetired() }
  }
}

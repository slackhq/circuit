// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.retain.ManagedRetainedValuesStore

/**
 * Owns the retained-value stores used by [RetainedValuesStoreProvider] to preserve values across
 * composition recreation.
 *
 * The owner must outlive every composition that should share retained values. Call [dispose] when
 * that lifetime permanently ends. Values are held in memory and do not survive process death. This
 * class is not thread-safe. Calls to it and callbacks from its leases must be serialized.
 *
 * Providers that share an owner use positional identity. Providers at the same position are matched
 * in acquisition order when recreated. Matching requires the previous composition to be disposed
 * before its replacement composes, so a provider that overlaps its predecessor receives a new empty
 * store. Use a separate owner for independently recreated roots whose order can change.
 *
 * Stores keep their values until [dispose], including when their provider permanently leaves the
 * composition.
 */
@ExperimentalCircuitRetainedApi
public class RetainedValuesStoreOwner {
  private val slots = mutableMapOf<Long, RetainedValuesStoreSlot>()
  private var isDisposed = false

  internal fun acquire(location: Long): RetainedValuesStoreLease {
    check(!isDisposed) { "Cannot acquire a retained values store from a disposed owner." }
    return slots.getOrPut(location) { RetainedValuesStoreSlot() }.acquire()
  }

  /**
   * Permanently releases every retained value held by this owner.
   *
   * Calling this function more than once has no effect. After disposal, this owner cannot be used
   * again.
   */
  public fun dispose() {
    if (isDisposed) return
    isDisposed = true

    slots.values.forEach(RetainedValuesStoreSlot::dispose)
    slots.clear()
  }
}

private class RetainedValuesStoreSlot {
  private val entries = mutableListOf<RetainedValuesStoreEntry>()
  private var isDisposed = false

  fun acquire(): RetainedValuesStoreLease {
    check(!isDisposed) { "Cannot acquire a retained values store from a disposed slot." }

    val entry =
      entries.firstOrNull { !it.isInUse } ?: RetainedValuesStoreEntry().also { entries += it }
    val leaseToken = Any()
    entry.activeLeaseToken = leaseToken
    return RetainedValuesStoreLease(
      store = entry.store,
      establish = { establish(entry, leaseToken) },
      releaseEntry = { release(entry, leaseToken) },
    )
  }

  fun dispose() {
    if (isDisposed) return
    isDisposed = true
    val entriesToDispose = entries.toList()
    entriesToDispose.forEach { it.isDisposed = true }
    entries.clear()
    entriesToDispose.forEach { entry -> entry.store.dispose() }
  }

  private fun establish(entry: RetainedValuesStoreEntry, leaseToken: Any) {
    if (isDisposed || entry.activeLeaseToken !== leaseToken) return
    entry.isEstablished = true
  }

  private fun release(entry: RetainedValuesStoreEntry, leaseToken: Any) {
    if (entry.activeLeaseToken !== leaseToken) return
    entry.activeLeaseToken = null
    if (isDisposed || entry.isDisposed) return

    if (!entry.isEstablished) {
      dispose(entry)
    }
  }

  private fun dispose(entry: RetainedValuesStoreEntry) {
    if (entry.isDisposed) return
    entry.isDisposed = true
    entries.remove(entry)
    entry.store.dispose()
  }
}

private class RetainedValuesStoreEntry {
  val store = ManagedRetainedValuesStore()
  var isEstablished = false
  var isDisposed = false
  var activeLeaseToken: Any? = null

  val isInUse: Boolean
    get() = activeLeaseToken != null
}

internal class RetainedValuesStoreLease(
  val store: ManagedRetainedValuesStore,
  private val establish: () -> Unit,
  private val releaseEntry: () -> Unit,
) : RememberObserver {
  private var state = State.Pending

  override fun onRemembered() {
    if (state != State.Pending) return
    state = State.Remembered
    establish()
  }

  override fun onForgotten() = release()

  override fun onAbandoned() = release()

  private fun release() {
    if (state == State.Released) return
    state = State.Released
    releaseEntry()
  }

  private enum class State {
    Pending,
    Remembered,
    Released,
  }
}

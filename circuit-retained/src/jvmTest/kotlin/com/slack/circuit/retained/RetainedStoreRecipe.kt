// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainObserver

/**
 * Example implementation of a keyed container for values retained with Compose's first-party
 * `retain` API.
 *
 * `retain` only supports positional identity, so a single call site cannot retain values for a
 * dynamic set of logical entities the way `rememberRetained(key = ...)` can. A [RetainedStore]
 * moves that keying into user space:
 * ```kotlin
 * val store = retain { RetainedStore<ChatId, ChatController>() }
 * val controller = store.getOrCreate(chatId) { ChatController(it) }
 * ```
 *
 * The store forwards the retention lifecycle to its values. Values implementing [RetainObserver]
 * receive [RetainObserver.onRetained] when created, composition enter/exit as the store observes
 * them, and [RetainObserver.onRetired] when removed, cleared, or when the store itself is retired.
 * Values implementing [AutoCloseable] are closed on retirement.
 *
 * Entries created with [getOrCreate] live until removed or the store is retired, so unbounded key
 * spaces must be pruned manually with [remove] or [clear]. Alternatively, [rememberRetainedEntry]
 * tracks how many active compositions reference a key and evicts entries that are no longer
 * referenced, approximating the unclaimed-value GC that `RetainedStateRegistry` performs at frame
 * end.
 */
// This is a reference implementation kept in tests rather than shipped API. It exists to document
// the migration path for keyed `rememberRetained` usages if circuit-retained is ever deprecated in
// favor of first-party `retain`. See RetainedStoreRecipeTest for behavior.
class RetainedStore<K, V : Any> : RetainObserver {

  private class Entry<V : Any>(val value: V, val inputs: Array<out Any?>?) {
    var refCount = 0
    var refCounted = false
  }

  private val entries = mutableMapOf<K, Entry<V>>()
  private var inComposition = false

  /** Keys currently present in the store. */
  val keys: Set<K>
    get() = entries.keys

  /** Returns the value for [key] if present. */
  operator fun get(key: K): V? = entries[key]?.value

  /**
   * Returns the value for [key], creating it with [factory] if absent. If [inputs] are provided and
   * differ from the inputs the existing value was created with, the existing value is retired and a
   * new one is created, matching `rememberRetained(inputs)` semantics.
   */
  fun getOrCreate(key: K, vararg inputs: Any?, factory: (K) -> V): V {
    val existing = entries[key]
    if (existing != null) {
      val inputsChanged =
        inputs.isNotEmpty() && (existing.inputs == null || !existing.inputs.contentEquals(inputs))
      if (!inputsChanged) return existing.value
      retireValue(existing.value)
      entries.remove(key)
    }
    val value = factory(key)
    entries[key] = Entry(value, inputs.takeIf { it.isNotEmpty() })
    if (value is RetainObserver) {
      value.onRetained()
      if (inComposition) value.onEnteredComposition()
    }
    return value
  }

  /**
   * [getOrCreate] plus a composition reference count on the entry. Pair with [release], or use
   * [rememberRetainedEntry] to do both automatically.
   */
  fun acquire(key: K, vararg inputs: Any?, factory: (K) -> V): V {
    val value = getOrCreate(key, *inputs, factory = factory)
    val entry = entries.getValue(key)
    entry.refCount++
    entry.refCounted = true
    return value
  }

  /**
   * Releases one composition reference on [key], where [value] is what the matching [acquire]
   * returned. Releases against a replaced entry (the value no longer matches, such as after an
   * input-driven recreation) are ignored. When the count reaches zero while the store is still in
   * composition, the entry left composition for good (not as part of a store-wide teardown) and is
   * evicted. Releases during teardown (store exited composition) keep the entry so it can be
   * re-acquired after restoration; entries still unreferenced when the store re-enters composition
   * are evicted then.
   */
  fun release(key: K, value: V) {
    val entry = entries[key] ?: return
    if (entry.value !== value) return
    entry.refCount--
    if (entry.refCount <= 0 && inComposition) {
      remove(key)
    }
  }

  /** Removes and retires the value for [key], returning it if it was present. */
  fun remove(key: K): V? {
    val entry = entries.remove(key) ?: return null
    retireValue(entry.value)
    return entry.value
  }

  /** Removes and retires all values. */
  fun clear() {
    val values = entries.values.toList()
    entries.clear()
    values.forEach { retireValue(it.value) }
  }

  override fun onRetained() {
    // Values are notified at creation time instead, since entries are only added via getOrCreate.
  }

  override fun onEnteredComposition() {
    // Refcounted entries nobody re-acquired during restoration are no longer referenced anywhere.
    // This runs at frame end, after restored content has composed and re-acquired its entries.
    // Evicted before inComposition flips so they are retired without a duplicate exited signal;
    // they never re-entered composition. Entries only used via getOrCreate are not tracked and
    // never evicted this way.
    entries.filterValues { it.refCounted && it.refCount <= 0 }.keys.toList().forEach { remove(it) }
    inComposition = true
    entries.values.forEach { (it.value as? RetainObserver)?.onEnteredComposition() }
  }

  override fun onExitedComposition() {
    inComposition = false
    entries.values.forEach { (it.value as? RetainObserver)?.onExitedComposition() }
  }

  override fun onRetired() {
    clear()
  }

  override fun onUnused() {
    // Values were told onRetained at creation, so their terminal signal is retirement.
    clear()
  }

  private fun retireValue(value: V) {
    if (value is RetainObserver) {
      if (inComposition) value.onExitedComposition()
      value.onRetired()
    }
    if (value is AutoCloseable) {
      value.close()
    }
  }
}

/**
 * Retains a value for [key] with a composition-scoped lifetime: the entry stays alive while any
 * composition references it (including across configuration changes, where the store itself is
 * retained), and is evicted once nothing references it anymore.
 */
@Composable
private fun <K, V : Any> RetainedStore<K, V>.rememberRetainedEntry(
  key: K,
  vararg inputs: Any?,
  factory: (K) -> V,
): V {
  val value = remember(this, key, *inputs) { acquire(key, *inputs, factory = factory) }
  DisposableEffect(this, key, *inputs) { onDispose { release(key, value) } }
  return value
}

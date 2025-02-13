// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.navigation.internal

/**
 * A no-op [MutableMap]. This mimics the behavior of Java's Collections.emptyMap(), which silently
 * ignores mutating operations.
 */
internal object NoOpMap : MutableMap<Any, Any> {
  override val size: Int = 0

  @Suppress("UNCHECKED_CAST")
  override val entries: MutableSet<MutableMap.MutableEntry<Any, Any>> =
    NoOpSet as MutableSet<MutableMap.MutableEntry<Any, Any>>
  override val keys: MutableSet<Any> = NoOpSet
  override val values: MutableCollection<Any> = NoOpSet

  override fun clear() {}

  override fun isEmpty() = true

  override fun remove(key: Any) = null

  override fun putAll(from: Map<out Any, Any>) {}

  override fun put(key: Any, value: Any) = null

  override fun get(key: Any) = null

  override fun containsValue(value: Any) = false

  override fun containsKey(key: Any) = false
}

/**
 * A no-op [MutableSet]. This mimics the behavior of Java's Collections.emptySet(), which silently
 * ignores mutating operations.
 */
private object NoOpSet : MutableSet<Any> {
  override fun add(element: Any) = false

  override fun addAll(elements: Collection<Any>) = false

  override val size: Int = 0

  override fun clear() {}

  override fun isEmpty() = true

  override fun containsAll(elements: Collection<Any>) = elements.isEmpty()

  override fun contains(element: Any) = false

  override fun iterator() = NoOpIterator

  override fun retainAll(elements: Collection<Any>) = false

  override fun removeAll(elements: Collection<Any>) = false

  override fun remove(element: Any) = false

  object NoOpIterator : MutableIterator<Any> {
    override fun hasNext() = false

    override fun next(): Any {
      throw UnsupportedOperationException()
    }

    override fun remove() {
      throw UnsupportedOperationException()
    }
  }
}

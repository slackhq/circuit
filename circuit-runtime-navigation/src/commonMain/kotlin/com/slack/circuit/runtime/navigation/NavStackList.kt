// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.navigation

import androidx.compose.runtime.Immutable

/**
 * Represents an immutable point-in-time view of a navigation stack, capturing items and a current
 * position. Provides access to [top] (newest), [active] (active), and [root] (oldest) items, plus
 * [forwardItems] and [backwardItems] sublists for navigation history. An empty [NavStackList] is
 * not allowed.
 *
 * Example with stack `[D, C, B, A]` where D is top, A is root, and current is C:
 * ```
 * top:      D
 * forward:  [D]
 * current:  C  ← current position
 * backward: [B, A]
 * root:     A
 * ```
 *
 * Can be created by the [navStackListOf] factory functions.
 *
 * @param T The type of items in the stack
 */
@Immutable
public interface NavStackList<T> : Iterable<T> {

  /** The top (newest/most recently added) item in the stack. */
  public val top: T

  /** The currently active item in the stack. May differ from [top] when forward history exists. */
  public val active: T

  /** The root (oldest/initial) item in the stack. */
  public val root: T

  /**
   * Items between [active] and [top] (exclusive of current). Empty if no forward history exists.
   */
  public val forwardItems: Iterable<T>

  /**
   * Items between [active] and [root] (exclusive of current). Empty if no backward history exists.
   */
  public val backwardItems: Iterable<T>

  /** Returns an iterator over all items from [top] to [root]. */
  override fun iterator(): Iterator<T>
}

/** Creates a [NavStackList] with a single item (which is top, current, and root). */
public fun <T> navStackListOf(item: T): NavStackList<T> = SingleNavStackList(item)

/**
 * Creates a [NavStackList] from the given items, with the first item as top and last as root. If
 * there are no items, returns null.
 */
public fun <T> navStackListOf(vararg items: T): NavStackList<T>? {
  return when (items.size) {
    0 -> null
    1 -> SingleNavStackList(items[0])
    else -> navStackListOf(items.toList())
  }
}

/**
 * Creates a [NavStackList] from the given items, with the first item as top/current and last as
 * root. If there are no items, returns null.
 */
public fun <T> navStackListOf(items: Iterable<T>): NavStackList<T>? {
  // Check for an empty iterable.
  val iterator = items.iterator()
  if (!iterator.hasNext()) {
    return null
  }
  return DefaultNavStackList(items)
}

/**
 * Creates a [NavStackList] with explicit forward/backward lists around a current item.
 *
 * @param forward Items between current and top (ordered from current toward top)
 * @param current The currently active item
 * @param backward Items between current and root (ordered from current toward root)
 */
public fun <T> navStackListOf(
  forward: List<T> = emptyList(),
  current: T,
  backward: List<T> = emptyList(),
): NavStackList<T> = DefaultNavStackList(forward, current, backward)

/**
 * Transforms each item in this [NavStackList] using the given [transform] function.
 *
 * The transformation is applied lazily when items are accessed. Useful for mapping between
 * different types (e.g., Records to Screens).
 */
public fun <T, R> NavStackList<T>.transform(transform: (T) -> R): NavStackList<R> {
  return MappingNavStackList(this, transform)
}

/** Implementation for a single-item [NavStackList]. */
private data class SingleNavStackList<T>(val item: T) : NavStackList<T> {
  private val list = listOf(item)
  override val top: T = item
  override val active: T = item
  override val root: T = item
  override val forwardItems: Iterable<T> = emptyList()
  override val backwardItems: Iterable<T> = emptyList()

  override fun iterator(): Iterator<T> = list.iterator()
}

/** Default implementation of [NavStackList] backed by lists of items. */
private data class DefaultNavStackList<T>(
  override val forwardItems: Iterable<T>,
  override val active: T,
  override val backwardItems: Iterable<T>,
) : NavStackList<T> {

  constructor(list: Iterable<T>) : this(emptyList(), list.first(), list.drop(1))

  private val list = forwardItems.reversed() + active + backwardItems

  override val top: T = list.first()
  override val root: T = list.last()

  override fun iterator(): Iterator<T> = list.iterator()
}

/** Implementation of [NavStackList] that lazily applies a transform function to items. */
private class MappingNavStackList<T, R>(
  private val original: NavStackList<T>,
  private val transform: (T) -> R,
) : NavStackList<R> {
  override val top: R by lazy(LazyThreadSafetyMode.NONE) { original.top.let(transform) }
  override val active: R by lazy(LazyThreadSafetyMode.NONE) { original.active.let(transform) }
  override val root: R by lazy(LazyThreadSafetyMode.NONE) { original.root.let(transform) }
  override val forwardItems: Iterable<R> by
    lazy(LazyThreadSafetyMode.NONE) { original.forwardItems.map(transform) }

  override val backwardItems: Iterable<R> by
    lazy(LazyThreadSafetyMode.NONE) { original.backwardItems.map(transform) }

  override fun iterator(): Iterator<R> {
    return original.iterator().asSequence().map(transform).iterator()
  }
}

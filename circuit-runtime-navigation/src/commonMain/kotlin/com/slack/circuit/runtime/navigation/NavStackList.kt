// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.navigation

import androidx.compose.runtime.Immutable

/**
 * Represents an immutable point-in-time view of a navigation stack, capturing items and a current
 * position. Provides access to [top] (newest), [active] (current), and [root] (oldest) items, plus
 * [forwardItems] and [backwardItems] sublists for navigation history. An empty [NavStackList] is
 * not allowed.
 *
 * Example with stack `[D, C, B, A]` where D is top, A is root, and current is C:
 * ```
 * top:           D
 * forwardItems:  [D]
 * active:        C  ← current position
 * backwardItems: [B, A]
 * root:          A
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
 * Creates a [NavStackList] from the given items, with the first item as top/active and last as
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
 * Creates a [NavStackList] with explicit forward/backward lists around an active item.
 *
 * @param forwardItems Items between active and top (ordered from active toward top)
 * @param activeItem The currently active item
 * @param backwardItems Items between active and root (ordered from active toward root)
 */
public fun <T> navStackListOf(
  forwardItems: List<T> = emptyList(),
  activeItem: T,
  backwardItems: List<T> = emptyList(),
): NavStackList<T> = DefaultNavStackList(forwardItems, activeItem, backwardItems)

/**
 * Transforms each item in this [NavStackList] using the given [transform] function.
 *
 * The transformation is applied lazily when items are accessed. Useful for mapping between
 * different types (e.g., Records to Screens).
 */
public fun <T, R> NavStackList<T>.transform(transform: (T) -> R): NavStackList<R> {
  return navStackListOf(
    forwardItems = forwardItems.map(transform),
    activeItem = transform(active),
    backwardItems = backwardItems.map(transform),
  )
}

/** Implementation for a single-item [NavStackList]. */
private class SingleNavStackList<T>(item: T) : NavStackList<T> {
  private val list = listOf(item)
  override val top: T = item
  override val active: T = item
  override val root: T = item
  override val forwardItems: Iterable<T> = emptyList()
  override val backwardItems: Iterable<T> = emptyList()

  override fun iterator(): Iterator<T> = list.iterator()

  override fun equals(other: Any?): Boolean = equalsTo(other)

  override fun hashCode(): Int = hashCodeOf()
}

/** Default implementation of [NavStackList] backed by lists of items. */
private class DefaultNavStackList<T>(
  override val forwardItems: Iterable<T>,
  override val active: T,
  override val backwardItems: Iterable<T>,
) : NavStackList<T> {

  constructor(list: Iterable<T>) : this(emptyList(), list.first(), list.drop(1))

  private val list = forwardItems.reversed() + active + backwardItems

  override val top: T = list.first()
  override val root: T = list.last()

  override fun iterator(): Iterator<T> = list.iterator()

  override fun equals(other: Any?): Boolean = equalsTo(other)

  override fun hashCode(): Int = hashCodeOf()
}

private fun NavStackList<*>.hashCodeOf(): Int {
  var result = forwardItems.hashCode()
  result = 31 * result + (active?.hashCode() ?: 0)
  result = 31 * result + backwardItems.hashCode()
  return result
}

private fun NavStackList<*>.equalsTo(other: Any?): Boolean {
  if (this === other) return true
  if (other == null || other !is NavStackList<*>) return false
  if (top != other.top) return false
  if (active != other.active) return false
  if (root != other.root) return false
  if (forwardItems != other.forwardItems) return false
  if (backwardItems != other.backwardItems) return false
  return true
}

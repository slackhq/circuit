package com.slack.circuit.runtime

import androidx.compose.runtime.Immutable

@Immutable
public interface NavStackList<T> : Iterable<T> {

  public val top: T

  public val current: T

  public val root: T

  /** All items that are between the [current] and [top] items (inclusive). */
  public val forward: Iterable<T>

  /** All items that are between the [current] and [root] items (inclusive). */
  public val backward: Iterable<T>

  /** Returns an iterator over all items from [top] to [root]. */
  override fun iterator(): Iterator<T>
}

public fun <T> navStackListOf(item: T): NavStackList<T> = SingletonNavStackList(item)

public fun <T> navStackListOf(vararg items: T): NavStackList<T> = navStackListOf(items.toList())

public fun <T> navStackListOf(items: Iterable<T>): NavStackList<T> = DefaultNavStackList(items)

public fun <T> navStackListOf(
  forward: List<T> = emptyList(),
  current: T,
  backward: List<T> = emptyList(),
): NavStackList<T> = DefaultNavStackList(forward, current, backward)

private data class SingletonNavStackList<T>(val item: T) : NavStackList<T> {
  private val list = listOf(item)
  override val top: T = item
  override val current: T = item
  override val root: T = item
  override val forward: Iterable<T> = emptyList()
  override val backward: Iterable<T> = emptyList()

  override fun iterator(): Iterator<T> = list.iterator()
}

private data class DefaultNavStackList<T>(
  override val forward: Iterable<T>,
  override val current: T,
  override val backward: Iterable<T>,
) : NavStackList<T> {

  constructor(list: Iterable<T>) : this(emptyList(), list.first(), list.drop(1))

  private val list = forward.reversed() + current + backward

  override val top: T = list.first()
  override val root: T = list.last()

  override fun iterator(): Iterator<T> = list.iterator()
}

public fun <T, R> NavStackList<T>.transform(transform: (T) -> R): NavStackList<R> {
  return MappingNavStackList(this, transform)
}

private class MappingNavStackList<T, R>(
  private val original: NavStackList<T>,
  private val transform: (T) -> R,
) : NavStackList<R> {
  override val top: R by lazy(LazyThreadSafetyMode.NONE) { original.top.let(transform) }
  override val current: R by lazy(LazyThreadSafetyMode.NONE) { original.current.let(transform) }
  override val root: R by lazy(LazyThreadSafetyMode.NONE) { original.root.let(transform) }
  override val forward: Iterable<R> by
    lazy(LazyThreadSafetyMode.NONE) { original.forward.map(transform) }

  override val backward: Iterable<R> by
    lazy(LazyThreadSafetyMode.NONE) { original.backward.map(transform) }

  override fun iterator(): Iterator<R> {
    return original.iterator().asSequence().map(transform).iterator()
  }
}

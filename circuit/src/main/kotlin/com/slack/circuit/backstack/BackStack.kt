package com.slack.circuit.backstack

import androidx.compose.runtime.Stable

/**
 * A caller-supplied stack of [Record]s for presentation with the [Navigator] composable. Iteration
 * order is top-first (first element is the top of the stack).
 */
@Stable
interface BackStack<R : BackStack.Record> : Iterable<R> {
  /** The number of records contained in this [BackStack] that will be seen by an iterator. */
  val size: Int

  /**
   * Attempt to pop the top item off of the back stack, returning the popped [Record] if popping was
   * successful or `null` if no entry was popped.
   */
  fun pop(): R?

  interface Record {
    /**
     * A value that identifies this record uniquely, even if it shares the same [route] with another
     * record. This key may be used by [BackStackRecordLocalProvider]s to associate presentation
     * data with a record across composition recreation.
     *
     * [key] MUST NOT change for the life of the record.
     */
    val key: String

    /** The name of the route that should present this record. */
    val route: String
  }
}

/** `true` if the [BackStack] contains no records. [BackStack.firstOrNull] will return `null`. */
val BackStack<*>.isEmpty: Boolean
  get() = size == 0

/** `true` if the [BackStack] contains exactly one record. */
val BackStack<*>.isAtRoot: Boolean
  get() = size == 1

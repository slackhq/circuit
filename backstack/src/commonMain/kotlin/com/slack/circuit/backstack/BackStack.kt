/*
 * Copyright (C) 2022 Adam Powell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit.backstack

import androidx.compose.runtime.Stable
import com.slack.circuit.backstack.BackStack.Record
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/**
 * A caller-supplied stack of [Record]s for presentation with a `Navigator`. Iteration order is
 * top-first (first element is the top of the stack).
 */
@Stable
public interface BackStack<R : Record> : Iterable<R> {
  /** The number of records contained in this [BackStack] that will be seen by an iterator. */
  public val size: Int

  /** The top-most record in the [BackStack], or `null` if the [BackStack] is empty. */
  public val topRecord: R?

  /**
   * Push a new [Record] onto the back stack. The new record will become the top of the stack.
   *
   * @param record The record to push onto the stack.
   * @param resultKey An optional key that would be used to tag a result produced by this record.
   *   The previous record on the stack will receive this key.
   */
  public fun push(record: R, resultKey: String? = null)

  /**
   * Push a new [Screen] onto the back stack. This will be enveloped in a [Record] and the new
   * record will become the top of the stack.
   *
   * @param screen The screen to push onto the stack.
   * @param resultKey An optional key that would be used to tag a result produced by this record.
   *   The previous record on the stack will receive this key.
   */
  public fun push(screen: Screen, resultKey: String? = null)

  /**
   * Attempt to pop the top item off of the back stack, returning the popped [Record] if popping was
   * successful or `null` if no entry was popped.
   *
   * @param result An optional [PopResult] that will be passed to previous record on the stack after
   *   this record is removed.
   */
  public fun pop(result: PopResult? = null): R?

  /**
   * Pop records off the top of the backstack until one is found that matches the given predicate.
   */
  public fun popUntil(predicate: (R) -> Boolean) {
    while (topRecord?.let(predicate) == false) pop()
  }

  /**
   * Saves the current back stack entry list in an internal state store. It can be later restored by
   * the root screen to [restoreState].
   *
   * This call will overwrite any existing stored state with the same root screen.
   */
  public fun saveState()

  /**
   * Restores the saved state with the given [screen], adding it on top of the existing entry list.
   * If you wish to replace the current entry list, you should [pop] all of the existing entries off
   * before calling this function.
   *
   * @param screen The root screen which was previously saved using [saveState].
   * @return Returns true if there was any back stack state to restore.
   */
  public fun restoreState(screen: Screen): Boolean

  @Stable
  public interface Record {
    /**
     * A value that identifies this record uniquely, even if it shares the same [screen] with
     * another record. This key may be used by [BackStackRecordLocalProvider]s to associate
     * presentation data with a record across composition recreation.
     *
     * [key] MUST NOT change for the life of the record.
     */
    public val key: String

    /** The [Screen] that should present this record. */
    public val screen: Screen

    /**
     * Awaits a [PopResult] produced by the record that previously sat on top of the stack above
     * this one. Returns null if no result was produced.
     *
     * @param key The key that was used to tag the result. This ensures that only the caller that
     *   requested a result when pushing the previous record can receive it.
     */
    public suspend fun awaitResult(key: String): PopResult?
  }
}

/** `true` if the [BackStack] contains no records. [BackStack.firstOrNull] will return `null`. */
public val BackStack<out Record>.isEmpty: Boolean
  get() = size == 0

/** `true` if the [BackStack] contains exactly one record. */
public val BackStack<out Record>.isAtRoot: Boolean
  get() = size == 1

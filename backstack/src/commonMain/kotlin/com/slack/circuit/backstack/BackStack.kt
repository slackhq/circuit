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
import androidx.compose.runtime.snapshots.Snapshot
import com.slack.circuit.backstack.BackStack.Record
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

  /** The bottom-most record in the [BackStack], or `null` if the [BackStack] is empty. */
  public val rootRecord: R?

  /**
   * Push a new [Record] onto the back stack. The new record will become the top of the stack.
   *
   * @param record The record to push onto the stack.
   * @return If the [record] was successfully pushed onto the back stack
   */
  public fun push(record: R): Boolean

  /**
   * Push a new [Screen] onto the back stack. This will be enveloped in a [Record] and the new
   * record will become the top of the stack.
   *
   * @param screen The screen to push onto the stack.
   * @return If the [screen] was successfully pushed onto the back stack
   */
  public fun push(screen: Screen): Boolean

  /**
   * Attempt to pop the top item off of the back stack, returning the popped [Record] if popping was
   * successful or `null` if no entry was popped.
   */
  public fun pop(): R?

  /**
   * Pop records off the top of the backstack until one is found that matches the given predicate.
   */
  public fun popUntil(predicate: (R) -> Boolean): List<R> {
    return buildList {
      while (topRecord?.let(predicate) == false) {
        val popped = pop() ?: break
        add(popped)
      }
    }
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

  /**
   * Peek at the [Screen] in the internal state store that have been saved using [saveState].
   *
   * @return The list of [Screen]s currently in the internal state store, will be empty if there is
   *   no saved state.
   */
  public fun peekState(): List<Screen>

  /**
   * Removes the state associated with the given [screen] from the internal state store.
   *
   * @return true if the state was removed, false if no state was found for the given screen.
   */
  public fun removeState(screen: Screen): Boolean

  /**
   * Whether the back stack contains the given [record].
   *
   * @param includeSaved Whether to also check if the record is contained by any saved back stack
   *   state. See [saveState].
   */
  public fun containsRecord(record: R, includeSaved: Boolean): Boolean

  /**
   * Whether a record with the given [key] is reachable within the back stack or saved state.
   * Reachable means that it is either currently in the visible back stack or if we popped `depth`
   * times, it would be found.
   *
   * @param key The record's key to look for.
   * @param depth How many records to consider popping from the top of the stack before considering
   *   the key unreachable. A depth of zero means only check the current visible stack. A depth of 1
   *   means check the current visible stack plus one record popped off the top, and so on.
   * @param includeSaved Whether to also check if the record is contained by any saved back stack
   *   state. See [saveState].
   */
  public fun isRecordReachable(key: String, depth: Int, includeSaved: Boolean): Boolean

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
  }
}

/** `true` if the [BackStack] contains no records. [Iterable.firstOrNull] will return `null`. */
public val BackStack<out Record>.isEmpty: Boolean
  get() = size == 0

/** `true` if the [BackStack] contains exactly one record. */
public val BackStack<out Record>.isAtRoot: Boolean
  get() = size == 1

/** Clear any saved state from the [BackStack]. */
public fun BackStack<out Record>.clearState() {
  Snapshot.withMutableSnapshot {
    for (screen in peekState()) {
      removeState(screen)
    }
  }
}

package com.slack.circuit.backstack

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.Snapshot
import com.slack.circuit.backstack.NavStack.Record
import com.slack.circuit.runtime.screen.Screen

/**
 * A navigation stack that supports forward navigation in addition to backward navigation. This is
 * the base interface for navigation stacks in Circuit.
 */
@Stable
public interface NavStack<R : Record> {
  /** The number of records in the stack. */
  public val size: Int

  /** The top-most record in the stack, including the or `null` if the stack is empty. */
  public val topRecord: R?

  /** The active record in the stack, or `null` if the stack is empty. */
  public val currentRecord: R?

  /** The bottom-most record in the stack, or `null` if the stack is empty. */
  public val rootRecord: R?

  public fun add(screen: Screen): Boolean

  public fun add(record: R): Boolean

  /**
   * Removes and returns the top record from the stack and then moves the stack one position in the
   * specified direction.
   *
   * @param direction The direction of removal (Forward or Backward).
   * @return The removed record, or null if the stack is empty or removal is not possible.
   */
  public fun remove(direction: Direction): R?

  public fun move(direction: Direction): Boolean

  /**
   * Saves the current stack entry list in an internal state store. It can be later restored by the
   * root screen to [restoreState].
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
   * @return Returns true if there was any stack state to restore.
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
   * Whether the stack contains the given [record].
   *
   * @param includeSaved Whether to also check if the record is contained by any saved stack state.
   *   See [saveState].
   */
  public fun containsRecord(record: R, includeSaved: Boolean): Boolean

  /**
   * Whether a record with the given [key] is reachable within the stack or saved state. Reachable
   * means that it is either currently in the visible stack or if we popped `depth` times, it would
   * be found.
   *
   * @param key The record's key to look for.
   * @param depth How many records to consider popping from the top of the stack before considering
   *   the key unreachable. A depth of zero means only check the current visible stack. A depth of 1
   *   means check the current visible stack plus one record popped off the top, and so on.
   * @param includeSaved Whether to also check if the record is contained by any saved stack state.
   *   See [saveState].
   */
  public fun isRecordReachable(key: String, depth: Int, includeSaved: Boolean): Boolean

  public enum class Direction {
    Forward,
    Backward,
  }

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

public val NavStack<out Record>.currentScreen: Screen?
  get() = currentRecord?.screen

public val NavStack<out Record>.isEmpty: Boolean
  get() = size == 0

public val NavStack<out Record>.lastIndex: Int
  get() = size - 1

public val NavStack<out Record>.isAtRoot: Boolean
  get() = size == 1

public val NavStack<out Record>.isAtTop: Boolean
  get() = currentRecord == topRecord

public val NavStack<out Record>.canGoBack: Boolean
  get() = currentRecord != rootRecord

public val NavStack<out Record>.canGoForward: Boolean
  get() = currentRecord != topRecord

/** Clear any saved state from the [BackStack]. */
public fun NavStack<out Record>.clearState() {
  Snapshot.withMutableSnapshot {
    for (screen in peekState()) {
      removeState(screen)
    }
  }
}

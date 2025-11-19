package com.slack.circuit.backstack

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.Snapshot
import com.slack.circuit.backstack.NavStack.Record
import com.slack.circuit.runtime.NavStackList
import com.slack.circuit.runtime.screen.Screen

/**
 * A navigation stack supporting bidirectional navigation with browser-style forward/backward
 * traversal.
 *
 * Manages [Record]s in a list with position tracking, enabling navigation without modifying the
 * stack structure. Key positions are [topRecord] (newest), [currentRecord] (active), and
 * [rootRecord] (oldest).
 *
 * Supports multiple independent nav stacks (e.g., bottom nav tabs) via [saveState], [restoreState],
 * [peekState], and [removeState]. State is keyed by root screen.
 *
 * @see SaveableNavStack for the primary implementation
 * @see NavStackList for immutable snapshots
 */
@Stable
public interface NavStack<R : Record> {
  /** The number of records in the stack. */
  public val size: Int

  /** The top-most (newest) record, or null if empty. Always the most recently added record. */
  public val topRecord: R?

  /**
   * The currently active record, or null if empty. May differ from [topRecord] when navigated
   * backward.
   */
  public val currentRecord: R?

  /** The bottom-most (oldest) record, or null if empty. Typically the initial root screen. */
  public val rootRecord: R?

  /**
   * Adds a screen to the stack. Truncates forward history if not at top.
   *
   * @return true if added, false otherwise
   */
  public fun push(screen: Screen): Boolean

  /**
   * Adds a record to the stack. Truncates forward history if not at top.
   *
   * @return true if added, false otherwise
   */
  public fun push(record: R): Boolean

  /**
   * Removes and returns the current record, truncating forward history.
   *
   * @return The removed record, or null if empty
   */
  public fun pop(): R?

  /**
   * Pops records until one matches the predicate.
   *
   * @return List of popped records
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
   * Move forward in navigation history towards the [topRecord].
   *
   * @return true if moved, false otherwise.
   */
  public fun forward(): Boolean

  /**
   * Move backward in navigation history towards the [rootRecord].
   *
   * @return true if moved, false otherwise.
   */
  public fun backward(): Boolean

  /**
   * Creates an immutable snapshot of the current stack state.
   *
   * @return [NavStackList] of current state, or null if empty.
   */
  public fun snapshot(): NavStackList<R>?

  /** Saves the current stack to an internal store, keyed by the root screen. */
  public fun saveState()

  /**
   * Restores previously saved state for the given root [screen], replacing the current stack.
   *
   * @return true if state was restored, false if no saved state found
   */
  public fun restoreState(screen: Screen): Boolean

  /**
   * Returns list of root screens that have saved state.
   *
   * @return List of screens with saved state, empty if none.
   */
  public fun peekState(): List<Screen>

  /**
   * Removes saved state for the given [screen].
   *
   * @return true if state was removed, false otherwise.
   */
  public fun removeState(screen: Screen): Boolean

  /**
   * Checks if the stack contains the given [record].
   *
   * @param includeSaved Whether to also check saved stack states
   */
  public fun containsRecord(record: R, includeSaved: Boolean): Boolean

  /**
   * Checks if a record with the given [key] is reachable within [depth] pops from current position.
   *
   * @param key The record key to find
   * @param depth Depth to search (0 = the current record, 1 = single record before and after)
   * @param includeSaved Whether to also check saved states
   */
  public fun isRecordReachable(key: String, depth: Int, includeSaved: Boolean): Boolean

  /**
   * A record in the navigation stack, wrapping a [Screen] with a unique identity.
   *
   * Each record has a stable [key] for identity tracking across configuration changes and state
   * restoration.
   */
  @Stable
  public interface Record {
    /**
     * Unique identifier for this record. Remains stable across configuration changes and must not
     * change for the life of the record. Used to associate retained and saved data with records.
     */
    public val key: String

    /** The [Screen] that this record presents. */
    public val screen: Screen
  }
}

/** The screen of the current record, or null if empty. */
public val NavStack<out Record>.currentScreen: Screen?
  get() = currentRecord?.screen

/** True if the stack is empty. */
public val NavStack<out Record>.isEmpty: Boolean
  get() = size == 0

/** The index of the last record in the stack. */
public val NavStack<out Record>.lastIndex: Int
  get() = size - 1

/** True if the current position is at the root. */
public val NavStack<out Record>.isAtRoot: Boolean
  get() = currentRecord == rootRecord

/** True if the current position is at the top. */
public val NavStack<out Record>.isAtTop: Boolean
  get() = currentRecord == topRecord

/** True if we can navigate backwards (not at root). */
public val NavStack<out Record>.canGoBack: Boolean
  get() = currentRecord != rootRecord

/** True if we can navigate forwards (not at top). */
public val NavStack<out Record>.canGoForward: Boolean
  get() = currentRecord != topRecord

/** Clears all saved state from the stack. */
public fun NavStack<out Record>.clearState() {
  Snapshot.withMutableSnapshot {
    for (screen in peekState()) {
      removeState(screen)
    }
  }
}

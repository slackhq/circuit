package com.slack.circuit.backstack

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.Snapshot
import com.slack.circuit.backstack.NavStack.Record
import com.slack.circuit.runtime.screen.Screen

/**
 * A navigation stack that supports forward navigation in addition to backward navigation. This is
 * the base interface for navigation stacks in Circuit.
 *
 * ## Navigation Model
 *
 * NavStack maintains a list of records with a current position, supporting browser-style
 * navigation:
 * - **topRecord**: The newest entry (most recently added)
 * - **currentRecord**: The currently visible/active entry
 * - **rootRecord**: The oldest entry (initial screen)
 *
 * Navigation operations:
 * - **add()**: Adds a new entry at the top and truncates forward history
 * - **remove()**: Removes the current entry (and forward history), makes the next entry current
 * - **move()**: Changes position without modifying the stack
 * - **snapshot()**: Captures the current state immutably
 *
 * ## Example Usage
 *
 * ```kotlin
 * val navStack = rememberSaveableNavStack(HomeScreen)
 *
 * // Build up a stack: [Home, Profile, Settings]
 * navStack.add(ProfileScreen)
 * navStack.add(SettingsScreen)
 * // Stack: [Settings (current), Profile, Home]
 *
 * // Navigate backward through history
 * navStack.move(Direction.Backward)
 * // Stack: [Settings, Profile (current), Home]
 *
 * navStack.move(Direction.Backward)
 * // Stack: [Settings, Profile, Home (current)]
 *
 * // Navigate forward
 * navStack.move(Direction.Forward)
 * // Stack: [Settings, Profile (current), Home]
 *
 * // Adding a new screen truncates forward history
 * navStack.add(NotificationsScreen)
 * // Stack: [Notifications (current), Profile, Home]
 * // Settings was removed from forward history
 *
 * // Remove current entry (also truncates forward history if any)
 * navStack.remove()
 * // Stack: [Profile (current), Home]
 * ```
 *
 * @see SaveableNavStack for a concrete implementation
 */
@Stable
public interface NavStack<R : Record> {
  /** The number of records in the stack. */
  public val size: Int

  /**
   * The top-most (newest) record in the stack, or `null` if the stack is empty. This is always the
   * most recently added record, regardless of the current position.
   */
  public val topRecord: R?

  /**
   * The currently active record in the stack, or `null` if the stack is empty. This is the record
   * at the current navigation position, which may differ from [topRecord] when the user has
   * navigated backward.
   */
  public val currentRecord: R?

  /**
   * The bottom-most (oldest) record in the stack, or `null` if the stack is empty. This is
   * typically the initial root screen of the navigation flow.
   */
  public val rootRecord: R?

  /**
   * Adds a screen to the stack, wrapping it in a new [Record].
   *
   * Implementations may truncate forward history when adding from a non-top position, similar to
   * browser navigation behavior.
   *
   * @param screen The screen to add
   * @return true if the screen was added, false if it was a duplicate of the current top
   */
  public fun push(screen: Screen): Boolean

  /**
   * Adds a record to the stack.
   *
   * Implementations may truncate forward history when adding from a non-top position, similar to
   * browser navigation behavior.
   *
   * @param record The record to add
   * @return true if the record was added, false if it was a duplicate of the current top
   */
  public fun push(record: R): Boolean

  /**
   * Removes and returns the current record from the stack.
   *
   * When called from a non-top position, this operation:
   * 1. Truncates any forward history (entries between top and current)
   * 2. Removes the current record
   * 3. Sets the new top as current
   *
   * This provides browser-like "remove and discard forward history" behavior.
   *
   * @return The removed record, or null if the stack is empty
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

  public fun forward(): Boolean

  public fun backward(): Boolean

  /**
   * Creates a snapshot of the current stack state, including all entries and the current position.
   *
   * The returned snapshot is immutable and can be used to capture the current navigation state for
   * inspection, serialization, or other purposes.
   *
   * @return A [Snapshot] containing the current entries and position
   */
  public fun snapshot(): Snapshot<R>

  /**
   * Saves the current stack entry list and position in an internal state store. It can be later
   * restored by the root screen to [restoreState].
   *
   * The saved state includes both the list of records and the current position, allowing for
   * complete restoration of the navigation state.
   *
   * This call will overwrite any existing stored state with the same root screen.
   */
  public fun saveState()

  /**
   * Restores the saved state with the given [screen], adding it on top of the existing entry list.
   * If you wish to replace the current entry list, you should [pop] all of the existing entries
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

  @Stable
  public interface Record {
    /**
     * A value that identifies this record uniquely, even if it shares the same [screen] with
     * another record. This key may be used by [NavStackRecordLocalProvider]s to associate
     * presentation data with a record across composition recreation.
     *
     * [key] MUST NOT change for the life of the record.
     */
    public val key: String

    /** The [Screen] that should present this record. */
    public val screen: Screen
  }

  /**
   * An immutable snapshot of a navigation stack's state at a point in time.
   *
   * ## Structure
   *
   * A snapshot captures the complete state using a **List+Index model**:
   * - **entries**: An immutable list of all records in the stack
   * - **currentIndex**: The position of the currently active record
   *
   * ## Index Semantics
   *
   * The list is ordered from newest (index 0) to oldest (last index):
   * ```
   * entries:  [TopScreen, MiddleScreen, RootScreen]
   * indices:   0          1             2
   *            ^                        ^
   *          newest                  oldest
   * ```
   * - `currentIndex = 0`: At the top (newest entry), no forward history
   * - `currentIndex = lastIndex`: At the root (oldest entry), no backward history
   * - `0 < currentIndex < lastIndex`: In the middle, can move both directions
   *
   * @see NavStack.snapshot
   * @see NavStack.saveState
   * @see NavStack.restoreState
   */
  @Stable
  public interface Snapshot<R : Record> : Iterable<R> {
    /**
     * All records in the stack, ordered from top (newest, index 0) to root (oldest, last index).
     */
    public val entries: List<R>

    /**
     * The index of the currently active record in [entries].
     * - `0`: At the top (newest entry)
     * - `entries.lastIndex`: At the root (oldest entry)
     * - Between: In the middle of the history
     */
    public val currentIndex: Int

    /** Retrieves the record at the given [index] in [entries]. */
    public operator fun get(index: Int): R = entries[index]

    /** Returns an iterator over all entries from top (index 0) to root (last index). */
    override fun iterator(): Iterator<R> = entries.iterator()

    public val current: R
      get() = entries[currentIndex]
  }
}

public val NavStack<out Record>.currentScreen: Screen?
  get() = currentRecord?.screen

public val NavStack<out Record>.isEmpty: Boolean
  get() = size == 0

public val NavStack<out Record>.lastIndex: Int
  get() = size - 1

public val NavStack<out Record>.isAtRoot: Boolean
  get() = currentRecord == rootRecord

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

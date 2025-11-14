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
  public fun add(screen: Screen): Boolean

  /**
   * Adds a record to the stack.
   *
   * Implementations may truncate forward history when adding from a non-top position, similar to
   * browser navigation behavior.
   *
   * @param record The record to add
   * @return true if the record was added, false if it was a duplicate of the current top
   */
  public fun add(record: R): Boolean

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
  public fun remove(): R?

  /**
   * Moves the current position in the stack without removing any records.
   * - **Direction.Forward**: Moves toward [topRecord] (newer entries)
   * - **Direction.Backward**: Moves toward [rootRecord] (older entries)
   *
   * Use [canGoForward] and [canGoBack] to check if movement is possible.
   *
   * @param direction The direction to move
   * @return true if the move was successful, false if already at the boundary
   */
  public fun move(direction: Direction): Boolean

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
   * If you wish to replace the current entry list, you should [remove] all of the existing entries
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

  /**
   * Navigation direction within a stack.
   * - **Forward**: Toward [topRecord] (newer/more recent entries)
   * - **Backward**: Toward [rootRecord] (older/earlier entries)
   *
   * This mirrors browser history navigation where Forward goes to newer pages and Backward goes to
   * older pages.
   */
  public enum class Direction {
    /** Navigate toward the top (newer entries) of the stack. */
    Forward,
    /** Navigate toward the root (older entries) of the stack. */
    Backward,
  }

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
   *
   * - `currentIndex = 0`: At the top (newest entry), no forward history
   * - `currentIndex = lastIndex`: At the root (oldest entry), no backward history
   * - `0 < currentIndex < lastIndex`: In the middle, can move both directions
   *
   * ## Navigation View
   *
   * The snapshot provides convenient methods to view the stack from a navigation perspective:
   * - **[forwardStack]**: Entries between current and top (newer entries you can navigate to)
   * - **[backwardStack]**: Entries from current to root (includes current, older entries)
   *
   * ```
   * Stack: [A, B, C, D, E]  (currentIndex = 2, current = C)
   *         ^        ^  ^
   *       top      cur root
   *
   * forwardStack()  = [B, A]     // Can move forward to these
   * backwardStack() = [C, D, E]  // Current and entries behind
   * ```
   *
   * ## Immutability
   *
   * Snapshots are immutable and will not change even if the source [NavStack] is modified. This
   * makes them safe to:
   * - Pass across composition boundaries
   * - Store for later comparison
   * - Serialize for state restoration
   * - Iterate over without synchronization
   *
   * ## Use Cases
   *
   * 1. **State Persistence**: Save via [saveState] and restore via [restoreState]
   * 2. **Inspection**: Examine stack contents without modifying the live stack
   * 3. **Iteration**: Use the [Iterable] interface to traverse all entries
   * 4. **Testing**: Verify navigation state in unit tests
   * 5. **UI Display**: Show forward/backward history in navigation UI
   *
   * ## Example
   *
   * ```kotlin
   * val snapshot = navStack.snapshot()
   *
   * // Check position
   * val isAtTop = snapshot.currentIndex == 0
   * val isAtRoot = snapshot.currentIndex == snapshot.entries.lastIndex
   *
   * // Access entries
   * val current = snapshot[snapshot.currentIndex]
   * val top = snapshot[0]
   *
   * // Iterate over all entries (top to root)
   * snapshot.forEach { record ->
   *   println("Screen: ${record.screen}")
   * }
   *
   * // View navigation stacks
   * val canGoForward = snapshot.forwardStack().any()
   * snapshot.forwardStack().forEach { record ->
   *   println("Can navigate forward to: ${record.screen}")
   * }
   * ```
   *
   * @see NavStack.snapshot
   * @see NavStack.saveState
   * @see NavStack.restoreState
   */
  @Stable
  public interface Snapshot<R : Record> : Iterable<R> {
    /** All records in the stack, ordered from top (newest, index 0) to root (oldest, last index). */
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

    /**
     * Returns entries between the current position and the top, in order from nearest to top.
     * These are entries you can navigate forward to (toward newer entries).
     *
     * Empty when [currentIndex] is 0 (at top).
     *
     * Example:
     * ```
     * entries = [A, B, C, D], currentIndex = 2 (at C)
     * forwardStack() = [B, A]  // Can move forward through B to reach A
     * ```
     */
    public fun forwardStack(): Iterable<R> = entries.subList(0, currentIndex).asReversed()

    /**
     * Returns entries from the current position to the root, in order from current to root.
     * This includes the current entry and all entries you can navigate backward to (toward older
     * entries).
     *
     * Example:
     * ```
     * entries = [A, B, C, D], currentIndex = 1 (at B)
     * backwardStack() = [B, C, D]  // Current B, can move back through C to D
     * ```
     */
    public fun backwardStack(): Iterable<R> = entries.subList(currentIndex, entries.size)
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

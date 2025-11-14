package com.slack.circuit.backstack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.slack.circuit.backstack.SaveableNavStack.Record
import com.slack.circuit.runtime.screen.Screen
import kotlin.collections.set
import kotlin.math.min
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Creates and remembers a [SaveableNavStack] with the given [root] screen.
 *
 * If [root] changes, a new nav stack will be created.
 *
 * @param init optional initializer callback to perform extra initialization logic.
 */
@Composable
public fun rememberSaveableNavStack(
  root: Screen,
  init: SaveableNavStack.() -> Unit = {},
): SaveableNavStack =
  rememberSaveable(root, saver = SaveableNavStack.Saver) { SaveableNavStack(root).apply(init) }

/**
 * Creates and remembers a [SaveableNavStack] filled with the given [initialScreens].
 *
 * [initialScreens] must not be empty. If [initialScreens] changes, a new nav stack will be created.
 */
@Composable
public fun rememberSaveableNavStack(initialScreens: List<Screen>): SaveableNavStack {
  require(initialScreens.isNotEmpty()) { "Initial input screens cannot be empty!" }
  return rememberSaveable(initialScreens, saver = SaveableNavStack.Saver) {
    SaveableNavStack().apply {
      for (screen in initialScreens) {
        add(screen)
      }
    }
  }
}

/**
 * A [NavStack] that supports saving its state via [rememberSaveable]. See
 * [rememberSaveableNavStack].
 *
 * Unlike [SaveableBackStack], this implementation supports forward navigation, removal in both
 * directions, and moving through the stack without removing entries.
 *
 * ## Internal Structure
 *
 * The stack maintains an [entryList] of records and a [currentIndex] to track position:
 * ```
 * entryList indices:  [0,    1,    2,    3,    4,    5]
 *                      ^                        ^     ^
 *                    topRecord            currentIndex rootRecord
 *                    (newest)               (current) (oldest)
 * ```
 *
 * ## Navigation Examples
 *
 * ### Moving Forward (toward top/newer entries):
 * ```
 * Before:  [A, B, C, D, E, F]
 *                    ^
 *              currentIndex = 3
 *
 * move(Forward)
 *
 * After:   [A, B, C, D, E, F]
 *                 ^
 *              currentIndex = 2
 * ```
 *
 * ### Moving Backward (toward root/older entries):
 * ```
 * Before:  [A, B, C, D, E, F]
 *                 ^
 *              currentIndex = 2
 *
 * move(Backward)
 *
 * After:   [A, B, C, D, E, F]
 *                    ^
 *              currentIndex = 3
 * ```
 *
 * ### Adding New Entry (truncates forward history):
 * ```
 * Before:  [A, B, C, D, E, F]
 *                 ^
 *              currentIndex = 2 (pointing to C)
 *
 * add(G)
 *
 * After:   [G, C, D, E, F]
 *           ^
 *        currentIndex = 0 (pointing to G)
 *        (A & B removed, G becomes new top and current)
 * ```
 *
 * ### Removing with Direction:
 * ```
 * # Forward removal (removes current, moves toward top)
 * Before:  [A, B, C, D, E, F]
 *                    ^
 *              currentIndex = 3
 *
 * remove(Forward)
 *
 * After:   [A, B, C, E, F]
 *                 ^
 *              currentIndex = 2
 *
 * # Backward removal (removes current, stays at same logical position)
 * Before:  [A, B, C, D, E, F]
 *                    ^
 *              currentIndex = 3
 *
 * remove(Backward)
 *
 * After:   [A, B, C, E, F]
 *                    ^
 *              currentIndex = 3 (now points to E)
 * ```
 */
public class SaveableNavStack
internal constructor(
  // Both visible for testing
  internal val entryList: SnapshotStateList<Record> = mutableStateListOf(),
  internal val stateStore: MutableMap<Screen, NavStack.Snapshot<Record>> = mutableMapOf(),
  initialIndex: Int = -1,
) : NavStack<Record>, Iterable<Record> {

  // Track the current position in the stack (index into entryList)
  // Index 0 is the top (newest), size-1 is the root (oldest)
  private var currentIndex by mutableIntStateOf(initialIndex)

  public constructor(root: Screen) : this(Record(root))

  public constructor(root: Record) : this() {
    add(root)
    // Set current to the new top
    currentIndex = 0
  }

  override val size: Int
    get() = entryList.size

  override fun iterator(): Iterator<Record> = entryList.iterator()

  override val topRecord: Record?
    get() = entryList.firstOrNull()

  override val currentRecord: Record?
    get() = entryList.getOrNull(currentIndex)

  override val rootRecord: Record?
    get() = entryList.lastOrNull()

  override fun add(screen: Screen): Boolean {
    return add(Record(screen))
  }

  override fun add(record: Record): Boolean {
    val topRecord = Snapshot.withoutReadObservation { topRecord }
    // Guard pushing the exact same record value to the top, records.key is always unique so verify
    // the parameters individually.
    return if (topRecord?.screen != record.screen || topRecord.args != record.args) {
      // When adding a new record, truncate any entries above the current position (forward history)
      if (currentIndex > 0) {
        // Remove all entries before currentIndex
        repeat(currentIndex) { entryList.removeAt(0) }
      }
      // Add the new record at the top
      entryList.add(0, record)
      // Set current to the new top
      currentIndex = 0
      true
    } else false
  }

  override fun remove(): Record? {
    if (currentIndex < 0 || entryList.isEmpty()) return null
    return Snapshot.withMutableSnapshot {
      // When removing the current record, truncate any entries above the current position (forward
      // history)
      if (currentIndex > 0) {
        // Remove all entries before currentIndex
        repeat(currentIndex) { entryList.removeAt(0) }
      }
      // Set current to the new top
      currentIndex = 0
      // Remove the current record
      entryList.removeAt(0)
    }
  }

  override fun move(direction: NavStack.Direction): Boolean {
    return when (direction) {
      NavStack.Direction.Forward -> {
        // Move forward (toward top, decrease index)
        if (currentIndex > 0) {
          currentIndex--
          true
        } else false
      }
      NavStack.Direction.Backward -> {
        // Move backward (toward root, increase index)
        if (currentIndex < entryList.lastIndex) {
          currentIndex++
          true
        } else false
      }
    }
  }

  override fun snapshot(): NavStack.Snapshot<Record> {
    return SaveableSnapshot(entryList.toList(), currentIndex)
  }

  override fun saveState() {
    if (entryList.isNotEmpty()) {
      val rootScreen = entryList.last().screen
      stateStore[rootScreen] = SaveableSnapshot(entryList.toList(), currentIndex)
    }
  }

  override fun restoreState(screen: Screen): Boolean {
    val stored = stateStore[screen]
    if (stored != null && stored.entries.isNotEmpty()) {
      // Add the stored state into the entry list
      entryList.addAll(stored.entries)
      // Restore the current index
      currentIndex = stored.currentIndex
      // Clear the stored state
      stateStore.remove(screen)
      return true
    }
    return false
  }

  override fun peekState(): List<Screen> {
    return stateStore.keys.toList()
  }

  override fun removeState(screen: Screen): Boolean {
    return stateStore.remove(screen) != null
  }

  override fun containsRecord(record: Record, includeSaved: Boolean): Boolean {
    // If it's in the main entry list, return true
    if (record in entryList) return true

    if (includeSaved && stateStore.isNotEmpty()) {
      // If we're checking our saved snapshots too, iterate through them and check
      for (snapshot in stateStore.values) {
        if (record in snapshot.entries) return true
      }
    }
    return false
  }

  override fun isRecordReachable(key: String, depth: Int, includeSaved: Boolean): Boolean {
    if (depth < 0) return false
    // Check in the current entry list
    for (i in 0 until min(depth, entryList.size)) {
      if (entryList[i].key == key) return true
    }
    // If includeSaved, check saved stack states too
    if (includeSaved && stateStore.isNotEmpty()) {
      val storedSnapshots = stateStore.values
      for ((i, snapshot) in storedSnapshots.withIndex()) {
        if (i >= depth) break
        // snapshot.entries is immutable, so safely get the record.
        if (snapshot.entries.getOrNull(i)?.key == key) return true
      }
    }
    return false
  }

  public data class Record(
    override val screen: Screen,
    val args: Map<String, Any?> = emptyMap(),
    @OptIn(ExperimentalUuidApi::class) override val key: String = Uuid.random().toString(),
  ) : NavStack.Record {

    internal companion object {
      val Saver: Saver<Record, Any> =
        mapSaver(
          save = { value ->
            buildMap {
              put("screen", value.screen)
              put("args", value.args)
              put("key", value.key)
            }
          },
          restore = { map ->
            @Suppress("UNCHECKED_CAST")
            Record(
              screen = map["screen"] as Screen,
              args = map["args"] as Map<String, Any?>,
              key = map["key"] as String,
            )
          },
        )
    }
  }

  public data class SaveableSnapshot(
    override val entries: List<Record>,
    override val currentIndex: Int,
  ) : NavStack.Snapshot<Record> {

    internal companion object {
      val Saver: Saver<NavStack.Snapshot<Record>, Any> =
        mapSaver(
          save = { value ->
            buildMap {
              with(Record.Saver) { put("entries", value.entries.mapNotNull { save(it) }) }
              put("currentIndex", value.currentIndex)
            }
          },
          restore = { map ->
            @Suppress("UNCHECKED_CAST")
            SaveableSnapshot(
              entries = (map["entries"] as List<List<Any>>).mapNotNull { Record.Saver.restore(it) },
              currentIndex = map["currentIndex"] as Int,
            )
          },
        )
    }
  }

  internal companion object {
    @Suppress("UNCHECKED_CAST")
    val Saver =
      listSaver<SaveableNavStack, List<Any?>>(
        save = { value ->
          buildList {
            // Add the current index
            add(listOf(value.currentIndex))
            // Save the entry list
            with(Record.Saver) { add(value.entryList.mapNotNull { save(it) }) }
            // Now add any snapshots from the state store
            with(SaveableSnapshot.Saver) { add(value.stateStore.values.map { save(it) }) }
          }
        },
        restore = { value ->
          var currentIndex = -1
          SaveableNavStack().also { navStack ->
            value.forEachIndexed { index, item ->
              when (index) {
                0 -> {
                  // The first list is the current index
                  currentIndex = item.first() as Int
                }
                1 -> {
                  // The second list is the entry list
                  item.mapNotNullTo(navStack.entryList) { Record.Saver.restore(it as List<Any>) }
                }
                else -> {
                  // Any list after that is from the state store (as snapshots)
                  item
                    .mapNotNull { SaveableSnapshot.Saver.restore(it as List<Any>) }
                    .forEach { snapshot ->
                      // The key is always the root screen (i.e. last item)
                      navStack.stateStore[snapshot.entries.last().screen] = snapshot
                    }
                }
              }
            }
            navStack.currentIndex = currentIndex
          }
        },
      )
  }
}

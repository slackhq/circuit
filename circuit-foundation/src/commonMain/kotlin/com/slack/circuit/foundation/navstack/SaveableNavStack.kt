// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.navstack

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
import com.slack.circuit.foundation.navstack.SaveableNavStack.Record
import com.slack.circuit.runtime.navigation.NavStack
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.screen.Screen
import kotlin.collections.set
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
): NavStack<out NavStack.Record> =
  rememberSaveable(root, saver = SaveableNavStack.Saver) { SaveableNavStack(root).apply(init) }

/**
 * Creates and remembers a [SaveableNavStack] filled with the given [initialScreens].
 *
 * [initialScreens] must not be empty. If [initialScreens] changes, a new nav stack will be created.
 */
@Composable
public fun rememberSaveableNavStack(initialScreens: List<Screen>): NavStack<out NavStack.Record> {
  require(initialScreens.isNotEmpty()) { "Initial input screens cannot be empty!" }
  return rememberSaveable(initialScreens, saver = SaveableNavStack.Saver) {
    SaveableNavStack().apply {
      for (screen in initialScreens) {
        push(screen)
      }
    }
  }
}

/**
 * Creates and remembers a [SaveableNavStack] filled with the given [navStackList].
 *
 * If [navStackList] changes, a new nav stack will be created.
 */
@Composable
public fun rememberSaveableNavStack(
  navStackList: NavStackList<Screen>
): NavStack<out NavStack.Record> {
  return rememberSaveable(navStackList, saver = SaveableNavStack.Saver) {
    SaveableNavStack().apply {
      navStackList.toList().asReversed().forEach { push(it) }
      repeat(navStackList.forwardItems.count()) { backward() }
    }
  }
}

/**
 * A [NavStack] that supports saving its state via [rememberSaveable]. See
 * [rememberSaveableNavStack].
 *
 * This implementation supports forward navigation, removal in both directions, and moving through
 * the stack without removing entries.
 */
public class SaveableNavStack
internal constructor(
  // Both visible for testing
  internal val entryList: SnapshotStateList<Record> = mutableStateListOf(),
  internal val stateStore: MutableMap<Screen, SaveableNavStackList> = mutableMapOf(),
  initialIndex: Int = -1,
) : NavStack<Record>, Iterable<Record> {

  // Track the current position in the stack (index into entryList)
  // Index 0 is the top (newest), size-1 is the root (oldest)
  private var currentIndex by mutableIntStateOf(initialIndex)

  public constructor(root: Screen) : this(Record(root))

  public constructor(root: Record) : this() {
    push(root)
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

  override fun push(screen: Screen): Boolean {
    return push(Record(screen))
  }

  override fun push(record: Record): Boolean {
    val currentRecord = Snapshot.withoutReadObservation { currentRecord }
    // Guard pushing the exact same record value to the top, records.key is always unique so verify
    // the parameters individually.
    return if (currentRecord?.screen != record.screen) {
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

  override fun pop(): Record? {
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

  override fun forward(): Boolean {
    // Move forward (toward top, decrease index)
    return if (currentIndex > 0) {
      currentIndex--
      true
    } else false
  }

  override fun backward(): Boolean {
    // Move backward (toward root, increase index)
    return if (currentIndex < entryList.lastIndex) {
      currentIndex++
      true
    } else false
  }

  override fun snapshot(): NavStackList<Record>? {
    return if (entryList.isNotEmpty() && currentIndex >= 0 && currentIndex <= entryList.lastIndex) {
      SaveableNavStackList(entryList.toList(), currentIndex)
    } else null
  }

  override fun saveState() {
    if (entryList.isNotEmpty()) {
      val rootScreen = entryList.last().screen
      stateStore[rootScreen] = SaveableNavStackList(entryList.toList(), currentIndex)
    }
  }

  override fun restoreState(screen: Screen): Boolean =
    Snapshot.withMutableSnapshot {
      val stored = stateStore[screen]
      if (stored != null && stored.entries.isNotEmpty()) {
        entryList.clear()
        // Add the stored state into the entry list
        entryList.addAll(stored.entries)
        // Restore the current index
        currentIndex = stored.currentIndex
        // Clear the stored state
        stateStore.remove(screen)
        true
      } else false
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
    if (isRecordReachable(key, depth, currentIndex, entryList)) {
      return true
    }
    // If includeSaved, check saved stack states too
    if (includeSaved && stateStore.isNotEmpty()) {
      for (snapshot in stateStore.values) {
        if (isRecordReachable(key, depth, snapshot.currentIndex, snapshot.entries)) {
          return true
        }
      }
    }
    return false
  }

  private fun isRecordReachable(
    key: String,
    depth: Int,
    index: Int,
    records: List<Record>,
  ): Boolean {
    val min = maxOf(0, index - depth)
    for (i in min until index) {
      if (records[i].key == key) return true
    }
    val max = minOf(index + depth, records.size)
    for (i in index until max) {
      if (records[i].key == key) return true
    }
    return false
  }

  public data class Record(
    override val screen: Screen,
    @OptIn(ExperimentalUuidApi::class) override val key: String = Uuid.random().toString(),
  ) : NavStack.Record {

    internal companion object {
      val Saver: Saver<Record, Any> =
        mapSaver(
          save = { value ->
            buildMap {
              put("screen", value.screen)
              put("key", value.key)
            }
          },
          restore = { map ->
            @Suppress("UNCHECKED_CAST")
            Record(screen = map["screen"] as Screen, key = map["key"] as String)
          },
        )
    }
  }

  internal data class SaveableNavStackList(val entries: List<Record>, val currentIndex: Int) :
    NavStackList<Record> {

    override val top: Record
      get() = entries.first()

    override val active: Record
      get() = entries[currentIndex]

    override val root: Record
      get() = entries.last()

    override val forwardItems: Iterable<Record>
      get() = entries.subList(0, currentIndex).asReversed()

    override val backwardItems: Iterable<Record>
      get() = entries.subList(currentIndex + 1, entries.size)

    override fun iterator(): Iterator<Record> {
      return entries.iterator()
    }

    companion object {
      val Saver: Saver<SaveableNavStackList, Any> =
        mapSaver(
          save = { value ->
            buildMap {
              with(Record.Saver) { put("entries", value.entries.mapNotNull { save(it) }) }
              put("currentIndex", value.currentIndex)
            }
          },
          restore = { map ->
            @Suppress("UNCHECKED_CAST")
            SaveableNavStackList(
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
            with(SaveableNavStackList.Saver) { add(value.stateStore.values.map { save(it) }) }
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
                    .mapNotNull { SaveableNavStackList.Saver.restore(it as List<Any>) }
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

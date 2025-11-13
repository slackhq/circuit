/*
 * Copyright (C) 2025 Slack Technologies, LLC
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.slack.circuit.backstack.SaveableBackStack.Record
import com.slack.circuit.runtime.screen.Screen
import kotlin.math.min

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
        push(screen)
      }
    }
  }
}

/**
 * A [NavStack] implementation that supports both backward and forward navigation with state saving.
 *
 * This implementation uses a single list with a pointer (currentIndex) to track the current
 * position in the navigation history:
 * - Items at indices 0 to currentIndex-1 are "back" items
 * - Item at currentIndex is the current screen
 * - Items after currentIndex are "forward" items
 *
 * When navigating back, the pointer is decremented.
 * When navigating forward, the pointer is incremented.
 * When pushing a new screen, items after the current position are removed and the new screen is
 * added at the end (standard browser behavior).
 */
@Stable
public class SaveableNavStack
internal constructor(
  // All visible for testing
  internal val historyList: SnapshotStateList<Record> = mutableStateListOf(),
  internal val initialIndex: Int = 0,
  internal val stateStore: MutableMap<Screen, Pair<List<Record>, Int>> = mutableMapOf(),
) : NavStack<Record> {

  private var currentIndex by mutableIntStateOf(initialIndex)

  public constructor(root: Screen) : this(Record(root))

  public constructor(root: Record) : this() {
    push(root)
  }

  /**
   * The size reflects only the visible back stack (from index 0 to currentIndex, inclusive).
   * This maintains compatibility with BackStack behavior.
   */
  override val size: Int
    get() = currentIndex + 1

  /**
   * Iterator returns only the visible back stack items (from index 0 to currentIndex).
   * This maintains compatibility with BackStack behavior where iteration is top-first.
   */
  override fun iterator(): Iterator<Record> = historyList.subList(0, currentIndex + 1).iterator()

  public override val topRecord: Record?
    get() = historyList.getOrNull(currentIndex)

  override val rootRecord: Record?
    get() = historyList.getOrNull(0)

  /**
   * Returns true if forward navigation is possible (i.e., there are items after currentIndex).
   */
  public val canGoForward: Boolean
    get() = currentIndex < historyList.size - 1

  /**
   * Returns the number of items in the forward stack.
   */
  public val forwardSize: Int
    get() = (historyList.size - 1 - currentIndex).coerceAtLeast(0)

  public override fun push(screen: Screen): Boolean {
    return push(screen, emptyMap())
  }

  public fun push(screen: Screen, args: Map<String, Any?>): Boolean {
    return push(Record(screen, args))
  }

  public override fun push(record: Record): Boolean {
    val topRecord = Snapshot.withoutReadObservation { topRecord }
    // Guard pushing the exact same record value to the top, records.key is always unique so verify
    // the parameters individually.
    return if (topRecord?.screen != record.screen || topRecord.args != record.args) {
      // Remove all forward history when pushing a new record
      if (currentIndex < historyList.size - 1) {
        historyList.subList(currentIndex + 1, historyList.size).clear()
      }
      // Add new record at the end and update pointer
      historyList.add(record)
      currentIndex = historyList.size - 1
      true
    } else false
  }

  /**
   * Navigate back in the history by decrementing the pointer.
   * Returns the record at the new current position, or null if already at the root.
   *
   * @param clearForward if true, removes forward history when going back (default: false)
   */
  public fun pop(clearForward: Boolean = false): Record? {
    // Don't go back if we're already at the root
    if (currentIndex <= 0) return null

    return Snapshot.withoutReadObservation {
      if (clearForward) {
        // Remove the current item and everything after it
        historyList.subList(currentIndex, historyList.size).clear()
      }
      // Move pointer back
      currentIndex--
      historyList.getOrNull(currentIndex)
    }
  }

  /**
   * Standard pop() implementation that maintains forward history.
   * This implements the BackStack interface.
   */
  override fun pop(): Record? = pop(clearForward = false)

  /**
   * Navigate forward in the history by incrementing the pointer.
   * Returns the record at the new current position, or null if already at the end of history.
   */
  public fun forward(): Record? {
    // Can't go forward if we're already at the end
    if (currentIndex >= historyList.size - 1) return null

    return Snapshot.withoutReadObservation {
      currentIndex++
      historyList.getOrNull(currentIndex)
    }
  }

  /**
   * Clear the forward history. Useful when you want to prevent forward navigation.
   */
  public fun clearForward() {
    if (currentIndex < historyList.size - 1) {
      historyList.subList(currentIndex + 1, historyList.size).clear()
    }
  }

  /**
   * Saves both the history list and current index in the state store.
   */
  override fun saveState() {
    val rootScreen = historyList.firstOrNull()?.screen ?: return
    stateStore[rootScreen] = Pair(historyList.toList(), currentIndex)
  }

  /**
   * Restores the saved history list and current index from the state store.
   */
  override fun restoreState(screen: Screen): Boolean {
    val stored = stateStore[screen]
    if (stored != null && stored.first.isNotEmpty()) {
      // Clear current history and restore saved state
      historyList.clear()
      historyList.addAll(stored.first)
      currentIndex = stored.second
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
    // Check in the history list
    if (record in historyList) return true

    if (includeSaved && stateStore.isNotEmpty()) {
      // If we're checking our saved lists too, iterate through them and check
      for ((stored, _) in stateStore.values) {
        if (record in stored) return true
      }
    }
    return false
  }

  override fun isRecordReachable(key: String, depth: Int, includeSaved: Boolean): Boolean {
    if (depth < 0) return false

    // Check in the visible back stack (from currentIndex backwards)
    for (i in currentIndex downTo 0.coerceAtLeast(currentIndex - depth)) {
      if (historyList.getOrNull(i)?.key == key) return true
    }

    // Check in the forward stack
    for (i in currentIndex + 1..min(currentIndex + depth, historyList.size - 1)) {
      if (historyList.getOrNull(i)?.key == key) return true
    }

    // If includeSaved, check saved backstack states too
    if (includeSaved && stateStore.isNotEmpty()) {
      for ((stored, _) in stateStore.values) {
        if (stored.take(depth).any { it.key == key }) return true
      }
    }
    return false
  }

  internal companion object {
    @Suppress("UNCHECKED_CAST")
    val Saver =
      listSaver<SaveableNavStack, Any>(
        save = { value ->
          buildList {
            with(Record.Saver) {
              // First: save the history list
              add(value.historyList.mapNotNull { save(it) })
              // Second: save the current index
              add(value.currentIndex)
              // Third: save the state store
              val stateStoreData =
                value.stateStore.map { (screen, pair) ->
                  mapOf(
                    "screen" to screen,
                    "records" to pair.first.mapNotNull { save(it) },
                    "index" to pair.second,
                  )
                }
              add(stateStoreData)
            }
          }
        },
        restore = { value ->
          val list = value as List<Any>
          SaveableNavStack().also { navStack ->
            with(Record.Saver) {
              // First: restore the history list
              (list[0] as List<List<Any>>).mapNotNullTo(navStack.historyList) { restore(it) }
              // Second: restore the current index
              navStack.currentIndex = list[1] as Int
              // Third: restore the state store
              @Suppress("UNCHECKED_CAST")
              (list[2] as List<Map<String, Any>>).forEach { stateEntry ->
                val screen = stateEntry["screen"] as Screen
                val records =
                  (stateEntry["records"] as List<List<Any>>).mapNotNull { restore(it) }
                val index = stateEntry["index"] as Int
                navStack.stateStore[screen] = Pair(records, index)
              }
            }
          }
        },
      )
  }
}

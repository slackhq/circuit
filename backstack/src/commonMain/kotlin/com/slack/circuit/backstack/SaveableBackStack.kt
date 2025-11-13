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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.slack.circuit.backstack.SaveableBackStack.Record
import com.slack.circuit.runtime.screen.Screen
import kotlin.math.min
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Creates and remembers a [SaveableBackStack] with the given [root] screen.
 *
 * If [root] changes, a new backstack will be created.
 *
 * @param init optional initializer callback to perform extra initialization logic.
 */
@Composable
public fun rememberSaveableBackStack(
  root: Screen,
  init: SaveableBackStack.() -> Unit = {},
): SaveableBackStack =
  rememberSaveable(root, saver = SaveableBackStack.Saver) { SaveableBackStack(root).apply(init) }

/**
 * Creates and remembers a [SaveableBackStack] filled with the given [initialScreens].
 *
 * [initialScreens] must not be empty. If [initialScreens] changes, a new backstack will be created.
 */
@Composable
public fun rememberSaveableBackStack(initialScreens: List<Screen>): SaveableBackStack {
  require(initialScreens.isNotEmpty()) { "Initial input screens cannot be empty!" }
  return rememberSaveable(initialScreens, saver = SaveableBackStack.Saver) {
    SaveableBackStack().apply {
      for (screen in initialScreens) {
        push(screen)
      }
    }
  }
}

/**
 * A [BackStack] that supports saving its state via [rememberSaveable]. See
 * [rememberSaveableBackStack].
 */
public class SaveableBackStack
internal constructor(
  // Both visible for testing
  internal val entryList: SnapshotStateList<Record> = mutableStateListOf(),
  internal val stateStore: MutableMap<Screen, List<Record>> = mutableMapOf(),
) : BackStack<Record> {

  public constructor(root: Screen) : this(Record(root))

  public constructor(root: Record) : this() {
    push(root)
  }

  override val size: Int
    get() = entryList.size

  override fun iterator(): Iterator<Record> = entryList.iterator()

  public override val topRecord: Record?
    get() = entryList.firstOrNull()

  override val rootRecord: Record?
    get() = entryList.lastOrNull()

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
      entryList.add(0, record)
      true
    } else false
  }

  override fun pop(): Record? {
    return Snapshot.withoutReadObservation { entryList.removeFirstOrNull() }
  }

  override fun saveState() {
    val rootScreen = entryList.last().screen
    stateStore[rootScreen] = entryList.toList()
  }

  override fun restoreState(screen: Screen): Boolean {
    val stored = stateStore[screen]
    if (!stored.isNullOrEmpty()) {
      // Add the store state into the entry list
      entryList.addAll(stored)
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
      // If we're checking our saved lists too, iterate through them and check
      for (stored in stateStore.values) {
        if (record in stored) return true
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
    // If includeSaved, check saved backstack states too
    if (includeSaved && stateStore.isNotEmpty()) {
      val storedValues = stateStore.values
      for ((i, stored) in storedValues.withIndex()) {
        if (i >= depth) break
        // stored can mutate, so safely get the record.
        if (stored.getOrNull(i)?.key == key) return true
      }
    }
    return false
  }

  public data class Record(
    override val screen: Screen,
    val args: Map<String, Any?> = emptyMap(),
    @OptIn(ExperimentalUuidApi::class) override val key: String = Uuid.random().toString(),
  ) : BackStack.Record {

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

  internal companion object {
    @Suppress("UNCHECKED_CAST")
    val Saver =
      listSaver<SaveableBackStack, List<Any?>>(
        save = { value ->
          buildList {
            with(Record.Saver) {
              // First list is the entry list
              add(value.entryList.mapNotNull { save(it) })
              // Now add any stacks from the state store
              value.stateStore.values.forEach { records -> add(records.mapNotNull { save(it) }) }
            }
          }
        },
        restore = { value ->
          SaveableBackStack().also { backStack ->
            value.forEachIndexed { index, list ->
              if (index == 0) {
                // The first list is the entry list
                list.mapNotNullTo(backStack.entryList) { Record.Saver.restore(it as List<Any>) }
              } else {
                // Any list after that is from the state store
                val records = list.mapNotNull { Record.Saver.restore(it as List<Any>) }
                // The key is always the root screen (i.e. last item)
                backStack.stateStore[records.last().screen] = records
              }
            }
          }
        },
      )
  }
}

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
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.DefaultCircuitSaver
import com.slack.circuit.runtime.screen.LocalCircuitSaver
import com.slack.circuit.runtime.screen.Screen
import kotlin.math.min
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Creates and remembers a [SaveableBackStack] with the given [root] screen.
 *
 * If [root] changes, a new backstack will be created.
 *
 * @param circuitSaver the [CircuitSaver] used to persist screens, defaulting to
 *   [LocalCircuitSaver].
 * @param init optional initializer callback to perform extra initialization logic.
 */
@Composable
public fun rememberSaveableBackStack(
  root: Screen,
  circuitSaver: CircuitSaver = LocalCircuitSaver.current,
  init: SaveableBackStack.() -> Unit = {},
): SaveableBackStack =
  rememberSaveable(root, saver = SaveableBackStack.Saver(circuitSaver)) {
    SaveableBackStack(root).apply(init)
  }

/**
 * Creates and remembers a [SaveableBackStack] filled with the given [initialScreens].
 *
 * [initialScreens] must not be empty. If [initialScreens] changes, a new backstack will be created.
 *
 * @param circuitSaver the [CircuitSaver] used to persist screens, defaulting to
 *   [LocalCircuitSaver].
 */
@Composable
public fun rememberSaveableBackStack(
  initialScreens: List<Screen>,
  circuitSaver: CircuitSaver = LocalCircuitSaver.current,
): SaveableBackStack {
  require(initialScreens.isNotEmpty()) { "Initial input screens cannot be empty!" }
  return rememberSaveable(initialScreens, saver = SaveableBackStack.Saver(circuitSaver)) {
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
    return push(Record(screen))
  }

  @Deprecated(
    "Pass data through the Screen itself instead. args will be removed in a future release."
  )
  public fun push(screen: Screen, args: Map<String, Any?>): Boolean {
    return push(Record(screen, args))
  }

  @Suppress("DEPRECATION")
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
      entryList.clear()
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
    if (isRecordReachable(key, depth, entryList)) {
      return true
    }
    // If includeSaved, check saved backstack states too
    if (includeSaved && stateStore.isNotEmpty()) {
      for (snapshot in stateStore.values) {
        if (isRecordReachable(key, depth, snapshot)) {
          return true
        }
      }
    }
    return false
  }

  private fun isRecordReachable(key: String, depth: Int, records: List<Record>): Boolean {
    // Check in the current entry list
    for (i in 0 until min(depth + 1, records.size)) {
      // stored can mutate, so safely get the record.
      if (records.getOrNull(i)?.key == key) return true
    }
    return false
  }

  public data class Record(
    override val screen: Screen,
    @property:Deprecated(
      "Pass data through the Screen itself instead. args will be removed in a future release."
    )
    val args: Map<String, Any?> = emptyMap(),
    @OptIn(ExperimentalUuidApi::class) override val key: String = Uuid.random().toString(),
  ) : BackStack.Record {

    public companion object {
      @Deprecated(
        "Use Saver(CircuitSaver) instead.",
        ReplaceWith(
          "Record.Saver(DefaultCircuitSaver)",
          "com.slack.circuit.runtime.screen.DefaultCircuitSaver",
        ),
      )
      public val Saver: Saver<Record, Any> = Saver(DefaultCircuitSaver)

      /** Returns a [Saver] that persists [Record]s with the given [circuitSaver]. */
      @Suppress("DEPRECATION")
      public fun Saver(circuitSaver: CircuitSaver): Saver<Record, Any> =
        mapSaver(
          save = { value ->
            buildMap {
              circuitSaver.save(value.screen)?.let { put("screen", it) }
              put("args", value.args)
              put("key", value.key)
            }
          },
          restore = { map ->
            val screen =
              map["screen"]?.let { circuitSaver.restore<Screen>(it) } ?: return@mapSaver null
            @Suppress("UNCHECKED_CAST")
            Record(
              screen = screen,
              args = map["args"] as Map<String, Any?>,
              key = map["key"] as String,
            )
          },
        )
    }
  }

  public companion object {
    @Deprecated(
      "Use Saver(CircuitSaver) instead.",
      ReplaceWith(
        "SaveableBackStack.Saver(DefaultCircuitSaver)",
        "com.slack.circuit.runtime.screen.DefaultCircuitSaver",
      ),
    )
    public val Saver: Saver<SaveableBackStack, Any> = Saver(DefaultCircuitSaver)

    /** Returns a [Saver] that persists [SaveableBackStack]s with the given [circuitSaver]. */
    @Suppress("UNCHECKED_CAST")
    public fun Saver(circuitSaver: CircuitSaver): Saver<SaveableBackStack, Any> {
      val recordSaver = Record.Saver(circuitSaver)
      return listSaver<SaveableBackStack, List<Any?>>(
        save = { value ->
          buildList {
            with(recordSaver) {
              // First list is the entry list
              add(value.entryList.mapNotNull { save(it) })
              // Now add any stacks from the state store
              value.stateStore.values.forEach { records -> add(records.mapNotNull { save(it) }) }
            }
          }
        },
        restore = { value ->
          val backStack = SaveableBackStack()
          value.forEachIndexed { index, list ->
            if (index == 0) {
              // The first list is the entry list
              list.mapNotNullTo(backStack.entryList) { recordSaver.restore(it as List<Any>) }
            } else {
              // Any list after that is from the state store
              list
                .filterIsInstance<List<Any>>()
                .mapNotNull { recordSaver.restore(it) }
                .takeIf { it.isNotEmpty() }
                ?.let { records ->
                  // The key is always the root screen (i.e. last item)
                  backStack.stateStore[records.last().screen] = records
                }
            }
          }
          // If every record was dropped, return null so rememberSaveable falls back to its
          // factory instead of restoring an empty, unusable stack.
          backStack.takeIf { it.entryList.isNotEmpty() }
        },
      )
    }
  }
}

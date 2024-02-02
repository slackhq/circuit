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
import androidx.compose.runtime.saveable.rememberSaveable
import com.benasher44.uuid.uuid4
import com.slack.circuit.runtime.screen.Screen

@Composable
public fun rememberSaveableBackStack(init: SaveableBackStack.() -> Unit): SaveableBackStack =
  rememberSaveable(saver = SaveableBackStack.Saver) {
    SaveableBackStack().apply(init).also {
      check(!it.isEmpty) { "Backstack must be non-empty after init." }
    }
  }

/**
 * A [BackStack] that supports saving its state via [rememberSaveable]. See
 * [rememberSaveableBackStack].
 */
public class SaveableBackStack : BackStack<SaveableBackStack.Record> {

  // Both visible for testing
  internal val entryList = mutableStateListOf<Record>()
  internal val stateStore = mutableMapOf<Screen, List<Record>>()

  override val size: Int
    get() = entryList.size

  override fun iterator(): Iterator<Record> = entryList.iterator()

  public override val topRecord: Record?
    get() = entryList.firstOrNull()

  public override fun push(screen: Screen) {
    push(screen, emptyMap())
  }

  public fun push(screen: Screen, args: Map<String, Any?>) {
    push(Record(screen, args))
  }

  public override fun push(record: Record) {
    entryList.add(0, record)
  }

  override fun pop(): Record? = entryList.removeFirstOrNull()

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

  public data class Record(
    override val screen: Screen,
    val args: Map<String, Any?> = emptyMap(),
    override val key: String = uuid4().toString(),
  ) : BackStack.Record {
    internal companion object {
      val Saver: Saver<Record, List<Any>> =
        Saver(
          save = { value ->
            buildList {
              add(value.screen)
              add(value.args)
              add(value.key)
            }
          },
          restore = { list ->
            @Suppress("UNCHECKED_CAST")
            Record(
              screen = list[0] as Screen,
              args = list[1] as Map<String, Any?>,
              key = list[2] as String,
            )
          },
        )
    }
  }

  internal companion object {
    val Saver =
      Saver<SaveableBackStack, List<List<Any>>>(
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
          @Suppress("UNCHECKED_CAST")
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

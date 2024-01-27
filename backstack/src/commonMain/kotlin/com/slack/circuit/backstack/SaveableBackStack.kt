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
import com.slack.circuit.runtime.screen.PopResult
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
@OptIn(InternalBackStackApi::class)
public class SaveableBackStack : BackStack<SaveableBackStack.Record> {

  private val entryList = mutableStateListOf<Record>()

  override val size: Int
    get() = entryList.size

  override fun iterator(): Iterator<Record> = entryList.iterator()

  public override val topRecord: Record?
    get() = entryList.firstOrNull()

  public override fun push(screen: Screen) {
    return push(screen, emptyMap())
  }

  public fun push(screen: Screen, args: Map<String, Any?>) {
    push(Record(screen, args))
  }

  public override fun push(record: Record) {
    entryList.add(0, record)
    // Clear the cached pending result from the previous top record
    // TODO this may not be enough. See TODO in clearPendingResult()
    entryList.getOrNull(1)?.clearPendingResult()
  }

  override fun pop(result: PopResult?): Record? {
    val popped = entryList.removeFirstOrNull()
    result?.let {
      // Send the pending result to our new top record
      topRecord?.updatePendingResult(it)
    }
    return popped
  }

  public data class Record
  internal constructor(
    override val screen: Screen,
    val args: Map<String, Any?>,
    override val key: String,
    private val resultRecord: ResultRecordImpl,
  ) : BackStack.Record, ResultRecord by resultRecord {

    public constructor(
      screen: Screen,
      args: Map<String, Any?> = emptyMap(),
      key: String = uuid4().toString(),
    ) : this(screen, args, key, ResultRecordImpl())

    internal companion object {
      val Saver: Saver<Record, List<Any>> =
        Saver(
          save = { value ->
            buildList {
              add(value.screen)
              add(value.args)
              add(value.key)
              // TODO this seems brittle if they ever get out of sync and order changes
              value.resultRecord._pendingResult?.let { add(it) }
              value.resultRecord.pendingResultKey?.let { add(it) }
            }
          },
          restore = { list ->
            @Suppress("UNCHECKED_CAST")
            Record(
                screen = list[0] as Screen,
                args = list[1] as Map<String, Any?>,
                key = list[2] as String,
              )
              .also { record ->
                (list.getOrNull(3) as? PopResult?)?.let(record.resultRecord::updatePendingResult)
                (list.getOrNull(4) as? String?)?.let(record.resultRecord::setResultKey)
              }
          },
        )
    }
  }

  internal companion object {
    val Saver =
      Saver<SaveableBackStack, List<Any>>(
        save = { value -> value.entryList.map { with(Record.Saver) { save(it)!! } } },
        restore = { list ->
          SaveableBackStack().also { backStack ->
            list.mapTo(backStack.entryList) {
              @Suppress("UNCHECKED_CAST") Record.Saver.restore(it as List<Any>)!!
            }
          }
        },
      )
  }
}

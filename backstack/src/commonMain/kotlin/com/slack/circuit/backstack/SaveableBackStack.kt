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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

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

  public interface ResultRecord {
    public fun setResultKey(key: String)

    public fun clearResultKey()

    public fun updatePendingResult(result: PopResult)

    public fun clearPendingResult()

    public suspend fun awaitResult(key: String): PopResult?
  }

  internal class ResultRecordImpl : ResultRecord {
    /**
     * A [Channel] of pending results. Note we use this instead of a [CompletableDeferred] because
     * we may push and pop back to a given record multiple times, and thus need to be able to push
     * and receive multiple results.
     *
     * TODO what's the right behavior here?
     * - Capacity 1 + overflow drop oldest: we only care about the most recent result
     * - Conflated: only take one result until the presenter takes it.
     */
    private val pendingResultChannel =
      Channel<PopResult>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    internal var pendingResultKey: String? = null

    internal var _pendingResult: PopResult? = null

    override fun setResultKey(key: String) {
      pendingResultKey = key
    }

    override fun clearResultKey() {
      pendingResultKey = null
    }

    override fun updatePendingResult(result: PopResult) {
      _pendingResult = result
      pendingResultChannel.trySend(result)
    }

    override fun clearPendingResult() {
      _pendingResult = null
      // TODO do we clear the channel's value here too?
    }

    override suspend fun awaitResult(key: String): PopResult? {
      return if (key == pendingResultKey) {
        pendingResultChannel.receive()
      } else {
        null
      }
    }
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

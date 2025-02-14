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
import com.slack.circuit.runtime.navigation.NavigationContext
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

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
    SaveableBackStack(null).apply {
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
  nullableRootRecord: Record?,
  // Unused marker just to differentiate the internal constructor on the JVM.
  @Suppress("UNUSED_PARAMETER") internalConstructorMarker: Any? = null,
) : BackStack<SaveableBackStack.Record> {

  public constructor(initialRecord: Record) : this(nullableRootRecord = initialRecord)

  public constructor(root: Screen) : this(Record(root))

  // Both visible for testing
  internal val entryList = mutableStateListOf<Record>()
  internal val stateStore = mutableMapOf<Screen, List<Record>>()

  init {
    nullableRootRecord?.let(::push)
  }

  override val size: Int
    get() = entryList.size

  override fun iterator(): Iterator<Record> = entryList.iterator()

  public override val topRecord: Record?
    get() = entryList.firstOrNull()

  public override fun push(
    screen: Screen,
    resultKey: String?,
    context: NavigationContext,
  ): Boolean {
    return push(screen, emptyMap(), resultKey, context)
  }

  public fun push(
    screen: Screen,
    args: Map<String, Any?>,
    resultKey: String?,
    context: NavigationContext,
  ): Boolean {
    return push(Record(screen, args, context = context), resultKey)
  }

  public override fun push(record: Record, resultKey: String?): Boolean {
    val topRecord = Snapshot.withoutReadObservation { entryList.firstOrNull() }
    // Guard pushing the exact same record value to the top, records.key is always unique so verify
    // the parameters individually.
    return if (topRecord?.screen != record.screen || topRecord.args != record.args) {
      entryList.add(0, record)
      // Clear the cached pending result from the previous top record
      entryList.getOrNull(1)?.apply { resultKey?.let(::prepareForResult) }
      true
    } else false
  }

  public override fun pop(result: PopResult?, context: NavigationContext): Record? {
    // Run in a snapshot to ensure the sendResult doesn't get missed.
    return Snapshot.withMutableSnapshot {
      val popped = entryList.removeFirstOrNull()
      if (result != null) {
        // Send the pending result to our new top record, but only if it's expecting one
        topRecord?.apply { if (expectingResult()) sendResult(result) }
      }
      popped
    }
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

  public data class Record(
    override val screen: Screen,
    val args: Map<String, Any?> = emptyMap(),
    @OptIn(ExperimentalUuidApi::class) override val key: String = Uuid.random().toString(),
    override val context: NavigationContext = NavigationContext.EMPTY,
  ) : BackStack.Record {
    /**
     * A [Channel] of pending results. Note we use this instead of a [CompletableDeferred] because
     * we may push and pop back to a given record multiple times, and thus need to be able to push
     * and receive multiple results. We use [BufferOverflow.DROP_OLDEST] to ensure we only care
     * about the most recent result.
     */
    private val resultChannel =
      Channel<PopResult>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var resultKey: String? = null

    internal fun expectingResult(): Boolean = resultKey != null

    internal fun prepareForResult(key: String) {
      resultKey = key
      readResult()
    }

    private fun readResult() = resultChannel.tryReceive().getOrNull()

    internal fun sendResult(result: PopResult) {
      resultChannel.trySend(result)
    }

    override suspend fun awaitResult(key: String): PopResult? {
      return if (key == resultKey) {
        resultKey = null
        resultChannel.receive()
      } else {
        null
      }
    }

    internal companion object {

      val Saver: Saver<Record, Any> =
        mapSaver(
          save = { value ->
            buildMap {
              put("screen", value.screen)
              put("args", value.args)
              put("key", value.key)
              put("context", with(NavigationContext.Saver) { save(value.context) })
              put("resultKey", value.resultKey)
              put("result", value.readResult())
            }
          },
          restore = { map ->
            @Suppress("UNCHECKED_CAST")
            Record(
                screen = map["screen"] as Screen,
                args = map["args"] as Map<String, Any?>,
                key = map["key"] as String,
                context =
                  NavigationContext.Saver.restore(map["context"] as Any) ?: NavigationContext.EMPTY,
              )
              .apply {
                // NOTE order matters here, prepareForResult() clears the buffer
                (map["resultKey"] as? String?)?.let(::prepareForResult)
                (map["result"] as? PopResult?)?.let(::sendResult)
              }
          },
        )
    }
  }

  internal companion object {
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
          @Suppress("UNCHECKED_CAST")
          SaveableBackStack(null).also { backStack ->
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

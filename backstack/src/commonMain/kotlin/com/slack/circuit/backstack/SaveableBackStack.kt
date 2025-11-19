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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.slack.circuit.backstack.SaveableBackStack.Record
import com.slack.circuit.runtime.NavStackList
import com.slack.circuit.runtime.screen.Screen
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
): SaveableBackStack {
  val navStack = rememberSaveableNavStack(root)
  return remember(navStack) { SaveableBackStack(navStack).apply { init() } }
}

/**
 * Creates and remembers a [SaveableBackStack] filled with the given [initialScreens].
 *
 * [initialScreens] must not be empty. If [initialScreens] changes, a new backstack will be created.
 */
@Composable
public fun rememberSaveableBackStack(initialScreens: List<Screen>): SaveableBackStack {
  require(initialScreens.isNotEmpty()) { "Initial input screens cannot be empty!" }
  val navStack = rememberSaveableNavStack(initialScreens)
  return remember(navStack) { SaveableBackStack(navStack) }
}

/**
 * A [BackStack] that supports saving its state via [rememberSaveable]. See
 * [rememberSaveableBackStack].
 */
public class SaveableBackStack
internal constructor(
  // Visible for testing
  internal val delegate: SaveableNavStack = SaveableNavStack()
) : BackStack<Record> {

  public constructor(root: Screen) : this(Record(root))

  public constructor(root: Record) : this() {
    push(root)
  }

  override val size: Int
    get() = delegate.size

  override fun iterator(): Iterator<Record> =
    delegate.entryList.iterator().asSequence().map { Record(it) }.iterator()

  override val topRecord: Record?
    get() = delegate.topRecord?.let { Record(it) }

  override val currentRecord: Record?
    get() = delegate.currentRecord?.let { Record(it) }

  override val rootRecord: Record?
    get() = delegate.rootRecord?.let { Record(it) }

  override fun forward(): Boolean = false

  override fun backward(): Boolean {
    return delegate.pop() != null
  }

  public override fun push(screen: Screen): Boolean = push(Record(screen))

  public override fun push(record: Record): Boolean {
    return delegate.push(SaveableNavStack.Record(record.screen, record.args, record.key))
  }

  override fun pop(): Record? {
    return delegate.pop()?.let { Record(it) }
  }

  override fun snapshot(): NavStackList<Record>? {
    return delegate.snapshot()?.let { BackStackList(it) }
  }

  override fun saveState() {
    delegate.saveState()
  }

  override fun restoreState(screen: Screen): Boolean {
    return delegate.restoreState(screen)
  }

  override fun peekState(): List<Screen> {
    return delegate.peekState()
  }

  override fun removeState(screen: Screen): Boolean {
    return delegate.removeState(screen)
  }

  override fun containsRecord(record: Record, includeSaved: Boolean): Boolean {
    return delegate.containsRecord(
      SaveableNavStack.Record(record.screen, record.args, record.key),
      includeSaved,
    )
  }

  override fun isRecordReachable(key: String, depth: Int, includeSaved: Boolean): Boolean {
    return delegate.isRecordReachable(key, depth, includeSaved)
  }

  /**
   * A snapshot of a back stack state. Since BackStack always has current at the top, currentIndex
   * is always 0.
   */
  private data class BackStackList(val other: NavStackList<SaveableNavStack.Record>) :
    NavStackList<Record> {

    override val top: Record = Record(other.top)
    override val current: Record = Record(other.current)
    override val root: Record = Record(other.root)

    override val forward: Iterable<Record>
      get() = other.forward.map { Record(it) }

    override val backward: Iterable<Record>
      get() = other.backward.map { Record(it) }

    override fun iterator(): Iterator<Record> {
      return other.iterator().asSequence().map { Record(it) }.iterator()
    }
  }

  public data class Record(
    override val screen: Screen,
    val args: Map<String, Any?> = emptyMap(),
    @OptIn(ExperimentalUuidApi::class) override val key: String = Uuid.random().toString(),
  ) : BackStack.Record {
    internal constructor(other: SaveableNavStack.Record) : this(other.screen, other.args, other.key)
  }
}

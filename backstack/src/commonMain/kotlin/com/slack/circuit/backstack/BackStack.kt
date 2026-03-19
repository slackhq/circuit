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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.Snapshot
import com.slack.circuit.backstack.BackStack.Record
import com.slack.circuit.runtime.navigation.NavStack
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf

/**
 * A caller-supplied stack of [Record]s for presentation with a `Navigator`. Iteration order is
 * top-first (first element is the top of the stack).
 *
 * BackStack extends [NavStack] but is intended for [push] or [pop] only navigation patterns.
 * Implementations may provide no-op implementations for forward navigation methods.
 */
@Stable
public interface BackStack<R : Record> : NavStack<R>, Iterable<R> {

  override val currentRecord: R?
    get() = topRecord

  /** Forward navigation is not supported by the [BackStack]. */
  override fun forward(): Boolean {
    return false
  }

  /**
   * Backward navigation is not supported by the [BackStack] as you are unable to go forward again.
   */
  override fun backward(): Boolean {
    return false
  }

  override fun snapshot(): NavStackList<R>? {
    return if (!isEmpty) navStackListOf(this) else null
  }

  @Stable public interface Record : NavStack.Record
}

/** `true` if the [BackStack] contains no records. [Iterable.firstOrNull] will return `null`. */
public val BackStack<out Record>.isEmpty: Boolean
  get() = size == 0

/** `true` if the [BackStack] contains exactly one record. */
public val BackStack<out Record>.isAtRoot: Boolean
  get() = size == 1

/** Clear any saved state from the [BackStack]. */
public fun BackStack<out Record>.clearState() {
  Snapshot.withMutableSnapshot {
    for (screen in peekState()) {
      removeState(screen)
    }
  }
}

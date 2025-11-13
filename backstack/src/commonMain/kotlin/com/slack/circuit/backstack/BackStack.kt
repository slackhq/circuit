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
import com.slack.circuit.backstack.NavStack.Record
import com.slack.circuit.runtime.screen.Screen

/**
 * A caller-supplied stack of [Record]s for presentation with a `Navigator`. Iteration order is
 * top-first (first element is the top of the stack).
 *
 * BackStack extends NavStack but is intended for backward-only navigation patterns. Implementations
 * may provide no-op implementations for forward navigation methods.
 */
@Stable
public interface BackStack<R : Record> : NavStack<R>, Iterable<R> {
  // BackStack inherits all methods from NavStack
}

public fun <R : Record> BackStack<R>.push(record: R): Boolean {
  return add(record)
}

public fun <R : Record> BackStack<R>.push(screen: Screen): Boolean {
  return add(screen)
}

/**
 * Attempt to pop the top item off of the back stack, returning the popped [Record] if popping was
 * successful or `null` if no entry was popped.
 */
public fun <R : Record> BackStack<R>.pop(): R? {
  return Snapshot.withMutableSnapshot {
    // todo edge cases
    while (canGoForward) {
      // Move forward and remove the previous record.
      move(NavStack.Direction.Forward)
      remove(NavStack.Direction.Backward)
    }
    remove(NavStack.Direction.Backward)
  }
}

/** Pop records off the top of the stack until one is found that matches the given predicate. */
public fun <R : Record> NavStack<R>.popUntil(predicate: (R) -> Boolean): List<R> {
  return buildList {
    while (currentRecord?.let(predicate) == false) {
      val popped = this@popUntil.remove(NavStack.Direction.Backward) ?: break
      add(popped)
    }
  }
}

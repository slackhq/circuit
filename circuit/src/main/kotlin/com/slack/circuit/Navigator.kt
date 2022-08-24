/*
 * Copyright (C) 2022 Slack Technologies, LLC
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
package com.slack.circuit

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView

fun interface OnPopHandler {
  fun onPop()
}

/** A basic navigation interface for navigating between [screens][Screen]. */
interface Navigator {
  fun goTo(screen: Screen)

  fun pop(): Screen?
}

/** Calls [Navigator.pop] until the given [predicate] is matched or it pops the root. */
fun Navigator.popUntil(predicate: (Screen) -> Boolean) {
  while (true) {
    val screen = pop() ?: break
    if (predicate(screen)) {
      break
    }
  }
}

/**
 * A simple rendering abstraction over a `@Composable () -> Unit`.
 *
 * This allows for any host container that can render composable functions to be used with a given
 * [Circuit.navigator] call, such as [ComponentActivity] and [ComposeView].
 */
fun interface ContentContainer {
  fun render(content: @Composable () -> Unit)
}

fun ComposeView.asContentContainer() = ContentContainer(::setContent)

fun ComponentActivity.asContentContainer() = ContentContainer { content ->
  setContent(content = content)
}

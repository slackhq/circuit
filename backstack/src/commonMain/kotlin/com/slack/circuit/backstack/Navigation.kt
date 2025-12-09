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
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.screen.Screen
import kotlin.DeprecationLevel.WARNING

/** Presentation logic for currently visible routes of a navigable UI. */
@Stable
public interface NavDecoration {
  @Composable
  public fun <T : NavArgument> DecoratedContent(
    args: List<T>,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  )
}

@Deprecated(
  message = "Use NavArgument from the new package instead",
  replaceWith = ReplaceWith("NavArgument", "com.slack.circuit.runtime.navigation.NavArgument"),
  level = WARNING,
)
/** Argument provided to [NavDecoration] that exposes the underlying [Screen]. */
public typealias NavArgument = NavArgument

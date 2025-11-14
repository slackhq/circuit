// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen

/** Presentation logic for currently visible routes of a navigable UI. */
@Stable
public interface NavDecoration {
  @Composable
  public fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    navigator: Navigator,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  )
}

/**
 * A lightweight view of a navigation stack state, used by [NavDecoration] to provide context about
 * the current navigation position and history.
 *
 * ## Structure
 *
 * `NavStackList` uses a **List+Index model** to represent navigation state:
 * - **entries**: A list of items ordered from newest (index 0) to oldest (last index)
 * - **currentIndex**: The position of the currently active/visible entry
 *
 * ## Index Semantics
 *
 * The list is ordered from top (newest) to root (oldest):
 * ```
 * entries:  [TopScreen, MiddleScreen, RootScreen]
 * indices:   0          1             2
 *            ^                        ^
 *          newest                  oldest
 * ```
 * - `currentIndex = 0`: At the top (newest entry), no forward history
 * - `currentIndex = lastIndex`: At the root (oldest entry), no backward history
 * - `0 < currentIndex < lastIndex`: In the middle, between newer and older entries
 *
 * ## Use in NavDecoration
 *
 * [NavDecoration] receives a `NavStackList` to apply presentation logic based on navigation state.
 * For example, a decoration might:
 * - Apply different transitions when navigating forward vs backward
 * - Show breadcrumbs based on the backward stack
 * - Display a forward navigation UI when history is available
 *
 * ## Example
 *
 * ```kotlin
 * val stackList = NavStackList(
 *   entries = listOf(DetailsScreen, ListScreen, HomeScreen),
 *   currentIndex = 1  // Currently at ListScreen
 * )
 *
 * // Access current and boundary entries
 * val current = stackList.current      // ListScreen
 * val top = stackList.top             // DetailsScreen (newest)
 * val root = stackList.root           // HomeScreen (oldest)
 *
 * // Check navigation position
 * val canGoForward = stackList.forwardStack().isNotEmpty()  // true (can go to DetailsScreen)
 * val canGoBack = stackList.backwardStack().size > 1        // true (can go to HomeScreen)
 *
 * // Iterate forward history
 * stackList.forwardStack().forEach { screen ->
 *   // Process DetailsScreen
 * }
 * ```
 *
 * @param T The type of entries stored in the navigation stack, typically implementing [NavArgument]
 * @param entries The complete list of entries, ordered from top (newest, index 0) to root (oldest,
 *   last index)
 * @param currentIndex The index of the currently active/visible entry in [entries]. Defaults to 0
 *   (top).
 * @see NavDecoration
 * @see NavArgument
 */
public data class NavStackList<T>(val entries: List<T>, val currentIndex: Int = 0) {
  /** The number of entries in the stack. */
  public val size: Int
    get() = entries.size

  /** The currently active/visible entry at [currentIndex]. */
  public val current: T
    get() = entries[currentIndex]

  /** The top (newest) entry in the stack at index 0. Could be in the forward history. */
  public val top: T
    get() = entries.first()

  /** The root (oldest) entry in the stack at the last index. */
  public val root: T
    get() = entries.last()

  /** Retrieves the entry at the given [index]. */
  public operator fun get(index: Int): T = entries[index]

  /**
   * Returns entries between the current position and the top, in order from nearest to top. These
   * are entries the user can navigate forward to (toward newer entries).
   *
   * Empty when [currentIndex] is 0 (at top).
   *
   * Example:
   * ```
   * entries = [A, B, C, D], currentIndex = 2 (at C)
   * forwardStack() = [B, A]  // Can move forward through B to reach A
   * ```
   */
  public fun forwardStack(): List<T> = entries.subList(0, currentIndex).asReversed()

  /**
   * Returns entries from the current position to the root, in order from current to root. This
   * includes the current entry and all entries the user can navigate backward to (toward older
   * entries).
   *
   * Example:
   * ```
   * entries = [A, B, C, D], currentIndex = 1 (at B)
   * backwardStack() = [B, C, D]  // Current B, can move back through C to D
   * ```
   */
  // todo Should this include the current entry?
  public fun backwardStack(): List<T> = entries.subList(currentIndex, entries.size)
}

/** Argument provided to [NavDecoration] that exposes the underlying [Screen]. */
public interface NavArgument {
  public val screen: Screen
  public val key: String
}

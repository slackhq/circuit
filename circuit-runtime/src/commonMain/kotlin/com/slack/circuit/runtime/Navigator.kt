// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.Snapshot
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/**
 * A minimal navigation interface that only supports forward navigation via [goTo].
 *
 * @see Navigator for the full navigation interface
 */
@Stable
public interface GoToNavigator {
  /**
   * Navigate to the [screen].
   *
   * @return true if the navigator successfully navigated to the [screen], false if the navigation
   *   was rejected.
   */
  public fun goTo(screen: Screen): Boolean
}

/** A basic navigation interface for navigating between [screens][Screen]. */
@Stable
public interface Navigator : GoToNavigator {
  public override fun goTo(screen: Screen): Boolean

  /**
   * Move forward in navigation history toward the top.
   *
   * @return true if moved forward, false if already at the top or no forward history exists
   */
  public fun forward(): Boolean

  /**
   * Move backward in navigation history toward the root.
   *
   * @return true if moved backward, false if already at the root
   */
  public fun backward(): Boolean

  /**
   * Remove and return the current screen from the navigation stack.
   *
   * @param result Optional result to pass back to the previous screen
   * @return The removed screen, or null if the stack is empty
   */
  public fun pop(result: PopResult? = null): Screen?

  /** Returns current top most screen of backstack, or null if the navStack is empty. */
  public fun peek(): Screen?

  /** Returns the current back stack. */
  public fun peekBackStack(): List<Screen>

  /** Returns a snapshot of the current navigation stack, or null if empty. */
  public fun peekNavStack(): NavStackList<Screen>?

  /**
   * Clear the existing backstack of [screens][Screen] and navigate to [newRoot].
   *
   * This is useful in preventing the user from returning to a completed workflow, such as a
   * tutorial, wizard, or authentication flow.
   *
   * Example
   *
   * ```kotlin
   * val navigator = Navigator()
   * navigator.push(LoginScreen1)
   * navigator.push(LoginScreen2)
   *
   * // Login flow is complete. Wipe backstack and set new root screen
   * val loginScreens = navigator.resetRoot(HomeScreen)
   * ```
   *
   * ## Multiple back stacks
   *
   * The [StateOptions] parameter enable functionality what is commonly called 'multiple back
   * stacks'. By optionally saving, and later restoring the back stack, you can enable different
   * root screens to have their own back stacks. A common use case is with the bottom navigation bar
   * UX pattern.
   *
   * ```kotlin
   * val options = StateOptions.SaveAndRestore
   * navigator.resetRoot(HomeNavTab1, options)
   * // User navigates to a details screen
   * navigator.push(EntityDetails(id = foo))
   * // Later, user clicks on a bottom navigation item
   * navigator.resetRoot(HomeNavTab2, options)
   * // Later, user switches back to the first navigation item
   * navigator.resetRoot(HomeNavTab1, options)
   * // The existing back stack is restored, and EntityDetails(id = foo) will be top of
   * // the back stack
   * ```
   *
   * There are times when saving and restoring the back stack may not be appropriate, so use this
   * feature only when it makes sense. A common example where it probably does not make sense is
   * launching screens which define a UX flow which has a defined completion, such as onboarding.
   *
   * @param newRoot The new root [Screen]
   * @param options The [StateOptions] to use.
   * @return The backstack before it was reset.
   */
  public fun resetRoot(newRoot: Screen, options: StateOptions = StateOptions.Default): List<Screen>

  /**
   * A holder for the state management configuration for a [Navigator.resetRoot] call.
   *
   * @property save Whether to save the current entry list. It can be restored by passing the
   *   current root [Screen] to [Navigator.resetRoot] with `restoreState = true`
   * @property restore Whether any previously saved state for the new root [Screen] should be
   *   restored. If this is `false` or there is no previous state, the back stack will only contain
   *   the new root [Screen].
   * @property clear Whether any previously saved state for the new root [Screen] should be cleared.
   *   Will be cleared after any restore regardless of [restore].
   */
  public data class StateOptions(
    val save: Boolean = false,
    val restore: Boolean = false,
    val clear: Boolean = false,
  ) {
    public companion object {
      /**
       * Default options for the "single back stack" pattern. Saves nothing and restores nothing.
       */
      public val Default: StateOptions = StateOptions()

      /**
       * Standard options for the "multiple back stacks" pattern. Saves the current backstack and
       * then if possible restores state for the new root screen.
       */
      public val SaveAndRestore: StateOptions = StateOptions(save = true, restore = true)
    }
  }

  /** A no-op implementation of [Navigator] that performs no actual navigation. */
  public object NoOp : Navigator {
    override fun goTo(screen: Screen): Boolean = true

    override fun forward(): Boolean = false

    override fun backward(): Boolean = false

    override fun pop(result: PopResult?): Screen? = null

    override fun peek(): Screen? = null

    override fun peekBackStack(): List<Screen> = emptyList()

    override fun peekNavStack(): NavStackList<Screen>? = null

    override fun resetRoot(newRoot: Screen, options: StateOptions): List<Screen> = emptyList()
  }
}

/** Parameter based alternate for [Navigator.resetRoot]. */
public fun Navigator.resetRoot(
  newRoot: Screen,
  saveState: Boolean = false,
  restoreState: Boolean = false,
  clearState: Boolean = false,
): List<Screen> =
  resetRoot(
    newRoot = newRoot,
    options = Navigator.StateOptions(save = saveState, restore = restoreState, clear = clearState),
  )

/**
 * Clear the existing backstack of [screens][Screen] and navigate to [newRoot].
 *
 * This is useful in preventing the user from returning to a completed workflow, such as a tutorial,
 * wizard, or authentication flow.
 *
 * This version of the function provides easy to lambdas for [saveState], [restoreState], and
 * [clearState] allowing computation of the values based on the current root screen.
 */
public inline fun Navigator.resetRoot(
  newRoot: Screen,
  saveState: (currentRoot: Screen?) -> Boolean = { false },
  restoreState: (currentRoot: Screen?) -> Boolean = { false },
  clearState: (currentRoot: Screen?) -> Boolean = { false },
): List<Screen> {
  val root = peekBackStack().lastOrNull()
  return resetRoot(
    newRoot = newRoot,
    options =
      Navigator.StateOptions(
        save = saveState(root),
        restore = restoreState(root),
        clear = clearState(root),
      ),
  )
}

/** Calls [Navigator.pop] until the given [predicate] is matched or it pops the root. */
public fun Navigator.popUntil(predicate: (Screen) -> Boolean) {
  while (peek()?.let(predicate) == false) pop() ?: break // Break on root pop
}

/** Pop the [Navigator] as if this was the root [Navigator.pop] call. */
public fun Navigator.popRoot(result: PopResult? = null) {
  Snapshot.withMutableSnapshot {
    // If a repeat pop approach is used (like popUntil) then the root backstack item is shown during
    // any root pop handling. This moves the top screen to become the root screen so it remains
    // visible for any final handling.
    val backStack = peekBackStack()
    if (backStack.size > 1) {
      resetRoot(backStack.first())
    }
    pop(result)
  }
}

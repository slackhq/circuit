// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.Snapshot
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** A Navigator that only supports [goTo]. */
@Stable
public interface GoToNavigator {
  /**
   * Navigate to the [screen].
   *
   * @return If the navigator successfully went to the [screen]
   */
  public fun goTo(screen: Screen): Boolean
}

/** A basic navigation interface for navigating between [screens][Screen]. */
@Stable
public interface Navigator : GoToNavigator {
  public override fun goTo(screen: Screen): Boolean

  public fun pop(result: PopResult? = null): Screen?

  /** Returns current top most screen of backstack, or null if backstack is empty. */
  public fun peek(): Screen?

  /** Returns the current back stack. */
  public fun peekBackStack(): ImmutableList<Screen>

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
   * The [saveState] and [restoreState] parameters enable functionality what is commonly called
   * 'multiple back stacks'. By optionally saving, and later restoring the back stack, you can
   * enable different root screens to have their own back stacks. A common use case is with the
   * bottom navigation bar UX pattern.
   *
   * ```kotlin
   * navigator.resetRoot(HomeNavTab1, saveState = true, restoreState = true)
   * // User navigates to a details screen
   * navigator.push(EntityDetails(id = foo))
   * // Later, user clicks on a bottom navigation item
   * navigator.resetRoot(HomeNavTab2, saveState = true, restoreState = true)
   * // Later, user switches back to the first navigation item
   * navigator.resetRoot(HomeNavTab1, saveState = true, restoreState = true)
   * // The existing back stack is restored, and EntityDetails(id = foo) will be top of
   * // the back stack
   * ```
   *
   * There are times when saving and restoring the back stack may not be appropriate, so use this
   * feature only when it makes sense. A common example where it probably does not make sense is
   * launching screens which define a UX flow which has a defined completion, such as onboarding.
   *
   * @param newRoot The new root [Screen]
   * @param saveState Whether to save the current entry list. It can be restored by passing the
   *   current root [Screen] to [resetRoot] with `restoreState = true`
   * @param restoreState Whether any previously saved state for the given [newRoot] should be
   *   restored. If this is `false` or there is no previous state, the back stack will only contain
   *   [newRoot].
   */
  public fun resetRoot(
    newRoot: Screen,
    saveState: Boolean = false,
    restoreState: Boolean = false,
  ): ImmutableList<Screen>

  public object NoOp : Navigator {
    override fun goTo(screen: Screen): Boolean = true

    override fun pop(result: PopResult?): Screen? = null

    override fun peek(): Screen? = null

    override fun peekBackStack(): ImmutableList<Screen> = persistentListOf()

    override fun resetRoot(
      newRoot: Screen,
      saveState: Boolean,
      restoreState: Boolean,
    ): ImmutableList<Screen> = persistentListOf()
  }
}

/**
 * Clear the existing backstack of [screens][Screen] and navigate to [newRoot].
 *
 * This is useful in preventing the user from returning to a completed workflow, such as a tutorial,
 * wizard, or authentication flow.
 *
 * This version of the function provides easy to lambdas for [saveState] and [restoreState] allowing
 * computation of the values based on the current root screen.
 */
public inline fun Navigator.resetRoot(
  newRoot: Screen,
  saveState: (currentRoot: Screen?) -> Boolean = { false },
  restoreState: (currentRoot: Screen?) -> Boolean = { false },
): List<Screen> {
  val root = peekBackStack().lastOrNull()
  return resetRoot(
    newRoot = newRoot,
    saveState = saveState(root),
    restoreState = restoreState(root),
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

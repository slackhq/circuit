// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.foundation.internal.BackHandler
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Creates and remembers a [NavigationInterceptor] from a [Navigator].
 *
 * @param navigator The [Navigator] to delegate to.
 * @param interceptors The [NavigationInterceptor]s to intercept navigation events.
 * @param eventListeners The [NavigationEventListener]s to notify.
 * @param notifier An optional [InterceptingNavigator.FailureNotifier] to notify of
 *   [NavigationInterceptor]
 * @param enableBackHandler Whether to enable the back button handler. When enabled back handling
 *   will be triggered on the [InterceptingNavigator] as a [Navigator.pop] to be intercepted.
 * @see Navigator
 * @see InterceptingNavigator
 */
@Composable
public fun rememberInterceptingNavigator(
  navigator: Navigator,
  interceptors: ImmutableList<NavigationInterceptor> = persistentListOf(),
  eventListeners: ImmutableList<NavigationEventListener> = persistentListOf(),
  notifier: InterceptingNavigator.FailureNotifier? = null,
  enableBackHandler: Boolean = true,
): Navigator {
  // Handle the NavigationInterceptors.
  val interceptingNavigator =
    remember(navigator, interceptors, eventListeners, notifier) {
      InterceptingNavigator(
        delegate = navigator,
        interceptors = interceptors,
        eventListeners = eventListeners,
        notifier = notifier,
      )
    }
  // Handle the back button here to get pop events from it.
  if (enableBackHandler) {
    var trigger by remember { mutableStateOf(false) }
    BackHandler(!trigger) {
      // Root pop check to prevent an infinite loop if this is used with the Android variant of
      // rememberCircuitNavigator as that calls `OnBackPressedDispatcher.onBackPressed`. We need to
      // unload this BackHandler from the composition before the root pop is triggered, so delay
      // calling pop until after the next composition.
      if (navigator.peekBackStack().size > 1) {
        interceptingNavigator.pop()
      } else {
        trigger = true
      }
    }
    if (trigger) {
      SideEffect {
        interceptingNavigator.pop()
        trigger = false
      }
    }
  }
  // Handle backstack changed event listeners.
  BackStackChangedEffect(interceptingNavigator, eventListeners)
  return interceptingNavigator
}

/**
 * A Circuit [Navigator] that allows for a [NavigationInterceptor] to intercept navigation events.
 * If navigation is not intercepted it will be passed to the delegate [Navigator]. Any provided
 * [NavigationEventListener] will be notified when the delegate Navigator is used.
 *
 * @param delegate The [Navigator] to delegate to.
 * @param interceptors The [NavigationInterceptor]s to intercept navigation events.
 * @param eventListeners The [NavigationEventListener]s to notify.
 * @param notifier An optional [FailureNotifier] to notify of [NavigationInterceptor] failure.
 * @see Navigator
 * @see NavigationInterceptor
 * @see NavigationEventListener
 */
public class InterceptingNavigator(
  private val delegate: Navigator,
  private val interceptors: ImmutableList<NavigationInterceptor>,
  private val eventListeners: ImmutableList<NavigationEventListener> = persistentListOf(),
  private val notifier: FailureNotifier? = null,
) : Navigator by delegate {

  override fun goTo(screen: Screen): Boolean {
    for (interceptor in interceptors) {
      when (val interceptedResult = interceptor.goTo(screen)) {
        is InterceptedResult.Skipped -> continue
        is InterceptedResult.Success -> {
          if (interceptedResult.consumed) return true
        }
        is InterceptedResult.Failure -> {
          notifier?.goToFailure(interceptedResult)
          if (interceptedResult.consumed) return false
        }
        is InterceptedGoToResult.Rewrite -> {
          // Recurse in case another interceptor wants to intercept the new screen.
          return goTo(interceptedResult.screen)
        }
      }
    }
    eventListeners.forEach { it.goTo(screen) }
    return delegate.goTo(screen)
  }

  override fun pop(result: PopResult?): Screen? {
    val backStack = peekBackStack()
    for (interceptor in interceptors) {
      when (val interceptedResult = interceptor.pop(backStack, result)) {
        is InterceptedResult.Skipped -> continue
        is InterceptedResult.Success -> {
          if (interceptedResult.consumed) return null
        }
        is InterceptedResult.Failure -> {
          notifier?.popFailure(interceptedResult)
          if (interceptedResult.consumed) return null
        }
      }
    }
    eventListeners.forEach { it.pop(backStack, result) }
    return delegate.pop(result)
  }

  override fun resetRoot(
    newRoot: Screen,
    saveState: Boolean,
    restoreState: Boolean,
  ): ImmutableList<Screen> {
    for (interceptor in interceptors) {
      when (val interceptedResult = interceptor.resetRoot(newRoot, saveState, restoreState)) {
        is InterceptedResult.Skipped -> continue
        is InterceptedResult.Success -> {
          if (interceptedResult.consumed) return persistentListOf()
        }
        is InterceptedResult.Failure -> {
          notifier?.rootResetFailure(interceptedResult)
          if (interceptedResult.consumed) return persistentListOf()
        }
        is InterceptedResetRootResult.Rewrite -> {
          // Recurse in case another interceptor wants to intercept the new screen.
          return resetRoot(
            interceptedResult.screen,
            interceptedResult.saveState,
            interceptedResult.restoreState,
          )
        }
      }
    }
    eventListeners.forEach { it.resetRoot(newRoot, saveState, restoreState) }
    return delegate.resetRoot(newRoot, saveState, restoreState)
  }

  /** Notifies of [NavigationInterceptor] failures. Useful for logging or analytics. */
  @Immutable
  public interface FailureNotifier {

    /**
     * Notifies of a [InterceptedResult.Failure] from a [NavigationInterceptor] during a
     * [NavigationInterceptor.goTo].
     */
    public fun goToFailure(interceptedResult: InterceptedResult.Failure)

    /**
     * Notifies of a [InterceptedResult.Failure] from a [NavigationInterceptor] during a
     * [NavigationInterceptor.pop].
     */
    public fun popFailure(interceptedResult: InterceptedResult.Failure)

    /**
     * Notifies of a [InterceptedResult.Failure] from a [NavigationInterceptor] during a
     * [NavigationInterceptor.resetRoot].
     */
    public fun rootResetFailure(interceptedResult: InterceptedResult.Failure)
  }
}

/** A SideEffect that notifies the [NavigationEventListener] when the backstack changes. */
@Composable
private fun BackStackChangedEffect(
  navigator: Navigator,
  eventListeners: ImmutableList<NavigationEventListener>,
) {
  // Key using the screen as it'll be the same through rotation, as the record key will change.
  val screens = navigator.peekBackStack()
  rememberRetained(screens) { eventListeners.forEach { it.onBackStackChanged(screens) } }
}

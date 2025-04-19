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
 * Creates and remembers a [CircuitNavigationInterceptor] from a [Navigator].
 *
 * @param navigator The [Navigator] to delegate to.
 * @param interceptors The [CircuitNavigationInterceptor]s to intercept navigation events.
 * @param eventListeners The [CircuitNavigationEventListener]s to notify.
 * @param notifier An optional [CircuitInterceptingNavigator.FailureNotifier] to notify of
 *   [CircuitNavigationInterceptor]
 * @param enableBackHandler Whether to enable the back button handler. When enabled back handling
 *   will be triggered on the [CircuitInterceptingNavigator] as a [Navigator.pop] to be intercepted.
 * @see Navigator
 * @see CircuitInterceptingNavigator
 */
@Composable
public fun rememberCircuitInterceptingNavigator(
  navigator: Navigator,
  interceptors: ImmutableList<CircuitNavigationInterceptor> = persistentListOf(),
  eventListeners: ImmutableList<CircuitNavigationEventListener> = persistentListOf(),
  notifier: CircuitInterceptingNavigator.FailureNotifier? = null,
  enableBackHandler: Boolean = true,
): Navigator {
  // Handle the NavigationInterceptors.
  val interceptingNavigator =
    remember(navigator, interceptors, eventListeners, notifier) {
      CircuitInterceptingNavigator(
        delegate = navigator,
        interceptors = interceptors,
        eventListeners = eventListeners,
        notifier = notifier,
      )
    }
  // Handle the back button here to get pop events from it.
  if (enableBackHandler) {
    var trigger by remember { mutableStateOf(false) }
    BackHandler(!trigger && enableBackHandler) {
      // Root pop check to prevent an infinite loop if this is used with the Android variant of
      // rememberCircuitNavigator that calls `OnBackPressedDispatcher.onBackPressed`. We need to
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
 * A Circuit [Navigator] that allows for a [CircuitNavigationInterceptor] to intercept navigation
 * events. If navigation is not intercepted it will be passed to the delegate [Navigator]. Any
 * provided [CircuitNavigationEventListener] will be notified when the delegate Navigator is used.
 *
 * @param delegate The [Navigator] to delegate to.
 * @param interceptors The [CircuitNavigationInterceptor]s to intercept navigation events.
 * @param eventListeners The [CircuitNavigationEventListener]s to notify.
 * @param notifier An optional [FailureNotifier] to notify of [CircuitNavigationInterceptor]
 *   failure.
 * @see Navigator
 * @see CircuitNavigationInterceptor
 * @see CircuitNavigationEventListener
 */
public class CircuitInterceptingNavigator(
  private val delegate: Navigator,
  private val interceptors: ImmutableList<CircuitNavigationInterceptor>,
  private val eventListeners: ImmutableList<CircuitNavigationEventListener> = persistentListOf(),
  private val notifier: FailureNotifier? = null,
) : Navigator by delegate {

  override fun goTo(screen: Screen): Boolean {
    for (interceptor in interceptors) {
      when (val interceptorResult = interceptor.goTo(screen)) {
        is InterceptorResult.Skipped -> continue
        is InterceptorResult.Success -> {
          if (interceptorResult.consumed) return true
        }
        is InterceptorResult.Failure -> {
          notifier?.goToInterceptorFailure(interceptorResult)
          if (interceptorResult.consumed) return false
        }
        is InterceptorGoToResult.Rewrite -> {
          // Recurse in case another interceptor wants to intercept the new screen.
          return goTo(interceptorResult.screen)
        }
      }
    }
    eventListeners.forEach { it.goTo(screen) }
    return delegate.goTo(screen)
  }

  override fun pop(result: PopResult?): Screen? {
    val backStack = peekBackStack()
    for (interceptor in interceptors) {
      when (val interceptorResult = interceptor.pop(backStack, result)) {
        is InterceptorResult.Skipped -> continue
        is InterceptorResult.Success -> {
          if (interceptorResult.consumed) return null
        }
        is InterceptorResult.Failure -> {
          notifier?.popInterceptorFailure(interceptorResult)
          if (interceptorResult.consumed) return null
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
      when (val interceptorResult = interceptor.resetRoot(newRoot, saveState, restoreState)) {
        is InterceptorResult.Skipped -> continue
        is InterceptorResult.Success -> {
          if (interceptorResult.consumed) return persistentListOf()
        }
        is InterceptorResult.Failure -> {
          notifier?.rootResetInterceptorFailure(interceptorResult)
          if (interceptorResult.consumed) return persistentListOf()
        }
        is InterceptorResetRootResult.Rewrite -> {
          // Recurse in case another interceptor wants to intercept the new screen.
          return resetRoot(
            interceptorResult.screen,
            interceptorResult.saveState,
            interceptorResult.restoreState,
          )
        }
      }
    }
    eventListeners.forEach { it.resetRoot(newRoot, saveState, restoreState) }
    return delegate.resetRoot(newRoot, saveState, restoreState)
  }

  /** Notifies of [CircuitNavigationInterceptor] failures. Useful for logging or analytics. */
  @Immutable
  public interface FailureNotifier {

    /**
     * Notifies of a [InterceptorResult.Failure] from a [CircuitNavigationInterceptor] during a
     * [CircuitNavigationInterceptor.goTo].
     */
    public fun goToInterceptorFailure(interceptorResult: InterceptorResult.Failure)

    /**
     * Notifies of a [InterceptorResult.Failure] from a [CircuitNavigationInterceptor] during a
     * [CircuitNavigationInterceptor.pop].
     */
    public fun popInterceptorFailure(interceptorResult: InterceptorResult.Failure)

    /**
     * Notifies of a [InterceptorResult.Failure] from a [CircuitNavigationInterceptor] during a
     * [CircuitNavigationInterceptor.resetRoot].
     */
    public fun rootResetInterceptorFailure(interceptorResult: InterceptorResult.Failure)
  }
}

/** A SideEffect that notifies the [CircuitNavigationEventListener] when the backstack changes. */
@Composable
private fun BackStackChangedEffect(
  navigator: Navigator,
  eventListeners: ImmutableList<CircuitNavigationEventListener>,
) {
  // Key using the screen as it'll be the same through rotation, as the record key will change.
  val screens = navigator.peekBackStack()
  rememberRetained(screens) { eventListeners.forEach { it.onBackStackChanged(screens) } }
}

// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

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
@OptIn(ExperimentalComposeUiApi::class)
@Composable
public fun rememberInterceptingNavigator(
  navigator: Navigator,
  interceptors: List<NavigationInterceptor> = emptyList(),
  eventListeners: List<NavigationEventListener> = emptyList(),
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
    // Check the screen and not the record as `popRoot()` reorders the screens creating new records.
    // Also `popUntil` can run to a null screen, which we want to treat as the last screen.
    val hasScreenChanged = remember {
      var lastScreen: Screen? = navigator.peek()
      derivedStateOf {
        val screen = navigator.peek()
        if (screen != null && screen != lastScreen) {
          lastScreen = screen
        }
        lastScreen
      }
    }
    var hasPendingRootPop by remember(hasScreenChanged) { mutableStateOf(false) }
    var enableRootBackHandler by remember(hasScreenChanged) { mutableStateOf(true) }
    BackHandler(enableRootBackHandler) {
      // Root pop check to prevent an infinite loop if this is used with the Android variant of
      // rememberCircuitNavigator as that calls `OnBackPressedDispatcher.onBackPressed`. We need to
      // unload this BackHandler from the composition before the root pop is triggered, so delay
      // calling pop until after the next composition.
      if (navigator.peekBackStack().size > 1) {
        interceptingNavigator.pop()
      } else {
        hasPendingRootPop = true
        enableRootBackHandler = false
      }
    }
    if (hasPendingRootPop) {
      SideEffect {
        interceptingNavigator.pop()
        hasPendingRootPop = false
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
  private val interceptors: List<NavigationInterceptor>,
  private val eventListeners: List<NavigationEventListener> = emptyList(),
  private val notifier: FailureNotifier? = null,
) : Navigator by delegate {

  override fun goTo(screen: Screen): Boolean {
    val navigationContext = InterceptingNavigationContext(this)
    for (interceptor in interceptors) {
      when (val interceptedResult = interceptor.goTo(screen, navigationContext)) {
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
    eventListeners.forEach { it.goTo(screen, navigationContext) }
    return delegate.goTo(screen)
  }

  override fun pop(result: PopResult?): Screen? {
    val navigationContext = InterceptingNavigationContext(this)
    for (interceptor in interceptors) {
      when (val interceptedResult = interceptor.pop(result, navigationContext)) {
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
    eventListeners.forEach { it.pop(result, navigationContext) }
    return delegate.pop(result)
  }

  override fun resetRoot(newRoot: Screen, options: StateOptions): List<Screen> {
    val navigationContext = InterceptingNavigationContext(this)
    for (interceptor in interceptors) {
      when (val interceptedResult = interceptor.resetRoot(newRoot, options, navigationContext)) {
        is InterceptedResult.Skipped -> continue
        is InterceptedResult.Success -> {
          if (interceptedResult.consumed) return emptyList()
        }
        is InterceptedResult.Failure -> {
          notifier?.rootResetFailure(interceptedResult)
          if (interceptedResult.consumed) return emptyList()
        }
        is InterceptedResetRootResult.Rewrite -> {
          // Recurse in case another interceptor wants to intercept the new screen.
          return resetRoot(interceptedResult.screen, interceptedResult.stateOptions)
        }
      }
    }
    eventListeners.forEach { it.resetRoot(newRoot, options, navigationContext) }
    return delegate.resetRoot(newRoot, options)
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

private class InterceptingNavigationContext(
  private val interceptingNavigator: InterceptingNavigator
) : NavigationContext {

  override fun peek(): Screen? = interceptingNavigator.peek()

  override fun peekBackStack(): List<Screen> = interceptingNavigator.peekBackStack()
}

/** A SideEffect that notifies the [NavigationEventListener] when the backstack changes. */
@Composable
private fun BackStackChangedEffect(
  navigator: InterceptingNavigator,
  eventListeners: List<NavigationEventListener>,
) {
  // Key using the screens as it'll be the same through rotation, as the record key will change.
  val screens = navigator.peekBackStack()
  rememberRetained(screens) {
    val navigationContext = InterceptingNavigationContext(navigator)
    eventListeners.forEach { it.onBackStackChanged(screens, navigationContext) }
  }
}

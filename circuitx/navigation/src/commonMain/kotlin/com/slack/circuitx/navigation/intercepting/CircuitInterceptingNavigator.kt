package com.slack.circuitx.navigation.intercepting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.CircuitNavigationInterceptor.Result
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Creates and remembers a [CircuitNavigationInterceptor].
 *
 * @see CircuitInterceptingNavigator
 */
@Composable
public fun rememberCircuitInterceptingNavigator(
  navigator: Navigator,
  interceptors: ImmutableList<CircuitNavigationInterceptor>,
  eventListeners: ImmutableList<CircuitNavigationEventListener> = persistentListOf(),
  notifier: CircuitInterceptingNavigator.FailureNotifier? = null,
): Navigator {
  return remember(navigator, interceptors, eventListeners, notifier) {
    CircuitInterceptingNavigator(
      delegate = navigator,
      interceptors = interceptors,
      eventListeners = eventListeners,
      notifier = notifier,
    )
  }
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
        is Result.Skipped -> continue
        is Result.Success -> if (interceptorResult.consumed) return true
        is Result.Failure -> {
          notifier?.goToInterceptorFailure(interceptorResult)
          if (interceptorResult.consumed) return false
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
        is Result.Skipped -> continue
        is Result.Success -> if (interceptorResult.consumed) return null
        is Result.Failure -> {
          notifier?.popInterceptorFailure(interceptorResult)
          if (interceptorResult.consumed) return null
        }
      }
    }
    eventListeners.forEach { it.pop(backStack, result) }
    return delegate.pop(result)
  }

  /** Notifies of [CircuitNavigationInterceptor] failures. Useful for logging or analytics. */
  @Immutable
  public interface FailureNotifier {

    /**
     * Notifies of a [Result.Failure] from a [CircuitNavigationInterceptor] during a
     * [CircuitNavigationInterceptor.goTo].
     */
    public fun goToInterceptorFailure(result: Result.Failure)

    /**
     * Notifies of a [Result.Failure] from a [CircuitNavigationInterceptor] during a
     * [CircuitNavigationInterceptor.pop].
     */
    public fun popInterceptorFailure(result: Result.Failure)
  }
}

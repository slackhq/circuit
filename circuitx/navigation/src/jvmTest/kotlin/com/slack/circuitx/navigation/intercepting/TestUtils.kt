package com.slack.circuitx.navigation.intercepting

import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.test.FakeNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal val FailConsumed = InterceptorResult.Failure(consumed = true)
internal val FailUnconsumed = InterceptorResult.Failure(consumed = false)
internal val SuccessUnconsumed = InterceptorResult.Success(consumed = false)

internal fun ComposeContentTestRule.setTestContent(
  interceptors: ImmutableList<CircuitNavigationInterceptor> = persistentListOf(),
  eventListeners: ImmutableList<CircuitNavigationEventListener> = persistentListOf(),
  notifier: CircuitInterceptingNavigator.FailureNotifier? = null,
): Pair<FakeNavigator, Navigator> {
  val navigator = FakeNavigator(TestScreen.RootAlpha)
  lateinit var interceptingNavigator: Navigator
  setContent {
    interceptingNavigator = remember {
      CircuitInterceptingNavigator(navigator, interceptors, eventListeners, notifier)
    }
  }
  return navigator to interceptingNavigator
}

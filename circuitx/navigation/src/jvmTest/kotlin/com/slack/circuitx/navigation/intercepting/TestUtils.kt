package com.slack.circuitx.navigation.intercepting

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.test.FakeNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal val FailConsumed = InterceptedResult.Failure(consumed = true)
internal val FailUnconsumed = InterceptedResult.Failure(consumed = false)
internal val SuccessUnconsumed = InterceptedResult.Success(consumed = false)

internal fun ComposeContentTestRule.setTestContent(
  interceptors: ImmutableList<NavigationInterceptor> = persistentListOf(),
  eventListeners: ImmutableList<NavigationEventListener> = persistentListOf(),
  notifier: InterceptingNavigator.FailureNotifier? = null,
  initialScreen: Screen = TestScreen.RootAlpha,
  additionalScreens: Array<Screen> = arrayOf(),
): Pair<FakeNavigator, Navigator> {
  val navigator = FakeNavigator(initialScreen, *additionalScreens)
  lateinit var interceptingNavigator: Navigator
  setContent {
    interceptingNavigator =
      rememberInterceptingNavigator(navigator, interceptors, eventListeners, notifier)
  }
  return navigator to interceptingNavigator
}

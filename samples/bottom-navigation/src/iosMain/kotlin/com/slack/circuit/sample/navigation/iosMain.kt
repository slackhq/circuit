package com.slack.circuit.sample.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.gesturenavigation.CupertinoGestureNavigationDecorator
import com.slack.circuitx.navigation.intercepting.InterceptedGoToResult
import com.slack.circuitx.navigation.intercepting.LoggingNavigationEventListener
import com.slack.circuitx.navigation.intercepting.LoggingNavigatorFailureNotifier
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor.Companion.SuccessConsumed
import com.slack.circuitx.navigation.intercepting.NavigationLogger
import com.slack.circuitx.navigation.intercepting.rememberInterceptingNavigator
import kotlinx.collections.immutable.persistentListOf
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalComposeUiApi::class)
@Suppress("Unused") // Called from Swift
fun MainViewController(): UIViewController {
  // CircuitX Navigation
  val interceptors = persistentListOf(InfoScreenRewriteInterceptor)
  val eventListeners = persistentListOf(LoggingNavigationEventListener(Logger))
  val notifier = LoggingNavigatorFailureNotifier(Logger)

  val tabs = TabScreen.all
  return ComposeUIViewController {
    MaterialTheme {
      val backStack = rememberSaveableBackStack(tabs.first())
      val navigator = rememberCircuitNavigator(backStack) {}
      // Build the delegate Navigator.
      val interceptingNavigator =
        rememberInterceptingNavigator(
          navigator = navigator,
          interceptors = interceptors,
          eventListeners = eventListeners,
          notifier = notifier,
        )
      val circuit =
        remember(navigator) {
          buildCircuitForTabs(tabs)
            .newBuilder()
            .setAnimatedNavDecoratorFactory(
              CupertinoGestureNavigationDecorator.Factory(
                onBackInvoked = { interceptingNavigator.pop() }
              )
            )
            .build()
        }
      CircuitCompositionLocals(circuit) {
        ContentScaffold(backStack, interceptingNavigator, tabs, Modifier.fillMaxSize())
      }
    }
  }
}

private object InfoScreenRewriteInterceptor : NavigationInterceptor {
  override fun goTo(screen: Screen): InterceptedGoToResult {
    return when (screen) {
      is InfoScreen -> {
        openInfoPage()
        SuccessConsumed
      }
      else -> NavigationInterceptor.Skipped
    }
  }

  private fun openInfoPage() {
    UIApplication.sharedApplication.openURL(
      url = NSURL(string = "https://slackhq.github.io/circuit/"),
      options = emptyMap<Any?, Any>(),
      completionHandler = null,
    )
  }
}

private object Logger : NavigationLogger {
  override fun log(message: String) {
    println("Circuit Navigation: $message")
  }
}

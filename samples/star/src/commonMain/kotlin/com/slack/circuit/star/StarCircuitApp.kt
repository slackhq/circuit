package com.slack.circuit.star

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuit.star.home.HomeScreen
import com.slack.circuit.star.navigation.OpenUrlScreen
import com.slack.circuit.star.ui.StarTheme
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory

@Stable
interface StarAppState {
  val useDarkTheme: Boolean
  val backStack: BackStack<*>
  val navigator: Navigator
}

private class StarAppStateImpl(
  useDarkTheme: Boolean,
  override val backStack: BackStack<*>,
  override val navigator: Navigator,
) : StarAppState {
  override var useDarkTheme: Boolean by mutableStateOf(useDarkTheme)
}

@Composable
fun rememberStarAppState(
  useDarkTheme: Boolean = isSystemInDarkTheme(),
  backStack: BackStack<*> = rememberSaveableBackStack(listOf(HomeScreen)),
  onRootPop: (PopResult?) -> Unit = {},
  navigator: Navigator = rememberCircuitNavigator(backStack, onRootPop = onRootPop),
): StarAppState {
  val uriHandler = LocalUriHandler.current
  val urlAwareNavigator =
    remember(navigator, uriHandler) {
      object : Navigator by navigator {
        override fun goTo(screen: Screen): Boolean {
          println("Going to $screen")
          return when (screen) {
            is OpenUrlScreen -> {
              uriHandler.openUri(screen.url)
              return true
            }
            else -> {
              println("Going to regular $screen")
              navigator.goTo(screen)
            }
          }
        }
      }
    }
  val state =
    remember(useDarkTheme, backStack, urlAwareNavigator) {
      StarAppStateImpl(useDarkTheme, backStack, urlAwareNavigator)
    }
  return state
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun StarCircuitApp(
  circuit: Circuit,
  modifier: Modifier = Modifier,
  state: StarAppState = rememberStarAppState(),
) {
  StarTheme(state.useDarkTheme) {
    // TODO why isn't the windowBackground enough so we don't need to do this?
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
      CircuitCompositionLocals(circuit) {
        SharedElementTransitionLayout {
          ContentWithOverlays {
            NavigableCircuitContent(
              navigator = state.navigator,
              backStack = state.backStack,
              decoratorFactory =
                remember(state.navigator) {
                  GestureNavigationDecorationFactory(onBackInvoked = state.navigator::pop)
                },
            )
          }
        }
      }
    }
  }
}

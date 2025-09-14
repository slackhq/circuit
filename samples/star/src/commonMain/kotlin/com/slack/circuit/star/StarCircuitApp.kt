package com.slack.circuit.star

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuit.star.home.HomeScreen
import com.slack.circuit.star.ui.StarTheme
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun StarCircuitApp(
  circuit: Circuit,
  modifier: Modifier = Modifier,
  useDarkTheme: Boolean = isSystemInDarkTheme(),
  backStack: BackStack<*> = rememberSaveableBackStack(listOf(HomeScreen)),
  onRootPop: (PopResult?) -> Unit = {},
  navigator: Navigator = rememberCircuitNavigator(backStack, onRootPop = onRootPop),
) {
  StarTheme(useDarkTheme) {
    // TODO why isn't the windowBackground enough so we don't need to do this?
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
      CircuitCompositionLocals(circuit) {
        SharedElementTransitionLayout {
          ContentWithOverlays {
            NavigableCircuitContent(
              navigator = navigator,
              backStack = backStack,
              decoratorFactory =
                remember(navigator) {
                  GestureNavigationDecorationFactory(onBackInvoked = navigator::pop)
                },
            )
          }
        }
      }
    }
  }
}

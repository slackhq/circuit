package com.slack.circuit.sample.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.rememberRetainedSaveable
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.InterceptedGoToResult
import com.slack.circuitx.navigation.intercepting.InterceptedPopResult
import com.slack.circuitx.navigation.intercepting.InterceptedResetRootResult
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor.Companion.Skipped
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor.Companion.SuccessConsumed

@Composable
fun rememberAdaptiveNavState(): AdaptiveNavState {
  return rememberRetainedSaveable(saver = AdaptiveNavState.Saver) { AdaptiveNavState() }
}

class AdaptiveNavState : NavigationInterceptor {

  var isOpen by mutableStateOf(false)
    private set

  fun close() {
    isOpen = false
  }

  override fun resetRoot(newRoot: Screen, options: StateOptions): InterceptedResetRootResult {
    isOpen = false
    return Skipped
  }

  override fun goTo(screen: Screen): InterceptedGoToResult {
    isOpen = false
    return Skipped
  }

  override fun pop(peekBackStack: List<Screen>, result: PopResult?): InterceptedPopResult {
    // && peekBackStack.first() is SecondaryScreen) {
    return if (!isOpen && peekBackStack.size == 2) {
      isOpen = true
      SuccessConsumed
    } else Skipped
  }

  object Saver : androidx.compose.runtime.saveable.Saver<AdaptiveNavState, Boolean> {
    override fun SaverScope.save(value: AdaptiveNavState): Boolean {
      return value.isOpen
    }

    override fun restore(value: Boolean): AdaptiveNavState {
      return AdaptiveNavState().apply { isOpen = value }
    }
  }
}

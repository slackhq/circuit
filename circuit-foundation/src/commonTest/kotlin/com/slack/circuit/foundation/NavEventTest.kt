package com.slack.circuit.foundation

import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavEventTest {

  @Test
  fun onNavEvent_pop_callsNavigatorPop() {
    val navigator = FakeNavigator()
    navigator.onNavEvent(NavEvent.Pop(TestPopResult))
    assertEquals(TestPopResult, navigator.lastPopResult)
  }

  @Test
  fun onNavEvent_goTo_callsNavigatorGoTo() {
    val navigator = FakeNavigator()
    navigator.onNavEvent(NavEvent.GoTo(TestScreen))
    assertEquals(TestScreen, navigator.lastGoToScreen)
  }

  @Test
  fun onNavEvent_resetRoot_callsNavigatorResetRoot() {
    val navigator = FakeNavigator()
    navigator.onNavEvent(
      NavEvent.ResetRoot(newRoot = TestScreen, saveState = true, restoreState = false)
    )
    assertEquals(TestScreen, navigator.lastResetRootScreen)
    assertTrue(navigator.lastResetRootSaveState)
    assertTrue(!navigator.lastResetRootRestoreState)
  }
}

private class FakeNavigator : Navigator {
  var lastGoToScreen: Screen? = null
  var lastPopResult: PopResult? = null
  var lastResetRootScreen: Screen? = null
  var lastResetRootSaveState: Boolean = false
  var lastResetRootRestoreState: Boolean = false

  override fun goTo(screen: Screen): Boolean {
    lastGoToScreen = screen
    return true
  }

  override fun pop(result: PopResult?): Screen? {
    lastPopResult = result
    return null
  }

  override fun resetRoot(newRoot: Screen, saveState: Boolean, restoreState: Boolean): List<Screen> {
    lastResetRootScreen = newRoot
    lastResetRootSaveState = saveState
    lastResetRootRestoreState = restoreState
    return emptyList()
  }

  override fun peek() = TODO("Not implemented")

  override fun peekBackStack() = TODO("Not implemented")
}

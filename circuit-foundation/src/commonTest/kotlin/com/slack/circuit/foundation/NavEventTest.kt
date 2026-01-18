// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    val options = StateOptions(save = true, restore = false, clear = true)
    navigator.onNavEvent(NavEvent.ResetRoot(newRoot = TestScreen, options = options))
    assertEquals(TestScreen, navigator.lastResetRootScreen)
    assertEquals(options, navigator.lastResetRootOptions)
  }

  @Test
  @Suppress("DEPRECATION")
  fun onNavEvent_deprecatedResetRoot_callsNavigatorResetRoot() {
    val navigator = FakeNavigator()
    navigator.onNavEvent(
      NavEvent.ResetRoot(newRoot = TestScreen, saveState = true, restoreState = true)
    )
    assertEquals(TestScreen, navigator.lastResetRootScreen)
    val options = navigator.lastResetRootOptions!!
    assertTrue(options.save)
    assertTrue(options.restore)
    assertFalse(options.clear) // default value
  }

  @Test
  fun onNavEvent_forward_callsNavigatorForward() {
    val navigator = FakeNavigator()
    navigator.onNavEvent(NavEvent.Forward)
    assertTrue(navigator.forwardCalled)
  }

  @Test
  fun onNavEvent_backward_callsNavigatorBackward() {
    val navigator = FakeNavigator()
    navigator.onNavEvent(NavEvent.Backward)
    assertTrue(navigator.backwardCalled)
  }
}

private class FakeNavigator : Navigator {
  var lastGoToScreen: Screen? = null
  var lastPopResult: PopResult? = null
  var lastResetRootScreen: Screen? = null
  var lastResetRootOptions: StateOptions? = null
  var forwardCalled = false
  var backwardCalled = false

  override fun goTo(screen: Screen): Boolean {
    lastGoToScreen = screen
    return true
  }

  override fun forward(): Boolean {
    forwardCalled = true
    return true
  }

  override fun backward(): Boolean {
    backwardCalled = true
    return true
  }

  override fun pop(result: PopResult?): Screen? {
    lastPopResult = result
    return null
  }

  override fun resetRoot(newRoot: Screen, options: StateOptions): List<Screen> {
    lastResetRootScreen = newRoot
    lastResetRootOptions = options
    return emptyList()
  }

  override fun peek() = TODO("Not implemented")

  override fun peekBackStack() = TODO("Not implemented")

  override fun peekNavStack(): NavStackList<Screen> = TODO("Not implemented")
}

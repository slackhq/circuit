// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(ComposeUiTestRunner::class)
class RememberCircuitNavigatorTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `rememberCircuitNavigator creates navigator with correct initial state`() =
    composeTestRule.run {
      val backStack = FakeBackStack()
      backStack.push(TestScreen)
      setContent {
        val navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
        assertNotNull(navigator)
        assertEquals(TestScreen, navigator.peek())
        assertEquals(listOf(TestScreen), navigator.peekBackStack())
      }
    }

  @Test
  fun `rememberCircuitNavigator creates new navigator when backstack instance changes`() =
    composeTestRule.run {
      val navigators = mutableSetOf<Navigator>()
      setContent {
        var backStack by remember { mutableStateOf(FakeBackStack().apply { push(TestScreen) }) }
        rememberCircuitNavigator(backStack = backStack, onRootPop = {}).also { navigators += it }
        SideEffect {
          // Simulate the backstack instance changing
          if (backStack.rootRecord?.screen != TestScreen2) {
            backStack = FakeBackStack().apply { push(TestScreen2) }
          }
        }
      }
      assertEquals(2, navigators.size)
      navigators.single { it.peek() == TestScreen }
      navigators.single { it.peek() == TestScreen2 }
      Unit
    }

  @Test
  fun `rememberCircuitNavigator always calls the correct onRootPop lambda`() =
    composeTestRule.run {
      val backStack = FakeBackStack()
      backStack.push(TestScreen)
      var onRootPopped = 0
      val onRootPop1 = { _: PopResult? -> onRootPopped = 1 }
      val onRootPop2 = { _: PopResult? -> onRootPopped = 2 }
      setContent {
        var onRootPop by remember { mutableStateOf(onRootPop1) }
        val navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = onRootPop)
        SideEffect {
          // Call pop on the root screen to trigger the onRootPop lambda
          if (onRootPopped == 0 && onRootPop == onRootPop2) {
            navigator.pop()
          }
          // Simulate the onRootPop changing
          if (onRootPop == onRootPop1) {
            onRootPop = onRootPop2
          }
        }
      }
      assertEquals(2, onRootPopped)
    }
}

private class FakeBackStack : BackStack<BackStack.Record> {

  private val stack = ArrayDeque<BackStack.Record>()
  override val size: Int
    get() = stack.size

  override val topRecord: BackStack.Record?
    get() = stack.firstOrNull()

  override val rootRecord: BackStack.Record?
    get() = stack.lastOrNull()

  override fun push(record: BackStack.Record): Boolean {
    return stack.add(record)
  }

  override fun push(screen: Screen): Boolean {
    return stack.add(FakeBackStackRecord(screen.toString(), screen))
  }

  override fun pop(): BackStack.Record? {
    return stack.removeFirstOrNull()
  }

  override fun saveState() = Unit

  override fun restoreState(screen: Screen): Boolean = false

  override fun peekState(): List<Screen> = emptyList()

  override fun removeState(screen: Screen): Boolean = false

  override fun containsRecord(record: BackStack.Record, includeSaved: Boolean): Boolean =
    record in stack

  override fun isRecordReachable(key: String, depth: Int, includeSaved: Boolean): Boolean {
    return stack.firstNotNullOfOrNull { record -> record.key.takeIf { it == key } } != null
  }

  override fun iterator(): Iterator<BackStack.Record> {
    return stack.iterator()
  }

  class FakeBackStackRecord(override val key: String, override val screen: Screen) :
    BackStack.Record
}

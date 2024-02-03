// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.runtime.popUntil
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(ComposeUiTestRunner::class)
class NavigatorTestUtil {
  @Test
  fun errorWhenBackstackIsEmpty() {
    val backStack = SaveableBackStack()
    val t = assertFailsWith<IllegalStateException> { NavigatorImpl(backStack) {} }
    assertThat(t).hasMessageThat().contains("Backstack size must not be empty.")
  }

  @Test
  fun popAtRoot() {
    val backStack = SaveableBackStack()
    backStack.push(TestScreen)
    backStack.push(TestScreen)

    var onRootPop = 0
    val navigator = NavigatorImpl(backStack) { onRootPop++ }

    assertThat(backStack).hasSize(2)
    assertThat(onRootPop).isEqualTo(0)

    navigator.pop()
    assertThat(backStack).hasSize(1)
    assertThat(onRootPop).isEqualTo(0)

    navigator.pop()
    assertThat(backStack).hasSize(1)
    assertThat(onRootPop).isEqualTo(1)
  }

  @Test
  fun resetRoot() {
    val backStack = SaveableBackStack()
    backStack.push(TestScreen)
    backStack.push(TestScreen2)

    val navigator = NavigatorImpl(backStack) { fail() }

    assertThat(backStack).hasSize(2)

    val oldScreens = navigator.resetRoot(TestScreen3)

    assertThat(backStack).hasSize(1)
    assertThat(backStack.topRecord?.screen).isEqualTo(TestScreen3)
    assertThat(oldScreens).hasSize(2)
    assertThat(oldScreens).isEqualTo(listOf(TestScreen2, TestScreen))
  }

  @Test
  fun popUntil() {
    val backStack = SaveableBackStack()
    backStack.push(TestScreen)
    backStack.push(TestScreen2)
    backStack.push(TestScreen3)

    val navigator = NavigatorImpl(backStack) { fail() }

    assertThat(backStack).hasSize(3)

    navigator.popUntil { it == TestScreen2 }

    assertThat(backStack).hasSize(2)
    assertThat(backStack.topRecord?.screen).isEqualTo(TestScreen2)
  }

  @Test
  fun peek() {
    val backStack = SaveableBackStack()
    backStack.push(TestScreen)
    backStack.push(TestScreen2)

    val navigator = NavigatorImpl(backStack) { fail() }

    assertEquals(TestScreen2, navigator.peek())
  }
}

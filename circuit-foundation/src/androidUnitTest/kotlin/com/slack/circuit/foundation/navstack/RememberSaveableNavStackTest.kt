// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.navstack

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.v2.runComposeUiTest
import com.slack.circuit.foundation.ComposeUiTestRunner
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.runtime.navigation.NavStack
import com.slack.circuit.runtime.navigation.navStackListOf
import com.slack.circuit.runtime.navigation.transform
import com.slack.circuit.runtime.screen.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.runner.RunWith

@RunWith(ComposeUiTestRunner::class)
class RememberSaveableNavStackTest {

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun test_saveable_save_and_restore() = runComposeUiTest {
    val restorationTester = StateRestorationTester(this)
    lateinit var navStack: NavStack<*>
    restorationTester.setContent { navStack = rememberSaveableNavStack(TestScreen.RootAlpha) }
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)
    navStack.backward()

    restorationTester.emulateSaveAndRestore()

    val expected =
      navStackListOf<Screen>(
        listOf(TestScreen.ScreenB),
        TestScreen.ScreenA,
        listOf(TestScreen.RootAlpha),
      )
    val snapshot = assertNotNull(navStack.snapshot()).transform { it.screen }
    assertEquals(expected, snapshot)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun test_saveable_save_and_restore_with_navstack_state() = runComposeUiTest {
    val restorationTester = StateRestorationTester(this)
    lateinit var navStack: NavStack<*>
    restorationTester.setContent { navStack = rememberSaveableNavStack(TestScreen.RootAlpha) }

    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)
    navStack.backward()

    // Now save state and pop everything off
    navStack.saveState()
    navStack.popUntil { false }
    navStack.push(TestScreen.RootBeta)
    navStack.push(TestScreen.ScreenC)

    restorationTester.emulateSaveAndRestore()

    // Assert that the entry list contains the correct items
    val expected = navStackListOf<Screen>(TestScreen.ScreenC, TestScreen.RootBeta)
    val snapshot = assertNotNull(navStack.snapshot()).transform { it.screen }
    assertEquals(expected, snapshot)

    navStack.restoreState(TestScreen.RootAlpha)
    // Assert that the saved state store was saved correctly
    val expectedState =
      navStackListOf<Screen>(
        listOf(TestScreen.ScreenB),
        TestScreen.ScreenA,
        listOf(TestScreen.RootAlpha),
      )
    val snapshotState = assertNotNull(navStack.snapshot()).transform { it.screen }
    assertEquals(expectedState, snapshotState)
  }
}

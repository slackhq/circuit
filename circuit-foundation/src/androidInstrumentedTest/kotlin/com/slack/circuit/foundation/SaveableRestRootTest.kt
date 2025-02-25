// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.internal.test.TestContentTags.TAG_LABEL
import com.slack.circuit.internal.test.TestCountPresenter
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.createTestCircuit
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SaveableRestRootTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testReset() = runTest {
    lateinit var navigator: Navigator
    composeTestRule.run {
      setTestContent { TestContent { navigator = it } }
      onNodeWithTag(TAG_LABEL).assertTextEquals(TestScreen.RootAlpha.label)
      mainClock.autoAdvance = false
      navigator.resetRoot(newRoot = TestScreen.ScreenA, saveState = true, restoreState = true)
      mainClock.advanceTimeByFrame()
      navigator.resetRoot(newRoot = TestScreen.ScreenB, saveState = true, restoreState = true)
      mainClock.advanceTimeByFrame()
      navigator.resetRoot(newRoot = TestScreen.ScreenA, saveState = true, restoreState = true)
      mainClock.advanceTimeByFrame()
      onNodeWithTag(TAG_LABEL).assertTextEquals(TestScreen.ScreenB.label)
    }
  }

  @OptIn(ExperimentalSharedTransitionApi::class)
  private fun ComposeContentTestRule.setTestContent(content: @Composable () -> Unit) {
    val circuit = createTestCircuit(rememberType = TestCountPresenter.RememberType.Saveable)
    setContent {
      PreviewSharedElementTransitionLayout { CircuitCompositionLocals(circuit) { content() } }
    }
  }
}

@Composable
private fun TestContent(liftNavigator: (Navigator) -> Unit) {
  Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
    val backStack = rememberSaveableBackStack(root = TestScreen.RootAlpha)
    val navigator = rememberCircuitNavigator(backStack = backStack)
    SideEffect { liftNavigator(navigator) }
    NavigableCircuitContent(navigator, backStack, modifier = Modifier)
  }
}

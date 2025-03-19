// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.internal.test.TestContentTags
import com.slack.circuit.internal.test.TestCountPresenter
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.createTestCircuit
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class GestureSaveableRestRootTest {

  @get:Rule val composeTestRule = createComposeRule()

  /*
  What happens:
  - Record A is set as args
  - Record A removed from args, Record B is args
  - Record A is in composition until transition completes
  - Record B removed from args, Record A is args
  - Record A is not in previousContentProviders
  - Record A has a new content provider created
  - New movable is created for the same spot, even though it can reparent the old one
  - SaveableState is reused (same key) but the old movable hasn't been disposed, so savable it still registered
  - SaveableState crashes
   */
  @Test
  fun testReset() = runTest {
    lateinit var navigator: Navigator
    composeTestRule.run {
      setTestContent { TestContent { navigator = it } }
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals(TestScreen.RootAlpha.label)
      mainClock.autoAdvance = false
      navigator.resetRoot(newRoot = TestScreen.ScreenA, saveState = true, restoreState = true)
      mainClock.advanceTimeByFrame()
      repeat(10) {
        navigator.resetRoot(newRoot = TestScreen.ScreenB, saveState = true, restoreState = true)
        mainClock.advanceTimeByFrame()
        navigator.resetRoot(newRoot = TestScreen.ScreenA, saveState = true, restoreState = true)
        mainClock.advanceTimeByFrame()
        navigator.resetRoot(newRoot = TestScreen.ScreenC, saveState = true, restoreState = true)
        mainClock.advanceTimeByFrame()
        navigator.resetRoot(newRoot = TestScreen.ScreenA, saveState = true, restoreState = true)
        mainClock.advanceTimeByFrame()
        navigator.resetRoot(newRoot = TestScreen.ScreenB, saveState = true, restoreState = true)
        mainClock.advanceTimeByFrame()
      }
      navigator.resetRoot(newRoot = TestScreen.ScreenA, saveState = true, restoreState = true)
      mainClock.autoAdvance = true
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals(TestScreen.ScreenA.label)
      Snapshot.withMutableSnapshot {
        navigator.goTo(TestScreen.RootBeta)
        navigator.goTo(TestScreen.RootAlpha)
      }
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals(TestScreen.RootAlpha.label)
      navigator.resetRoot(newRoot = TestScreen.ScreenB, saveState = false, restoreState = false)
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals(TestScreen.ScreenB.label)
      navigator.resetRoot(newRoot = TestScreen.ScreenA, saveState = false, restoreState = false)
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals(TestScreen.ScreenA.label)
    }
  }

  @SuppressLint("NewApi")
  @OptIn(ExperimentalSharedTransitionApi::class)
  private fun ComposeContentTestRule.setTestContent(content: @Composable () -> Unit) {
    val circuit =
      createTestCircuit(rememberType = TestCountPresenter.RememberType.Saveable)
        .newBuilder()
        .setAnimatedNavDecoratorFactory(NavigatorDefaults.DefaultDecoratorFactory)
        .setAnimatedNavDecoratorFactory(GestureNavigationDecorationFactory {})
        .build()
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

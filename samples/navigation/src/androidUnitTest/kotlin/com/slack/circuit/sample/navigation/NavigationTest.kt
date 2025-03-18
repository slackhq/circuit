// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val ANIMATION_DURATION = 1000

@RunWith(RobolectricTestRunner::class)
class NavigationTest {

  @get:Rule(order = 1) val robolectricRule = AddActivityToRobolectricRule()
  @get:Rule(order = 2) val composeTestRule = createComposeRule()

  @Test
  fun `Test savable, fast multi-root reset`() {
    with(composeTestRule) {
      val tabs = TabScreen.all
      lateinit var backStack: SaveableBackStack
      lateinit var navigator: Navigator
      setTestContent(tabs) {
        backStack = rememberSaveableBackStack(tabs.first())
        navigator = rememberCircuitNavigator(backStack)
        ContentScaffold(backStack, navigator, tabs, Modifier.fillMaxSize())
      }
      val tabNodes = onAllNodesWithTag(ContentTags.TAG_TAB)
      val tabRootNode = tabNodes.filterToOne(hasText(TabScreen.root.label))
      val tab1Node = tabNodes.filterToOne(hasText(TabScreen.screen1.label))
      val tab2Node = tabNodes.filterToOne(hasText(TabScreen.screen2.label))
      val tab3Node = tabNodes.filterToOne(hasText(TabScreen.screen3.label))
      // Check the title
      onNodeWithTag(ContentTags.TAG_LABEL).assertTextEquals(TabScreen.root.label)
      tabRootNode.assertIsSelected()
      // Root backstack
      repeat(6) { onNodeWithTag(ContentTags.TAG_CONTENT).performClick() }
      onNodeWithTag(ContentTags.TAG_LABEL).assertTextEquals(TabScreen.screen2.label)
      // Tab 1 backstack
      tab1Node.performClick()
      repeat(30) { onNodeWithTag(ContentTags.TAG_CONTENT).performClick() }
      onNodeWithTag(ContentTags.TAG_LABEL).assertTextEquals(TabScreen.screen3.label)
      // Tab 2 backstack
      tab2Node.performClick()
      repeat(4) { onNodeWithTag(ContentTags.TAG_CONTENT).performClick() }
      onNodeWithTag(ContentTags.TAG_LABEL).assertTextEquals(TabScreen.screen2.label)
      // Tab 3 backstack
      tab3Node.performClick()
      repeat(4) { onNodeWithTag(ContentTags.TAG_CONTENT).performClick() }
      onNodeWithTag(ContentTags.TAG_LABEL).assertTextEquals(TabScreen.screen3.label)
      // Reset to root
      tabRootNode.performClick()

      mainClock.autoAdvance = false
      repeat(10) {
        // Root -> 1 -> 3 -> 2 -> 3
        tabRootNode.performClick()
        mainClock.advanceTimeBy(200L)

        tab1Node.performClick()
        mainClock.advanceTimeBy(200L)

        tab3Node.performClick()
        mainClock.advanceTimeBy(200L)

        tab2Node.performClick()
        mainClock.advanceTimeBy(200L)

        tab3Node.performClick()
        mainClock.advanceTimeBy(200L)
      }
      repeat(10) {
        // Root -> 1 -> 3 -> 2 -> 3
        tabRootNode.performClick()
        mainClock.advanceTimeBy(200L)

        tab1Node.performClick()
        mainClock.advanceTimeBy(200L)

        tab3Node.performClick()
        mainClock.advanceTimeBy(200L)

        tab2Node.performClick()
        mainClock.advanceTimeBy(200L)

        tab3Node.performClick()
        mainClock.advanceTimeBy(200L)
        if (it == 5) {
          navigator.pop()
          mainClock.advanceTimeBy(200L)
        }
      }
      mainClock.autoAdvance = true
      onNodeWithTag(ContentTags.TAG_LABEL).assertTextEquals(TabScreen.screen2.label)
    }
  }

  @SuppressLint("NewApi")
  @OptIn(ExperimentalSharedTransitionApi::class)
  private fun ComposeContentTestRule.setTestContent(
    tabs: List<TabScreen>,
    content: @Composable () -> Unit,
  ) {
    val circuit =
      buildCircuitForTabs(tabs)
        .newBuilder()
        .setAnimatedNavDecoratorFactory(CrossFadeNavDecoratorFactory(ANIMATION_DURATION))
        .build()
    setContent {
      PreviewSharedElementTransitionLayout { CircuitCompositionLocals(circuit) { content() } }
    }
  }
}

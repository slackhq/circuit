// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.WindowInsets
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.star.home.HomeTestConstants.BOTTOM_NAVIGATION_TAG
import com.slack.circuit.star.home.HomeTestConstants.NAVIGATION_RAIL_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class HomeUiTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  @Config(qualifiers = "w480dp-h360dp-land")
  fun compactHeight_usesNavigationRail() {
    var selectedIndex = 0
    composeTestRule.run {
      setContent {
        HomeNavigationLayout(
          selectedIndex = selectedIndex,
          onSelectedIndex = { selectedIndex = it },
        ) { paddingValues ->
          Box(Modifier.fillMaxSize().padding(paddingValues).testTag(CONTENT_TAG))
        }
      }

      val rail = onNodeWithTag(NAVIGATION_RAIL_TAG).assertIsDisplayed()
      onNodeWithTag(BOTTOM_NAVIGATION_TAG).assertDoesNotExist()
      val railRight = rail.fetchSemanticsNode().boundsInRoot.right
      val contentLeft = onNodeWithTag(CONTENT_TAG).fetchSemanticsNode().boundsInRoot.left
      assertThat(contentLeft).isAtLeast(railRight)

      onNodeWithContentDescription("About", useUnmergedTree = true).performClick()
      assertThat(selectedIndex).isEqualTo(1)
    }
  }

  @Test
  @Config(qualifiers = "w480dp-h800dp-port")
  fun nonCompactHeight_usesBottomNavigation() {
    composeTestRule.run {
      setContent { HomeNavigationLayout(selectedIndex = 0, onSelectedIndex = {}) {} }

      onNodeWithTag(BOTTOM_NAVIGATION_TAG).assertIsDisplayed()
      onNodeWithTag(NAVIGATION_RAIL_TAG).assertDoesNotExist()
    }
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  @Config(qualifiers = "w480dp-h360dp-land")
  fun displayCutout_doesNotExpandNavigationRail() {
    val cutoutWidth = 48.dp
    var cutoutWidthPx = 0
    var hasCutout by mutableStateOf(false)

    composeTestRule.run {
      setContent {
        cutoutWidthPx = with(LocalDensity.current) { cutoutWidth.roundToPx() }
        val windowInsets =
          WindowInsetsCompat.Builder()
            .setInsets(
              WindowInsetsCompat.Type.displayCutout(),
              Insets.of(if (hasCutout) cutoutWidthPx else 0, 0, 0, 0),
            )
            .build()
        DeviceConfigurationOverride(DeviceConfigurationOverride.WindowInsets(windowInsets)) {
          HomeNavigationLayout(selectedIndex = 0, onSelectedIndex = {}) {
            Box(Modifier.fillMaxSize().testTag(CONTENT_TAG))
          }
        }
      }

      val railWithoutCutout = onNodeWithTag(NAVIGATION_RAIL_TAG).fetchSemanticsNode().boundsInRoot
      runOnIdle { hasCutout = true }
      waitForIdle()

      val railWithCutout = onNodeWithTag(NAVIGATION_RAIL_TAG).fetchSemanticsNode().boundsInRoot
      assertThat(railWithCutout.width).isWithin(0.5f).of(railWithoutCutout.width)
      assertThat(railWithCutout.left).isWithin(0.5f).of(0f)
      val contentLeft = onNodeWithTag(CONTENT_TAG).fetchSemanticsNode().boundsInRoot.left
      assertThat(contentLeft).isAtLeast(cutoutWidthPx.toFloat())
    }
  }

  private companion object {
    const val CONTENT_TAG = "home_content"
  }
}

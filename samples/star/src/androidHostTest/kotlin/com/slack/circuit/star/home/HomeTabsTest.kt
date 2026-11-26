// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.home

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuit.star.home.HomeTestConstants.NAVIGATION_RAIL_ITEM_TAG
import com.slack.circuit.star.petlist.PetListScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests that each home tab keeps its own back stack. */
@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeTabsTest {
  @get:Rule val composeTestRule = createComposeRule()

  /** A screen opened from the pet list. */
  private data object OpenedScreen : Screen

  private data class TestState(val open: () -> Unit) : CircuitUiState

  /** The real home screen over stand-ins for the tab roots, which need no pet data. */
  private val circuit =
    Circuit.Builder()
      .addPresenter<HomeScreen, HomeScreen.State> { _, navigator, _ ->
        presenterOf { HomePresenter(navigator) }
      }
      .addUi<HomeScreen, HomeScreen.State> { state, modifier -> HomeContent(state, modifier) }
      .addPresenterFactory { screen, navigator, _ ->
        if (screen is HomeScreen) {
          null
        } else {
          presenterOf { TestState(open = { navigator.goTo(OpenedScreen) }) }
        }
      }
      .addUiFactory { screen, _ ->
        if (screen is HomeScreen) {
          null
        } else {
          ui<TestState> { state, modifier ->
            val tag =
              when (screen) {
                PetListScreen -> PET_LIST_TAG
                OpenedScreen -> OPENED_TAG
                else -> ABOUT_TAG
              }
            BasicText(tag, modifier.testTag(tag).clickable { state.open() })
          }
        }
      }
      .build()

  @Test
  // Wide enough for the navigation rail, and too short for panes
  @Config(qualifiers = "w640dp-h360dp-land")
  fun tabKeepsItsBackStackAcrossATabSwitch() {
    composeTestRule.run {
      setContent {
        CircuitCompositionLocals(circuit, CircuitSaver.NoOp) {
          SharedElementTransitionLayout {
            val backStack = rememberSaveableBackStack(HomeScreen)
            val navigator = rememberCircuitNavigator(backStack) {}
            NavigableCircuitContent(navigator, backStack)
          }
        }
      }
      onNodeWithTag(PET_LIST_TAG).performClick()
      onNodeWithTag(OPENED_TAG).assertIsDisplayed()

      onNodeWithTag("$NAVIGATION_RAIL_ITEM_TAG-1").performClick()
      onNodeWithTag(ABOUT_TAG).assertIsDisplayed()
      onNodeWithTag(OPENED_TAG).assertDoesNotExist()

      // The first tab comes back with the screen that was open in it
      onNodeWithTag("$NAVIGATION_RAIL_ITEM_TAG-0").performClick()
      onNodeWithTag(OPENED_TAG).assertIsDisplayed()
      onNodeWithTag(ABOUT_TAG).assertDoesNotExist()
    }
  }

  private companion object {
    const val PET_LIST_TAG = "pet_list"
    const val OPENED_TAG = "opened"
    const val ABOUT_TAG = "about"
  }
}

// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.internal.test.TestContent
import com.slack.circuit.internal.test.TestContentTags.TAG_COUNT
import com.slack.circuit.internal.test.TestContentTags.TAG_GO_NEXT
import com.slack.circuit.internal.test.TestContentTags.TAG_INCREASE_COUNT
import com.slack.circuit.internal.test.TestContentTags.TAG_LABEL
import com.slack.circuit.internal.test.TestContentTags.TAG_POP
import com.slack.circuit.internal.test.TestCountPresenter
import com.slack.circuit.internal.test.TestEvent
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.TestState
import com.slack.circuit.internal.test.createTestCircuit
import com.slack.circuit.retained.CircuitRetainedSettings
import com.slack.circuit.retained.ExperimentalCircuitRetainedApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.screen.CircuitSaver
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pins the per-record retention lifecycle for first-party `retain {}` values in record content,
 * with both the default animated decoration and EmptyDecoration:
 * - pushing a screen saves the previous record's values instead of retiring them
 * - popping back reclaims the same instance (no re-creation)
 * - the popped record's values are retired
 */
@OptIn(ExperimentalCircuitRetainedApi::class)
@RunWith(ComposeUiTestRunner::class)
class FirstPartyRetainRecordLifecycleTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val events = mutableListOf<String>()
  private val previousUseFirstParty = CircuitRetainedSettings.useFirstParty
  private val previousEnforceRetainObserverCompatibility =
    CircuitRetainedSettings.enforceRetainObserverCompatibility

  private inner class Tracked(val label: String) : RetainObserver {
    override fun onRetained() {
      events += "$label:retained"
    }

    override fun onEnteredComposition() {
      events += "$label:entered"
    }

    override fun onExitedComposition() {
      events += "$label:exited"
    }

    override fun onRetired() {
      events += "$label:retired"
    }

    override fun onUnused() {
      events += "$label:unused"
    }
  }

  @After
  fun tearDown() {
    CircuitRetainedSettings.useFirstParty = previousUseFirstParty
    CircuitRetainedSettings.enforceRetainObserverCompatibility =
      previousEnforceRetainObserverCompatibility
  }

  @Test
  fun perRecordRetentionLifecycleWithAnimatedDecoration() {
    runScenario(useEmptyDecoration = false)
  }

  @Test
  fun perRecordRetentionLifecycleWithEmptyDecoration() {
    runScenario(useEmptyDecoration = true)
  }

  @Test
  fun savedRecordsSurviveUntilHostRetires() {
    CircuitRetainedSettings.useFirstParty = true
    lateinit var navigator: Navigator
    val circuit =
      createTestCircuit(
        presenter = { screen, _ ->
          val label = (screen as TestScreen).label
          presenterOf {
            retain {
              events += "$label:create"
              Tracked(label)
            }
            TestState(0, label) {}
          }
        }
      )
    var showHost by mutableStateOf(true)

    composeTestRule.setContent {
      if (showHost) {
        CircuitCompositionLocals(circuit, CircuitSaver.NoOp) {
          val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
          navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
          NavigableCircuitContent(navigator = navigator, backStack = backStack)
        }
      }
    }

    composeTestRule.onNodeWithTag(TAG_LABEL).assertTextEquals("A")
    composeTestRule.runOnIdle {
      navigator.resetRoot(TestScreen.ScreenB, StateOptions.SaveAndRestore)
    }
    composeTestRule.onNodeWithTag(TAG_LABEL).assertTextEquals("B")
    composeTestRule.waitForIdle()
    assertThat(events).doesNotContain("A:retired")

    composeTestRule.runOnIdle {
      navigator.resetRoot(TestScreen.ScreenA, StateOptions.SaveAndRestore)
    }
    composeTestRule.onNodeWithTag(TAG_LABEL).assertTextEquals("A")
    composeTestRule.waitForIdle()
    assertThat(events.count { it == "A:create" }).isEqualTo(1)
    assertThat(events).doesNotContain("A:retired")

    composeTestRule.runOnIdle { showHost = false }
    composeTestRule.waitForIdle()
    assertThat(events.count { it == "A:retired" }).isEqualTo(1)
    assertThat(events.count { it == "B:retired" }).isEqualTo(1)
  }

  @Test
  fun removedConditionalUiValueRetiresWhileRecordRemainsComposed() {
    CircuitRetainedSettings.useFirstParty = true
    var showRetained by mutableStateOf(true)
    val circuit =
      Circuit.Builder()
        .addPresenter<TestScreen, TestState> { screen, _, _ ->
          presenterOf { TestState(0, screen.label) {} }
        }
        .addUi<TestScreen, TestState> { state, modifier ->
          if (showRetained) {
            retain {
              events += "conditional:create"
              Tracked("conditional")
            }
          }
          TestContent(state, modifier)
        }
        .build()

    composeTestRule.setContent {
      CircuitCompositionLocals(circuit, CircuitSaver.NoOp) {
        val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
        val navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
        NavigableCircuitContent(navigator = navigator, backStack = backStack)
      }
    }

    composeTestRule.onNodeWithTag(TAG_LABEL).assertTextEquals("A")
    assertThat(events.count { it == "conditional:create" }).isEqualTo(1)

    composeTestRule.runOnIdle { showRetained = false }
    composeTestRule.waitForIdle()
    assertThat(events.count { it == "conditional:retired" }).isEqualTo(1)

    composeTestRule.runOnIdle { showRetained = true }
    composeTestRule.waitForIdle()
    assertThat(events.count { it == "conditional:create" }).isEqualTo(2)
  }

  @Test
  fun firstPartyPresenterStateUpdatesWhileActive() {
    CircuitRetainedSettings.useFirstParty = true
    val circuit = createTestCircuit(rememberType = TestCountPresenter.RememberType.FirstPartyRetain)

    composeTestRule.setContent {
      CircuitCompositionLocals(circuit, CircuitSaver.NoOp) {
        val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
        val navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
        NavigableCircuitContent(navigator = navigator, backStack = backStack)
      }
    }

    composeTestRule.onNodeWithTag(TAG_COUNT).assertTextEquals("0")
    composeTestRule.onNodeWithTag(TAG_INCREASE_COUNT).performClick()
    composeTestRule.onNodeWithTag(TAG_COUNT).assertTextEquals("1")
  }

  @Test
  fun navigationInternalsAreRetainObserverCompatible() {
    CircuitRetainedSettings.useFirstParty = true
    CircuitRetainedSettings.enforceRetainObserverCompatibility = true
    val circuit = createTestCircuit()

    composeTestRule.setContent {
      CircuitCompositionLocals(circuit) {
        val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
        val navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
        NavigableCircuitContent(navigator = navigator, backStack = backStack)
      }
    }

    composeTestRule.onNodeWithTag(TAG_LABEL).assertTextEquals("A")
  }

  @Test
  fun retainedPresenterSurvivesInterruptedRootTransition() {
    CircuitRetainedSettings.useFirstParty = true
    lateinit var navigator: Navigator
    val circuit =
      createTestCircuit(
        presenter = { screen, _ ->
          val label = (screen as TestScreen).label
          presenterOf {
            retain {
              events += "$label:create"
              Tracked(label)
            }
            TestState(0, label) {}
          }
        }
      )

    composeTestRule.setContent {
      CircuitCompositionLocals(circuit, CircuitSaver.NoOp) {
        val backStack = rememberSaveableBackStack(TestScreen.RootAlpha)
        navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
        NavigableCircuitContent(navigator = navigator, backStack = backStack)
      }
    }

    composeTestRule.onNodeWithTag(TAG_LABEL).assertTextEquals("Root Alpha")
    composeTestRule.mainClock.autoAdvance = false
    try {
      navigator.resetRoot(TestScreen.ScreenA, StateOptions.SaveAndRestore)
      composeTestRule.mainClock.advanceTimeByFrame()
      repeat(10) {
        navigator.resetRoot(TestScreen.ScreenB, StateOptions.SaveAndRestore)
        composeTestRule.mainClock.advanceTimeByFrame()
        navigator.resetRoot(TestScreen.ScreenA, StateOptions.SaveAndRestore)
        composeTestRule.mainClock.advanceTimeByFrame()
        navigator.resetRoot(TestScreen.ScreenC, StateOptions.SaveAndRestore)
        composeTestRule.mainClock.advanceTimeByFrame()
        navigator.resetRoot(TestScreen.ScreenA, StateOptions.SaveAndRestore)
        composeTestRule.mainClock.advanceTimeByFrame()
        navigator.resetRoot(TestScreen.ScreenB, StateOptions.SaveAndRestore)
        composeTestRule.mainClock.advanceTimeByFrame()
      }
      navigator.resetRoot(TestScreen.ScreenA, StateOptions.SaveAndRestore)
    } finally {
      composeTestRule.mainClock.autoAdvance = true
    }

    composeTestRule.onNodeWithTag(TAG_LABEL).assertTextEquals("A")
    composeTestRule.waitForIdle()
    assertThat(events.count { it == "A:create" }).isEqualTo(1)
    assertThat(events).doesNotContain("A:retired")
  }

  private fun runScenario(useEmptyDecoration: Boolean) {
    CircuitRetainedSettings.useFirstParty = true
    val circuit =
      createTestCircuit(
        presenter = { screen, navigator ->
          val label = (screen as TestScreen).label
          presenterOf {
            retain {
              events += "$label:create"
              Tracked(label)
            }
            TestState(0, label) { event ->
              when (event) {
                TestEvent.GoToNextScreen -> navigator.goTo(TestScreen.ScreenB)
                TestEvent.PopNavigation -> navigator.pop()
                else -> Unit
              }
            }
          }
        }
      )

    composeTestRule.setContent {
      CircuitCompositionLocals(circuit, CircuitSaver.NoOp) {
        val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
        val navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
        if (useEmptyDecoration) {
          NavigableCircuitContent(
            navigator = navigator,
            backStack = backStack,
            decoration = NavigatorDefaults.EmptyDecoration,
          )
        } else {
          NavigableCircuitContent(navigator = navigator, backStack = backStack)
        }
      }
    }

    composeTestRule.onNodeWithTag(TAG_LABEL).assertTextEquals("A")
    composeTestRule.onNodeWithTag(TAG_GO_NEXT).performClick()
    composeTestRule.onNodeWithTag(TAG_LABEL).assertTextEquals("B")
    composeTestRule.waitForIdle()

    // A went to the back stack: its value must be saved, not retired.
    assertThat(events).contains("A:exited")
    assertThat(events).doesNotContain("A:retired")

    composeTestRule.onNodeWithTag(TAG_POP).performClick()
    composeTestRule.onNodeWithTag(TAG_LABEL).assertTextEquals("A")
    composeTestRule.waitForIdle()

    // A was reclaimed, not recreated, and popped B was retired.
    assertThat(events.count { it == "A:create" }).isEqualTo(1)
    assertThat(events).doesNotContain("A:retired")
    assertThat(events).contains("B:retired")
  }
}

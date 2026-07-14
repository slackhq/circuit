// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained.android

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.retain.retainRetainedValuesStoreRegistry
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import leakcanary.DetectLeaksAfterTestSuccess.Companion.detectLeaksAfterTestSuccessWrapping
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * Verifies the interop between Compose's first-party retain scoping
 * (`RetainedValuesStoreRegistry.LocalRetainedValuesStoreProvider`) and `SaveableStateProvider`
 * inside `movableContentOf`, the shape `NavigableCircuitContent` uses per record. This combination
 * corrupted `rememberSaveable` restoration in early retain versions; these tests pin the fixed
 * behavior on device.
 */
class FirstPartyRetainInteropTest {
  private val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val rule =
    RuleChain.emptyRuleChain().detectLeaksAfterTestSuccessWrapping(tag = "ActivitiesDestroyed") {
      around(composeTestRule)
    }

  private val scenario: ActivityScenario<ComponentActivity>
    get() = composeTestRule.activityRule.scenario

  private val observedCounts = mutableMapOf<String, Int>()
  private val setters = mutableMapOf<String, (Int) -> Unit>()
  private val retainedBoxes = mutableMapOf<String, Any>()

  private var currentScreen by mutableStateOf("A")

  @Composable
  private fun CounterScreen(name: String) {
    var count by rememberSaveable { mutableIntStateOf(0) }
    val box = retain { Any() }
    observedCounts[name] = count
    retainedBoxes[name] = box
    setters[name] = { count = it }
  }

  /** The NavigableCircuitContent shape: per-screen movable content, store + saveable scoping. */
  @Composable
  private fun ScreenHost(withStoreProvider: Boolean) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val registry = retainRetainedValuesStoreRegistry()
    val screens = remember {
      listOf("A", "B").associateWith { name ->
        movableContentOf {
          if (withStoreProvider) {
            registry.LocalRetainedValuesStoreProvider(name) {
              saveableStateHolder.SaveableStateProvider(name) { CounterScreen(name) }
            }
          } else {
            saveableStateHolder.SaveableStateProvider(name) { CounterScreen(name) }
          }
        }
      }
    }
    screens.getValue(currentScreen).invoke()
  }

  private fun setActivityContent(withStoreProvider: Boolean) {
    scenario.onActivity { activity ->
      activity.setContent { ScreenHost(withStoreProvider) }
    }
  }

  private fun switchScreens(vararg names: String) {
    for (name in names) {
      composeTestRule.runOnIdle { currentScreen = name }
      composeTestRule.waitForIdle()
    }
  }

  @Test
  fun saveableRestoresAcrossScreenSwitchWithStoreProvider() {
    setActivityContent(withStoreProvider = true)
    composeTestRule.runOnIdle { setters.getValue("A").invoke(5) }
    switchScreens("B", "A", "B", "A")
    composeTestRule.runOnIdle { assertThat(observedCounts["A"]).isEqualTo(5) }
  }

  @Test
  fun saveableRestoresAcrossScreenSwitchWithoutStoreProvider() {
    setActivityContent(withStoreProvider = false)
    composeTestRule.runOnIdle { setters.getValue("A").invoke(5) }
    switchScreens("B", "A")
    composeTestRule.runOnIdle { assertThat(observedCounts["A"]).isEqualTo(5) }
  }

  @Test
  fun retainedValueSurvivesScreenSwitch() {
    setActivityContent(withStoreProvider = true)
    composeTestRule.waitForIdle()
    val original = composeTestRule.runOnIdle { retainedBoxes.getValue("A") }
    switchScreens("B", "A")
    composeTestRule.runOnIdle { assertThat(retainedBoxes["A"]).isSameInstanceAs(original) }
  }

  @Test
  fun retainedAndSaveableSurviveRealRecreation() {
    setActivityContent(withStoreProvider = true)
    composeTestRule.runOnIdle { setters.getValue("A").invoke(11) }
    composeTestRule.waitForIdle()
    val boxBefore = composeTestRule.runOnIdle { retainedBoxes.getValue("A") }

    scenario.recreate()
    setActivityContent(withStoreProvider = true)
    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle {
      assertThat(observedCounts["A"]).isEqualTo(11)
      assertThat(retainedBoxes["A"]).isSameInstanceAs(boxBefore)
    }
  }

  @Test
  fun offScreenSaveableSurvivesRealRecreation() {
    setActivityContent(withStoreProvider = true)
    composeTestRule.runOnIdle { setters.getValue("A").invoke(7) }
    switchScreens("B")

    scenario.recreate()
    setActivityContent(withStoreProvider = true)
    composeTestRule.waitForIdle()

    switchScreens("A")
    composeTestRule.runOnIdle { assertThat(observedCounts["A"]).isEqualTo(7) }
  }
}

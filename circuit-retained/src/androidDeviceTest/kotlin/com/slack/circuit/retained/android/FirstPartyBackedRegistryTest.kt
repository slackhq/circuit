// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained.android

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.retained.CircuitRetainedSettings
import com.slack.circuit.retained.ExperimentalCircuitRetainedApi
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.lifecycleRetainedStateRegistry
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.retained.rememberRetainedSaveable
import leakcanary.DetectLeaksAfterTestSuccess.Companion.detectLeaksAfterTestSuccessWrapping
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * Exercises [lifecycleRetainedStateRegistry] with [CircuitRetainedSettings.useFirstParty] enabled,
 * where retention is delegated to Compose's first-party retain store instead of a Circuit-managed
 * ViewModel.
 */
@OptIn(ExperimentalCircuitRetainedApi::class)
class FirstPartyBackedRegistryTest {
  private val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val rule =
    RuleChain.emptyRuleChain().detectLeaksAfterTestSuccessWrapping(tag = "ActivitiesDestroyed") {
      around(composeTestRule)
    }

  private val scenario: ActivityScenario<ComponentActivity>
    get() = composeTestRule.activityRule.scenario

  private val retainedValues = mutableMapOf<String, Any>()
  private var showContent by mutableStateOf(true)

  init {
    CircuitRetainedSettings.useFirstParty = true
  }

  @After
  fun tearDown() {
    CircuitRetainedSettings.useFirstParty = false
  }

  @Composable
  private fun RetainingContent() {
    CompositionLocalProvider(LocalRetainedStateRegistry provides lifecycleRetainedStateRegistry()) {
      if (showContent) {
        val value = rememberRetained { Any() }
        retainedValues["value"] = value
      }
    }
  }

  private fun setActivityContent() {
    scenario.onActivity { activity -> activity.setContent { RetainingContent() } }
  }

  @Test
  fun retainedValueSurvivesRecreation() {
    setActivityContent()
    composeTestRule.waitForIdle()
    val original = composeTestRule.runOnIdle { retainedValues.getValue("value") }

    scenario.recreate()
    setActivityContent()
    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle {
      assertThat(retainedValues["value"]).isSameInstanceAs(original)
    }
  }

  @Test
  fun retainedValueForgottenWhenContentRemoved() {
    setActivityContent()
    composeTestRule.waitForIdle()
    val original = composeTestRule.runOnIdle { retainedValues.getValue("value") }

    composeTestRule.runOnIdle { showContent = false }
    composeTestRule.waitForIdle()
    composeTestRule.runOnIdle { showContent = true }
    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle {
      assertThat(retainedValues["value"]).isNotSameInstanceAs(original)
    }
  }

  private val counts = mutableMapOf<String, Int>()
  private val countSetters = mutableMapOf<String, (Int) -> Unit>()

  @Composable
  private fun SaveableRetainingContent() {
    CompositionLocalProvider(LocalRetainedStateRegistry provides lifecycleRetainedStateRegistry()) {
      var count by
        rememberRetainedSaveable(stateSaver = autoSaver()) {
          androidx.compose.runtime.mutableIntStateOf(0)
        }
      counts["count"] = count
      countSetters["count"] = { count = it }
    }
  }

  /** The retained+saveable hybrid keeps working with the first-party backing. */
  @Test
  fun retainedSaveableSurvivesRecreation() {
    runRetainedSaveableRecreation()
  }

  /** Control: same content under the ViewModel backing. */
  @Test
  fun retainedSaveableSurvivesRecreationWithViewModelBacking() {
    CircuitRetainedSettings.useFirstParty = false
    runRetainedSaveableRecreation()
  }

  private fun runRetainedSaveableRecreation() {
    // Content must be set from a single source location; positional retained/saveable keys are
    // derived from the content lambda's source position.
    fun setSaveableContent() {
      scenario.onActivity { activity -> activity.setContent { SaveableRetainingContent() } }
    }

    setSaveableContent()
    composeTestRule.runOnIdle { countSetters.getValue("count").invoke(42) }
    composeTestRule.runOnIdle { assertThat(counts["count"]).isEqualTo(42) }

    scenario.recreate()
    setSaveableContent()
    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle { assertThat(counts["count"]).isEqualTo(42) }
  }

  @Test
  fun retainedValueSurvivesRecreationTwice() {
    setActivityContent()
    composeTestRule.waitForIdle()
    val original = composeTestRule.runOnIdle { retainedValues.getValue("value") }

    repeat(2) {
      scenario.recreate()
      setActivityContent()
      composeTestRule.waitForIdle()
    }

    composeTestRule.runOnIdle {
      assertThat(retainedValues["value"]).isSameInstanceAs(original)
    }
  }
}

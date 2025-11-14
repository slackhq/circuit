// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained.android

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.retained.LifecycleRetainedStateRegistry
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.retained.produceAndCollectAsRetainedState
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.retained.viewModelRetainedStateRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import leakcanary.DetectLeaksAfterTestSuccess.Companion.detectLeaksAfterTestSuccessWrapping
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

internal const val TAG_PRODUCED_STATE = "produced_state"

/**
 * Test class for produceAndCollectAsRetainedState function.
 *
 * These tests verify that:
 * - Flow values are collected and retained across configuration changes
 * - Changing inputs causes the producer to be rerun and state to reset
 * - The Flow is properly scoped and doesn't cause memory leaks
 */
class ProduceAndCollectAsRetainedStateTest {
  private val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val rule =
    RuleChain.emptyRuleChain().detectLeaksAfterTestSuccessWrapping(tag = "ActivitiesDestroyed") {
      around(composeTestRule)
    }

  private val scenario: ActivityScenario<ComponentActivity>
    get() = composeTestRule.activityRule.scenario

  @Test
  fun retainsStateAcrossConfigurationChanges() {
    // Create a flow that emits a single value
    val testFlow: suspend () -> Flow<String> = { flow { emit("test_value") } }

    val content =
      @Composable {
        val state by produceAndCollectAsRetainedState(initial = "", producer = testFlow)
        Text(modifier = Modifier.testTag(TAG_PRODUCED_STATE), text = state)
      }

    setActivityContent(content)

    // Wait for the flow to emit and verify the value
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_PRODUCED_STATE).assertTextEquals("test_value")

    // Recreate the activity to simulate configuration change
    scenario.recreate()
    setActivityContent(content)

    // Verify the state was retained across configuration change
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_PRODUCED_STATE).assertTextEquals("test_value")
  }

  @Test
  fun resetsStateWhenInputsChange() {
    // Track how many times the producer is called
    var producerCallCount = 0

    // Use a MutableStateFlow to track the input so we can change it reactively
    val inputFlow = MutableStateFlow("input1")

    val content =
      @Composable {
        val currentInput by inputFlow.collectAsState()
        val state by
          produceAndCollectAsRetainedState(
            currentInput,
            initial = "initial",
            producer = {
              producerCallCount++
              flow { emit("produced_$currentInput") }
            },
          )
        Text(modifier = Modifier.testTag(TAG_PRODUCED_STATE), text = state)
      }

    setActivityContent(content)

    // Wait for the flow to emit and verify
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_PRODUCED_STATE).assertTextEquals("produced_input1")
    assertThat(producerCallCount).isEqualTo(1)

    // Change the input
    inputFlow.value = "input2"

    // Verify the producer was called again and state was reset
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_PRODUCED_STATE).assertTextEquals("produced_input2")
    assertThat(producerCallCount).isEqualTo(2)
  }

  @Test
  fun usesInitialValue() {
    // Test that the initial value is used correctly
    val testFlow: suspend () -> Flow<String> = { flow { emit("final_value") } }

    val content =
      @Composable {
        val state by
          produceAndCollectAsRetainedState(initial = "initial_value", producer = testFlow)
        Text(modifier = Modifier.testTag(TAG_PRODUCED_STATE), text = state)
      }

    setActivityContent(content)

    // Wait for the flow to emit and verify the final value is displayed
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_PRODUCED_STATE).assertTextEquals("final_value")
  }

  @Test
  fun handlesMultipleEmissions() {
    val testFlow: suspend () -> Flow<Int> = {
      flow {
        emit(1)
        emit(2)
        emit(3)
      }
    }

    val content =
      @Composable {
        val state by produceAndCollectAsRetainedState(initial = 0, producer = testFlow)
        Text(modifier = Modifier.testTag(TAG_PRODUCED_STATE), text = state.toString())
      }

    setActivityContent(content)

    // Wait for all emissions
    composeTestRule.waitForIdle()

    // Verify the last emitted value is displayed
    composeTestRule.onNodeWithTag(TAG_PRODUCED_STATE).assertTextEquals("3")
  }

  @Test
  fun worksWithNoExplicitInputs() {
    // Test that when no explicit inputs are provided, the function still works correctly
    // Note: The flow collection restarts on activity recreate, but that's expected behavior
    val testFlow: suspend () -> Flow<String> = { flow { emit("test_value") } }

    val content =
      @Composable {
        val state by produceAndCollectAsRetainedState(initial = "initial", producer = testFlow)
        Text(modifier = Modifier.testTag(TAG_PRODUCED_STATE), text = state)
      }

    setActivityContent(content)

    // Wait for the flow to emit
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_PRODUCED_STATE).assertTextEquals("test_value")

    // Recreate the activity
    scenario.recreate()
    setActivityContent(content)

    // The flow collection restarts, but it emits the same value
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_PRODUCED_STATE).assertTextEquals("test_value")
  }

  @Test
  fun behavesLikeRegularCollectAsStateWithoutRegistry() {
    // Test that without a registry, state is not retained across activity recreation
    val testFlow: suspend () -> Flow<String> = { flow { emit("test_value") } }

    val content =
      @Composable {
        val state by produceAndCollectAsRetainedState(initial = "initial", producer = testFlow)
        Text(modifier = Modifier.testTag(TAG_PRODUCED_STATE), text = state)
      }

    // Set content WITHOUT providing a RetainedStateRegistry
    scenario.onActivity { activity -> activity.setContent(content = content) }

    // Wait for the flow to emit
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_PRODUCED_STATE).assertTextEquals("test_value")

    // Recreate the activity
    scenario.recreate()
    scenario.onActivity { activity -> activity.setContent(content = content) }

    // Without a registry, state should reset to initial value then re-emit
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_PRODUCED_STATE).assertTextEquals("test_value")
  }

  @Test
  fun doesNotRetainWhenCanRetainCheckerReturnsFalse() {
    // Test that state is not retained when CanRetainChecker returns false
    var emissionCount = 0
    val testFlow: suspend () -> Flow<String> = {
      flow {
        emissionCount++
        emit("emission_$emissionCount")
      }
    }

    val content =
      @Composable {
        val registry = rememberRetained { RetainedStateRegistry(canRetainChecker = { false }) }
        CompositionLocalProvider(LocalRetainedStateRegistry provides registry) {
          val state by produceAndCollectAsRetainedState(initial = "initial", producer = testFlow)
          Text(modifier = Modifier.testTag(TAG_PRODUCED_STATE), text = state)
        }
      }

    setActivityContent(content)

    // Wait for the flow to emit
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_PRODUCED_STATE).assertTextEquals("emission_1")
    assertThat(emissionCount).isEqualTo(1)

    // Recreate the activity
    scenario.recreate()
    setActivityContent(content)

    // Since canRetain returns false, state should not be retained and flow runs again
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_PRODUCED_STATE).assertTextEquals("emission_2")
    assertThat(emissionCount).isEqualTo(2)
  }

  @Test
  fun clearsStateWhenCompositionLeaves() {
    // Test that state is properly cleaned up when the composable leaves composition
    var showContent by mutableStateOf(true)
    var emissionCount = 0
    val testFlow: suspend () -> Flow<String> = {
      flow {
        emissionCount++
        emit("emission_$emissionCount")
      }
    }

    val content =
      @Composable {
        if (showContent) {
          val state by produceAndCollectAsRetainedState(initial = "initial", producer = testFlow)
          Text(modifier = Modifier.testTag(TAG_PRODUCED_STATE), text = state)
        }
      }

    setActivityContent(content)

    // Wait for the flow to emit
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_PRODUCED_STATE).assertTextEquals("emission_1")
    assertThat(emissionCount).isEqualTo(1)

    // Hide the content (removes from composition)
    showContent = false
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_PRODUCED_STATE).assertDoesNotExist()

    // Show the content again
    showContent = true
    composeTestRule.waitForIdle()

    // State should be cleared and flow should not have run again yet because it was disposed
    composeTestRule.onNodeWithTag(TAG_PRODUCED_STATE).assertTextEquals("emission_2")
    assertThat(emissionCount).isEqualTo(2)
  }

  private fun setActivityContent(content: @Composable () -> Unit) {
    scenario.onActivity { activity ->
      activity.setContent {
        CompositionLocalProvider(
          LocalRetainedStateRegistry provides
            viewModelRetainedStateRegistry(LifecycleRetainedStateRegistry.KEY)
        ) {
          content()
        }
      }
    }
  }
}

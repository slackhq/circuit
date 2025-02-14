// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** Expected unit tests for [ImpressionEffect] and [LaunchedImpressionEffect]. */
expect class ImpressionEffectTest : ImpressionEffectTestShared {
  @Test override fun recreateImpressionEffect()

  @Test override fun inputChangeImpressionEffect()

  @Test override fun recreateLaunchedImpressionEffect()

  @Test override fun inputChangeLaunchedImpressionEffect()
}

/** Shared unit tests for [ImpressionEffect] and [LaunchedImpressionEffect]. */
internal interface ImpressionEffectTestShared {

  @Test fun recreateImpressionEffect()

  @Test fun inputChangeImpressionEffect()

  @Test fun recreateLaunchedImpressionEffect()

  @Test fun inputChangeLaunchedImpressionEffect()
}

/** Shared implementation of unit tests for [ImpressionEffect] and [LaunchedImpressionEffect]. */
@OptIn(
  ExperimentalCoroutinesApi::class // For advanceUntilIdle()
)
@Ignore // Only run the actual uses of this.
internal class ImpressionEffectTestSharedImpl : ImpressionEffectTestShared {

  private val registry = RetainedStateRegistry()
  private val composed = MutableStateFlow(true)

  override fun recreateImpressionEffect() {
    runTest {
      var count = 0
      moleculeFlow(RecompositionMode.Immediate) {
          setRetainedContent { ImpressionEffect { count++ } }
        }
        .test {
          // Verify the impression block was called once
          assertEquals(1, count)
          // Simulate retained leaving and joining the composition
          recreate()
          // Verify the impression block wasn't called again
          assertEquals(1, count)
          cancelAndIgnoreRemainingEvents()
        }
    }
  }

  override fun inputChangeImpressionEffect() {

    runTest {
      val inputs = MutableStateFlow("init")
      var count = 0
      moleculeFlow(RecompositionMode.Immediate) {
          setRetainedContent {
            val input by inputs.collectAsState()
            ImpressionEffect(input) { count++ }
          }
        }
        .test {
          // Verify the impression block was called once
          assertEquals(1, count)
          // Change the input
          inputs.value = "new"
          advanceUntilIdle()
          // Verify the impression block was called again
          assertEquals(2, count)
          cancelAndIgnoreRemainingEvents()
        }
    }
  }

  override fun recreateLaunchedImpressionEffect() {
    runTest {
      var count = 0
      moleculeFlow(RecompositionMode.Immediate) {
          setRetainedContent {
            LaunchedImpressionEffect {
              delay(1)
              count++
            }
          }
        }
        .test {
          // Advance over the delay and execute
          advanceTimeBy(2)
          // Verify the impression block was called once
          assertEquals(1, count)
          // Simulate retained leaving and joining the composition
          recreate()
          // Advance over the delay and execute
          advanceTimeBy(2)
          // Verify the impression block wasn't called again
          assertEquals(1, count)
          cancelAndIgnoreRemainingEvents()
        }
    }
  }

  override fun inputChangeLaunchedImpressionEffect() {

    runTest {
      val inputs = MutableStateFlow("init")
      var count = 0
      moleculeFlow(RecompositionMode.Immediate) {
          setRetainedContent {
            val input by inputs.collectAsState()
            LaunchedImpressionEffect(input) {
              delay(1)
              count++
            }
          }
        }
        .test {
          // Advance over the delay and execute
          advanceTimeBy(2)
          // Verify the impression block was called once
          assertEquals(1, count)
          // Change the input
          inputs.value = "new"
          // Advance over the delay and execute
          advanceTimeBy(2)
          // Verify the impression block was called again
          assertEquals(2, count)
          cancelAndIgnoreRemainingEvents()
        }
    }
  }

  /** Simulating a screen with this. */
  @Composable
  private fun setRetainedContent(content: @Composable () -> Unit = {}) {
    CompositionLocalProvider(LocalRetainedStateRegistry provides registry) {
      val isCompose by composed.collectAsState(initial = true)
      if (isCompose) {
        content()
      }
    }
  }

  /** Simulate a retained leaving and joining of the composition. */
  private fun recreate() {
    registry.saveAll()
    composed.value = false
    composed.value = true
  }
}

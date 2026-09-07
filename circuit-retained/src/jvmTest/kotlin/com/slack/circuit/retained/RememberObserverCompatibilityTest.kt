// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(
  ExperimentalCircuitRetainedApi::class,
  androidx.compose.ui.test.ExperimentalTestApi::class,
)

package com.slack.circuit.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.ui.test.v2.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Test

class RememberObserverCompatibilityTest {
  private val previousSetting = CircuitRetainedSettings.enforceRetainObserverCompatibility

  @After
  fun tearDown() {
    CircuitRetainedSettings.enforceRetainObserverCompatibility = previousSetting
  }

  @Test
  fun rememberRetainedRejectsRememberObserver() {
    assertRememberObserverRejected { rememberRetained { TestRememberObserver() } }
  }

  @Test
  fun keyedRetainRejectsRememberObserver() {
    assertRememberObserverRejected { retain(key = "observer") { TestRememberObserver() } }
  }

  @Test
  fun rememberRetainedRejectsRememberObserverWithoutRetainedRegistry() {
    assertRememberObserverRejected(installRetainedRegistry = false) {
      rememberRetained { TestRememberObserver() }
    }
  }

  @Test
  fun rememberRetainedAllowsRememberObserverWithRetainObserver() {
    CircuitRetainedSettings.enforceRetainObserverCompatibility = true

    runTestComposition { rememberRetained { CompatibleObserver() } }
  }

  private fun assertRememberObserverRejected(
    installRetainedRegistry: Boolean = true,
    content: @Composable () -> Any,
  ) {
    CircuitRetainedSettings.enforceRetainObserverCompatibility = true

    val exception =
      assertFailsWith<IllegalArgumentException> {
        runTestComposition(installRetainedRegistry, content)
      }

    assertThat(exception)
      .hasMessageThat()
      .isEqualTo(
        "Retained a value that implements RememberObserver but not RetainObserver. " +
          "To receive the correct callbacks, the retained value 'TestRememberObserver' must also " +
          "implement RetainObserver."
      )
  }

  private fun runTestComposition(
    installRetainedRegistry: Boolean = true,
    content: @Composable () -> Any,
  ) {
    var result: Any? = null
    runComposeUiTest {
      setContent {
        if (installRetainedRegistry) {
          CompositionLocalProvider(LocalRetainedStateRegistry provides RetainedStateRegistry()) {
            result = content()
          }
        } else {
          result = content()
        }
      }
      waitForIdle()
    }
    assertThat(result).isNotNull()
  }

  private class TestRememberObserver : RememberObserver {
    override fun onRemembered() = Unit

    override fun onForgotten() = Unit

    override fun onAbandoned() = Unit

    override fun toString(): String = "TestRememberObserver"
  }

  private class CompatibleObserver : RememberObserver, RetainObserver {
    override fun onRemembered() = Unit

    override fun onForgotten() = Unit

    override fun onAbandoned() = Unit

    override fun onRetained() = Unit

    override fun onEnteredComposition() = Unit

    override fun onExitedComposition() = Unit

    override fun onRetired() = Unit

    override fun onUnused() = Unit
  }
}

// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.LocalCircuitSaver
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.ProvideCircuitSaver
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.plus
import com.slack.circuit.runtime.screen.restorePopResult
import com.slack.circuit.runtime.screen.restoreScreen
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(ComposeUiTestRunner::class)
class RememberDefaultCircuitSaverTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun compositionLocalsUsesSaverConfiguredOnCircuit() {
    val configuredSaver = testCircuitSaver()
    val inheritedSaver = testCircuitSaver()
    val circuit = Circuit.Builder().setCircuitSaver(configuredSaver).build()
    lateinit var observedSaver: CircuitSaver

    composeTestRule.setContent {
      ProvideCircuitSaver(inheritedSaver) {
        CircuitCompositionLocals(circuit) { observedSaver = LocalCircuitSaver.current }
      }
    }

    composeTestRule.runOnIdle { assertSame(configuredSaver, observedSaver) }
  }

  @Test
  fun compositionLocalsUsesInheritedSaverWhenCircuitHasNone() {
    val inheritedSaver = testCircuitSaver()
    val circuit = Circuit.Builder().build()
    lateinit var observedSaver: CircuitSaver

    composeTestRule.setContent {
      ProvideCircuitSaver(inheritedSaver) {
        CircuitCompositionLocals(circuit) { observedSaver = LocalCircuitSaver.current }
      }
    }

    composeTestRule.runOnIdle { assertSame(inheritedSaver, observedSaver) }
  }

  @Test
  fun compositionLocalsUsesRegistryBackedSaverWhenCircuitHasNone() {
    val accepted = RegistryTestScreen("accepted")
    val registry = SaveableStateRegistry(emptyMap()) { it === accepted }
    val circuit = Circuit.Builder().build()
    lateinit var observedSaver: CircuitSaver

    composeTestRule.setContent {
      CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
        CircuitCompositionLocals(circuit) { observedSaver = LocalCircuitSaver.current }
      }
    }

    composeTestRule.runOnIdle { assertSame(accepted, observedSaver.save(accepted)) }
  }

  @Test
  fun compositionLocalsAppliesSaverTransformToInheritedSaver() {
    val transformedSaver = testCircuitSaver()
    val inheritedSaver = testCircuitSaver()
    lateinit var observedFallbackSaver: CircuitSaver
    val circuit =
      Circuit.Builder()
        .setCircuitSaver { fallbackSaver ->
          observedFallbackSaver = fallbackSaver
          transformedSaver
        }
        .build()
    lateinit var observedSaver: CircuitSaver

    composeTestRule.setContent {
      ProvideCircuitSaver(inheritedSaver) {
        CircuitCompositionLocals(circuit) { observedSaver = LocalCircuitSaver.current }
      }
    }

    composeTestRule.runOnIdle {
      assertSame(inheritedSaver, observedFallbackSaver)
      assertSame(transformedSaver, observedSaver)
    }
  }

  @Test
  fun saverTransformCanComposeWithRegistryBackedSaver() {
    val accepted = RegistryTestScreen("accepted")
    val registry = SaveableStateRegistry(emptyMap()) { it === accepted }
    val serializingSaver =
      object : CircuitSaver() {
        override fun canSave(value: CircuitSaveable): Boolean = false

        override fun save(value: CircuitSaveable): Any? = null

        override fun canRestore(saved: Any): Boolean = false

        override fun restore(saved: Any): CircuitSaveable? = null
      }
    val circuit =
      Circuit.Builder()
        .setCircuitSaver { fallbackSaver -> serializingSaver + fallbackSaver }
        .build()
    var recomposeTrigger by mutableStateOf(0)
    lateinit var observedSaver: CircuitSaver

    composeTestRule.setContent {
      val currentRegistry = if (recomposeTrigger >= 0) registry else error("unreachable")
      CompositionLocalProvider(LocalSaveableStateRegistry provides currentRegistry) {
        CircuitCompositionLocals(circuit) { observedSaver = LocalCircuitSaver.current }
      }
    }

    // The serializing saver claims nothing, so the registry-backed saver handles the value.
    lateinit var firstSaver: CircuitSaver
    composeTestRule.runOnIdle {
      firstSaver = observedSaver
      assertSame(accepted, observedSaver.save(accepted))
      recomposeTrigger++
    }
    composeTestRule.runOnIdle { assertSame(firstSaver, observedSaver) }
  }

  @Test
  fun explicitCompositionLocalsSaverOverridesCircuitSaverAndIsInherited() {
    val configuredSaver = testCircuitSaver()
    val explicitSaver = testCircuitSaver()
    val circuit = Circuit.Builder().setCircuitSaver(configuredSaver).build()
    val nestedCircuit = Circuit.Builder().build()
    lateinit var observedSaver: CircuitSaver

    composeTestRule.setContent {
      CircuitCompositionLocals(circuit, explicitSaver) {
        CircuitCompositionLocals(nestedCircuit) { observedSaver = LocalCircuitSaver.current }
      }
    }

    composeTestRule.runOnIdle { assertSame(explicitSaver, observedSaver) }
  }

  @Test
  fun savesValuesAcceptedByCurrentRegistryAndFailsOnRejectedValues() {
    val accepted = RegistryTestScreen("accepted")
    val rejected = RegistryTestScreen("rejected")
    val registry = SaveableStateRegistry(emptyMap()) { it === accepted }
    lateinit var saver: CircuitSaver

    composeTestRule.setContent {
      CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
        saver = rememberDefaultCircuitSaver()
      }
    }

    composeTestRule.runOnIdle {
      assertSame(accepted, saver.save(accepted))
      val failure = assertFailsWith<IllegalArgumentException> { saver.save(rejected) }
      assertContains(failure.message.orEmpty(), "cannot save")
    }
  }

  @Test
  fun rejectedValuesFallThroughInAComposite() {
    val screen = RegistryTestScreen("rejected")
    val registry = SaveableStateRegistry(emptyMap()) { false }
    val fallback =
      object : CircuitSaver() {
        override fun save(value: CircuitSaveable): Any = "fallback"

        override fun canSave(value: CircuitSaveable): Boolean = true

        override fun canRestore(saved: Any): Boolean = true

        override fun restore(saved: Any): CircuitSaveable? = null
      }
    lateinit var saver: CircuitSaver

    composeTestRule.setContent {
      CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
        saver = rememberDefaultCircuitSaver()
      }
    }

    composeTestRule.runOnIdle { assertEquals("fallback", (saver + fallback).save(screen)) }
  }

  @Test
  fun failsWithoutRegistryUnlessFollowedByDroppingSaver() {
    lateinit var saver: CircuitSaver

    composeTestRule.setContent {
      CompositionLocalProvider(LocalSaveableStateRegistry provides null) {
        saver = rememberDefaultCircuitSaver()
      }
    }

    composeTestRule.runOnIdle {
      val screen = RegistryTestScreen("screen")
      assertFailsWith<IllegalArgumentException> { saver.save(screen) }
      assertNull((saver + CircuitSaver.NoOp).save(screen))
    }
  }

  @Test
  fun remembersSaverUntilRegistryChanges() {
    val firstRegistry = SaveableStateRegistry(emptyMap()) { true }
    val secondRegistry = SaveableStateRegistry(emptyMap()) { true }
    var registryState by mutableStateOf(firstRegistry to 0)
    lateinit var saver: CircuitSaver

    composeTestRule.setContent {
      CompositionLocalProvider(LocalSaveableStateRegistry provides registryState.first) {
        saver = rememberDefaultCircuitSaver()
      }
    }

    lateinit var firstSaver: CircuitSaver
    composeTestRule.runOnIdle { firstSaver = saver }
    composeTestRule.runOnIdle { registryState = registryState.copy(second = 1) }
    composeTestRule.runOnIdle { assertSame(firstSaver, saver) }
    composeTestRule.runOnIdle { registryState = secondRegistry to 2 }
    composeTestRule.runOnIdle { assertNotSame(firstSaver, saver) }
  }

  @Test
  fun restoresRawCircuitSaveableValues() {
    val registry = SaveableStateRegistry(emptyMap()) { false }
    val screen = RegistryTestScreen("screen")
    val result = RegistryTestResult(42)
    lateinit var saver: CircuitSaver

    composeTestRule.setContent {
      CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
        saver = rememberDefaultCircuitSaver()
      }
    }

    composeTestRule.runOnIdle {
      assertSame(screen, saver.restoreScreen<RegistryTestScreen>(screen))
      assertSame(result, saver.restorePopResult<RegistryTestResult>(result))
      assertNull(saver.restoreScreen<Screen>(Any()))
      assertNull(saver.restorePopResult<PopResult>(Any()))
    }
  }
}

private fun testCircuitSaver(): CircuitSaver =
  object : CircuitSaver() {
    override fun canSave(value: CircuitSaveable): Boolean = true

    override fun save(value: CircuitSaveable): Any? = null

    override fun canRestore(saved: Any): Boolean = true

    protected override fun restore(saved: Any): CircuitSaveable? = null
  }

private data class RegistryTestScreen(val value: String) : Screen

private data class RegistryTestResult(val value: Int) : PopResult

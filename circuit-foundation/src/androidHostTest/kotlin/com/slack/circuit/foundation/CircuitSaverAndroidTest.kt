// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.DefaultCircuitSaver
import com.slack.circuit.runtime.screen.LocalCircuitSaver
import com.slack.circuit.runtime.screen.ParcelablePopResult
import com.slack.circuit.runtime.screen.ParcelableScreen
import com.slack.circuit.runtime.screen.ProvideCircuitSaver
import com.slack.circuit.runtime.screen.restorePopResult
import com.slack.circuit.runtime.screen.restoreScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(ComposeUiTestRunner::class)
class CircuitSaverAndroidTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun parcelableScreenAndPopResultRoundTripThroughBundle() {
    val screen = TestParcelableScreen("screen")
    val result = TestParcelablePopResult(42)

    val restored =
      roundTrip(
        Bundle().apply {
          putParcelable("screen", screen)
          putParcelable("result", result)
        }
      )

    assertEquals(screen, restored.parcelable<TestParcelableScreen>("screen"))
    assertEquals(result, restored.parcelable<TestParcelablePopResult>("result"))
  }

  @Test
  fun defaultCircuitSaverPassesThroughParcelableValues() {
    val screen = TestParcelableScreen("screen")
    val result = TestParcelablePopResult(42)

    val savedScreen = DefaultCircuitSaver.save(screen)
    val savedResult = DefaultCircuitSaver.save(result)

    assertSame(screen, savedScreen)
    assertSame(result, savedResult)
    assertSame(screen, DefaultCircuitSaver.restoreScreen<TestParcelableScreen>(savedScreen!!))
    assertSame(result, DefaultCircuitSaver.restorePopResult<TestParcelablePopResult>(savedResult!!))
  }

  @Test
  fun provideCircuitSaverProvidesExactInstance() {
    val saver = TestCircuitSaver()
    lateinit var observed: CircuitSaver

    composeTestRule.setContent {
      ProvideCircuitSaver(saver) { observed = LocalCircuitSaver.current }
    }

    composeTestRule.runOnIdle { assertSame(saver, observed) }
  }

  @Test
  fun circuitBuilderSaverCanBeInheritedOverriddenAndCleared() {
    val outerSaver = TestCircuitSaver()
    val configuredSaver = TestCircuitSaver()
    val replacementSaver = TestCircuitSaver()
    val defaultCircuit = Circuit.Builder().build()
    val circuit = Circuit.Builder().setCircuitSaver(configuredSaver).build()
    val inheritedCircuit = circuit.newBuilder().build()
    val overriddenCircuit = circuit.newBuilder().setCircuitSaver(replacementSaver).build()
    val clearedCircuit = circuit.newBuilder().setCircuitSaver(null).build()
    lateinit var default: CircuitSaver
    lateinit var configured: CircuitSaver
    lateinit var inherited: CircuitSaver
    lateinit var overridden: CircuitSaver
    lateinit var cleared: CircuitSaver

    composeTestRule.setContent {
      ProvideCircuitSaver(outerSaver) {
        CircuitCompositionLocals(defaultCircuit) { default = LocalCircuitSaver.current }
        CircuitCompositionLocals(circuit) { configured = LocalCircuitSaver.current }
        CircuitCompositionLocals(inheritedCircuit) { inherited = LocalCircuitSaver.current }
        CircuitCompositionLocals(overriddenCircuit) { overridden = LocalCircuitSaver.current }
        CircuitCompositionLocals(clearedCircuit) { cleared = LocalCircuitSaver.current }
      }
    }

    composeTestRule.runOnIdle {
      assertSame(outerSaver, default)
      assertSame(configuredSaver, configured)
      assertSame(configuredSaver, inherited)
      assertSame(replacementSaver, overridden)
      assertSame(outerSaver, cleared)
    }
  }
}

private fun roundTrip(bundle: Bundle): Bundle {
  val parcel = Parcel.obtain()
  return try {
    bundle.writeToParcel(parcel, 0)
    parcel.setDataPosition(0)
    Bundle.CREATOR.createFromParcel(parcel).apply {
      classLoader = CircuitSaverAndroidTest::class.java.classLoader
    }
  } finally {
    parcel.recycle()
  }
}

@Suppress("DEPRECATION")
private inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? =
  getParcelable(key) as? T

@Parcelize private data class TestParcelableScreen(val value: String) : ParcelableScreen

@Parcelize private data class TestParcelablePopResult(val value: Int) : ParcelablePopResult

private class TestCircuitSaver : CircuitSaver() {
  override fun save(value: CircuitSaveable): Any = value

  override fun restore(saved: Any): CircuitSaveable? = saved as? CircuitSaveable
}

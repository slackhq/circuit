// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.LocalCircuitSaver
import com.slack.circuit.runtime.screen.ParcelablePopResult
import com.slack.circuit.runtime.screen.ParcelableScreen
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.ProvideCircuitSaver
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.plus
import com.slack.circuit.runtime.screen.restorePopResult
import com.slack.circuit.runtime.screen.restoreScreen
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
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
  fun rememberedDefaultSaverPassesThroughParcelableMarkerValues() {
    val saver = rememberDefaultSaver()
    val screen = TestParcelableScreen("screen")
    val result = TestParcelablePopResult(42)

    val savedScreen = saver.save(screen)
    val savedResult = saver.save(result)

    assertSame(screen, savedScreen)
    assertSame(result, savedResult)
    assertSame(screen, saver.restoreScreen<TestParcelableScreen>(savedScreen!!))
    assertSame(result, saver.restorePopResult<TestParcelablePopResult>(savedResult!!))
  }

  @Test
  fun rememberedDefaultSaverPassesThroughDirectParcelableValues() {
    val saver = rememberDefaultSaver()
    val screen = TestDirectParcelableScreen("screen")
    val result = TestDirectParcelablePopResult(42)

    val savedScreen = saver.save(screen)
    val savedResult = saver.save(result)

    assertSame(screen, savedScreen)
    assertSame(result, savedResult)
    assertSame(screen, saver.restoreScreen<TestDirectParcelableScreen>(savedScreen!!))
    assertSame(
      result,
      saver.restorePopResult<TestDirectParcelablePopResult>(savedResult!!),
    )
  }

  @Test
  fun rememberedDefaultSaverFailsOnPlainValues() {
    val saver = rememberDefaultSaver()

    val failure = assertFailsWith<IllegalArgumentException> { saver.save(TestPlainScreen) }
    assertContains(failure.message.orEmpty(), "cannot save")
    assertFailsWith<IllegalArgumentException> { saver.save(TestPlainPopResult) }
  }

  @Test
  fun droppingSaverCanFollowRememberedDefaultSaver() {
    val dropped = mutableListOf<CircuitSaveable>()
    val saver = rememberDefaultSaver() + CircuitSaver.Dropping(dropped::add)

    assertNull(saver.save(TestPlainScreen))
    assertNull(saver.save(TestPlainPopResult))
    assertEquals(listOf<CircuitSaveable>(TestPlainScreen, TestPlainPopResult), dropped)
  }

  @Test
  fun rememberedDroppingSaverRestoresPlainScreenThroughInitialValue() {
    val restorationTester = StateRestorationTester(composeTestRule)
    var initializations = 0
    restorationTester.setContent {
      val defaultSaver = rememberDefaultCircuitSaver()
      val saver = remember(defaultSaver) { defaultSaver + CircuitSaver.NoOp }
      rememberSaveableBackStack(TestPlainScreen, saver) { initializations++ }
    }

    restorationTester.emulateSavedInstanceStateRestore()

    assertEquals(2, initializations)
  }

  @Test
  fun saverTransformRestoresWithNewSaverInstancesAndRegistry() {
    val restorationTester = StateRestorationTester(composeTestRule)
    val fallbackSavers = mutableListOf<CircuitSaver>()
    val envelopeSavers = mutableListOf<TestEnvelopeCircuitSaver>()
    val rawScreen = TestDirectParcelableScreen("raw")
    val circuit =
      Circuit.Builder()
        .setCircuitSaver { fallbackSaver ->
          fallbackSavers += fallbackSaver
          val envelopeSaver = TestEnvelopeCircuitSaver()
          envelopeSavers += envelopeSaver
          envelopeSaver + fallbackSaver
        }
        .build()
    lateinit var backStack: SaveableBackStack

    restorationTester.setContent {
      CircuitCompositionLocals(circuit) {
        backStack = rememberSaveableBackStack(listOf(rawScreen, TestPlainScreen))
      }
    }

    lateinit var originalFallbackSaver: CircuitSaver
    lateinit var originalEnvelopeSaver: TestEnvelopeCircuitSaver
    composeTestRule.runOnIdle {
      assertEquals(listOf(TestPlainScreen, rawScreen), backStack.map { it.screen })
      originalFallbackSaver = fallbackSavers.single()
      originalEnvelopeSaver = envelopeSavers.single()
    }

    restorationTester.emulateSavedInstanceStateRestore()

    composeTestRule.runOnIdle {
      assertEquals(listOf(TestPlainScreen, rawScreen), backStack.map { it.screen })
      assertEquals(2, fallbackSavers.size)
      assertEquals(2, envelopeSavers.size)
      assertNotSame(originalFallbackSaver, fallbackSavers.last())
      assertNotSame(originalEnvelopeSaver, envelopeSavers.last())
    }
  }

  @Test
  fun rememberedDefaultSaverRestoresRawValuesAndIgnoresUnrelatedInput() {
    val saver = rememberDefaultSaver()

    assertSame(
      TestPlainScreen,
      saver.restoreScreen<Screen>(TestPlainScreen),
    )
    assertSame(
      TestPlainPopResult,
      saver.restorePopResult<PopResult>(TestPlainPopResult),
    )
    assertNull(saver.restoreScreen<Screen>(Any()))
    assertNull(saver.restorePopResult<PopResult>(Any()))
  }

  private fun rememberDefaultSaver(): CircuitSaver {
    lateinit var saver: CircuitSaver
    composeTestRule.setContent { saver = rememberDefaultCircuitSaver() }
    return composeTestRule.runOnIdle { saver }
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
  fun localCircuitSaverFailsWhenUnprovided() {
    val error =
      assertFailsWith<IllegalStateException> {
        composeTestRule.setContent { LocalCircuitSaver.current }
      }

    assertContains(error.message.orEmpty(), "No CircuitSaver provided")
  }

  @Test
  fun circuitCompositionLocalsProvidesExactSaver() {
    val circuit = Circuit.Builder().setCircuitSaver(TestCircuitSaver()).build()
    val saver = TestCircuitSaver()
    lateinit var observed: CircuitSaver

    composeTestRule.setContent {
      CircuitCompositionLocals(circuit, saver) {
        observed = LocalCircuitSaver.current
      }
    }

    composeTestRule.runOnIdle { assertSame(saver, observed) }
  }

  @Test
  fun circuitBuilderSaverCanBeInheritedOverriddenAndCleared() {
    val configuredSaver = TestCircuitSaver()
    val replacementSaver = TestCircuitSaver()
    val circuit = Circuit.Builder().setCircuitSaver(configuredSaver).build()

    assertNull(Circuit.Builder().build().circuitSaver)
    assertSame(configuredSaver, circuit.circuitSaver)
    assertSame(configuredSaver, circuit.newBuilder().build().circuitSaver)
    assertSame(
      replacementSaver,
      circuit.newBuilder().setCircuitSaver(replacementSaver).build().circuitSaver,
    )
    assertNull(circuit.newBuilder().setCircuitSaver(null).build().circuitSaver)
  }

  @Test
  fun circuitBuilderSaverAndTransformClearEachOther() {
    val staticSaver = TestCircuitSaver()
    val transformedSaver = TestCircuitSaver()
    val transform: (CircuitSaver) -> CircuitSaver = { transformedSaver }

    val transformedCircuit =
      Circuit.Builder().setCircuitSaver(staticSaver).setCircuitSaver(transform).build()
    assertNull(transformedCircuit.circuitSaver)
    assertSame(transform, transformedCircuit.circuitSaverTransform)

    val inherited = transformedCircuit.newBuilder().build()
    assertSame(transform, inherited.circuitSaverTransform)

    val staticCircuit = transformedCircuit.newBuilder().setCircuitSaver(staticSaver).build()
    assertSame(staticSaver, staticCircuit.circuitSaver)
    assertNull(staticCircuit.circuitSaverTransform)
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

@Parcelize private data class TestDirectParcelableScreen(val value: String) : Screen, Parcelable

@Parcelize private data class TestDirectParcelablePopResult(val value: Int) : PopResult, Parcelable

private data object TestPlainScreen : Screen

private data object TestPlainPopResult : PopResult

private class TestEnvelopeCircuitSaver : CircuitSaver() {
  protected override fun canSave(value: CircuitSaveable): Boolean = value === TestPlainScreen

  override fun save(value: CircuitSaveable): Any =
    Bundle().apply { putBoolean("test-envelope", true) }

  protected override fun canRestore(saved: Any): Boolean =
    saved is Bundle && saved.containsKey("test-envelope")

  protected override fun restore(saved: Any): CircuitSaveable = TestPlainScreen
}

private class TestCircuitSaver : CircuitSaver() {
  override fun canSave(value: CircuitSaveable): Boolean = true

  override fun save(value: CircuitSaveable): Any = value

  override fun canRestore(saved: Any): Boolean = true

  override fun restore(saved: Any): CircuitSaveable? = saved as? CircuitSaveable
}

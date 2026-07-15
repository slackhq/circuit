// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.DefaultCircuitSaver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(ComposeUiTestRunner::class)
class AnsweringResultHandlerSaverTest {

  @get:Rule val composeTestRule = createComposeRule()
  val stateRestorationTester = StateRestorationTester(composeTestRule)

  lateinit var handler: AnsweringResultHandler

  @Test
  fun saverRestoresEmptyHandler() {
    setContent()
    stateRestorationTester.emulateSavedInstanceStateRestore()
    assertFalse(handler.expectingResult("record1"))
  }

  @Test
  fun saverRestoresExpectingResult() {
    setContent()
    handler.prepareForResult("record1", "resultKey1")
    assertTrue(handler.expectingResult("record1"))
    stateRestorationTester.emulateSavedInstanceStateRestore()
    assertTrue(handler.expectingResult("record1"))
  }

  @Test
  fun saverRestoresAwaitingResult() = runTest {
    setContent()
    handler.prepareForResult("record1", "resultKey1")
    assertTrue(handler.expectingResult("record1"))
    handler.sendResult("record1", TestPopResult)

    stateRestorationTester.emulateSavedInstanceStateRestore()
    assertTrue(handler.expectingResult("record1"))

    val result = handler.awaitResult("record1", "resultKey1")
    assertEquals(TestPopResult, result)
  }

  @Test
  fun saverRecordsPendingResultPresenceAndPeeksNonDestructively() = runTest {
    val handler = AnsweringResultHandler()
    handler.prepareForResult("record1", "resultKey1")

    val savedWithoutPendingResult = save(handler)
    assertEquals(listOf("resultKey1", null, false), savedWithoutPendingResult[1])

    var saveCalls = 0
    val countingSaver =
      object : CircuitSaver() {
        override fun save(value: CircuitSaveable): Any {
          saveCalls++
          return value
        }

        override fun restore(saved: Any): CircuitSaveable? = saved as? CircuitSaveable
      }
    handler.sendResult("record1", TestPopResult)

    val savedWithPendingResult = save(handler, countingSaver)
    assertEquals(listOf("resultKey1", TestPopResult, true), savedWithPendingResult[1])
    assertEquals(1, saveCalls)
    assertEquals(TestPopResult, handler.awaitResult("record1", "resultKey1"))
  }

  @Test
  fun saverDropsExpectationWhenPendingResultIsSkippedDuringSave() = runTest {
    val skippingSaver =
      object : CircuitSaver() {
        override fun save(value: CircuitSaveable): Any? = null

        override fun restore(saved: Any): CircuitSaveable? = error("Nothing should be restored")
      }
    setContent(skippingSaver)
    handler.prepareForResult("record1", "resultKey1")
    handler.sendResult("record1", TestPopResult)

    stateRestorationTester.emulateSavedInstanceStateRestore()

    assertFalse(handler.expectingResult("record1"))
    assertNull(withTimeout(1_000) { handler.awaitResult("record1", "resultKey1") })
  }

  @Test
  fun saverDropsExpectationWhenPendingResultCannotBeRestored() = runTest {
    val droppingSaver =
      object : CircuitSaver() {
        override fun save(value: CircuitSaveable): Any = value

        override fun restore(saved: Any): CircuitSaveable? = null
      }
    setContent(droppingSaver)
    handler.prepareForResult("record1", "resultKey1")
    handler.sendResult("record1", TestPopResult)

    stateRestorationTester.emulateSavedInstanceStateRestore()

    assertFalse(handler.expectingResult("record1"))
    assertNull(withTimeout(1_000) { handler.awaitResult("record1", "resultKey1") })
  }

  @Test
  fun saverRestoresLegacyExpectationWithoutPendingResult() {
    val restored =
      AnsweringResultHandler.Saver(DefaultCircuitSaver)
        .restore(listOf("record1", listOf("resultKey1", null)))

    assertTrue(restored!!.expectingResult("record1"))
  }

  @Test
  fun saverRestoresLegacyPendingResult() = runTest {
    val restored =
      AnsweringResultHandler.Saver(DefaultCircuitSaver)
        .restore(listOf("record1", listOf("resultKey1", TestPopResult)))

    assertEquals(TestPopResult, restored!!.awaitResult("record1", "resultKey1"))
  }

  @Test
  fun saverIgnoresEntryWithoutResultKey() {
    val restored =
      AnsweringResultHandler.Saver(DefaultCircuitSaver)
        .restore(listOf("record1", listOf(null, TestPopResult, true)))

    assertFalse(restored!!.expectingResult("record1"))
  }

  @Test
  fun saverFailsWhenPendingResultRestoresAsScreen() {
    val mismatchingSaver =
      object : CircuitSaver() {
        override fun save(value: CircuitSaveable): Any = value

        override fun restore(saved: Any): CircuitSaveable = TestScreen
      }

    assertFailsWith<IllegalStateException> {
      AnsweringResultHandler.Saver(mismatchingSaver)
        .restore(listOf("record1", listOf("resultKey1", TestPopResult, true)))
    }
  }

  private fun setContent(circuitSaver: CircuitSaver = DefaultCircuitSaver) {
    stateRestorationTester.setContent {
      handler = rememberAnsweringResultHandler(circuitSaver = circuitSaver)
    }
  }
}

@Suppress("UNCHECKED_CAST")
private fun save(
  handler: AnsweringResultHandler,
  circuitSaver: CircuitSaver = DefaultCircuitSaver,
): List<Any?> =
  with(AnsweringResultHandler.Saver(circuitSaver)) {
    SaverScope { true }.save(handler) as List<Any?>
  }

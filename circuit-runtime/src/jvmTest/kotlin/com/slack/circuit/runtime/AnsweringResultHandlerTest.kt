// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime

import com.slack.circuit.runtime.screen.PopResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCircuitApi::class)
class AnsweringResultHandlerTest {

  @Test
  fun `matching result is delivered and clears expectation`() = runTest {
    val handler = AnsweringResultHandler()
    handler.prepareForResult("record", "result")
    handler.sendResult("record", TestPopResult("value"))

    assertEquals(TestPopResult("value"), handler.awaitResult("record", "result"))
    assertFalse(handler.expectingResult("record"))
  }

  @Test
  fun `wrong result key does not consume pending result`() = runTest {
    val handler = AnsweringResultHandler()
    handler.prepareForResult("record", "result")
    handler.sendResult("record", TestPopResult("value"))

    assertNull(handler.awaitResult("record", "other"))
    assertTrue(handler.expectingResult("record"))
    assertEquals(TestPopResult("value"), handler.awaitResult("record", "result"))
  }

  @Test
  fun `newest pending result is delivered`() = runTest {
    val handler = AnsweringResultHandler()
    handler.prepareForResult("record", "result")
    handler.sendResult("record", TestPopResult("first"))
    handler.sendResult("record", TestPopResult("second"))

    assertEquals(TestPopResult("second"), handler.awaitResult("record", "result"))
  }

  private data class TestPopResult(val value: String) : PopResult
}

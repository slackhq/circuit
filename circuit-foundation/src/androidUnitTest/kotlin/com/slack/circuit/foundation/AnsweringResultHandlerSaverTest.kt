// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(ComposeUiTestRunner::class)
class AnsweringResultHandlerSaverTest {

  @get:Rule val composeTestRule = createComposeRule()
  val stateRestorationTester = StateRestorationTester(composeTestRule)

  lateinit var handler: AnsweringResultHandler

  @Before
  fun setUp() {
    stateRestorationTester.setContent { handler = rememberAnsweringResultHandler() }
  }

  @Test
  fun saverRestoresEmptyHandler() {
    stateRestorationTester.emulateSavedInstanceStateRestore()
    assertFalse(handler.expectingResult("record1"))
  }

  @Test
  fun saverRestoresExpectingResult() {
    handler.prepareForResult("record1", "resultKey1")
    assertTrue(handler.expectingResult("record1"))
    stateRestorationTester.emulateSavedInstanceStateRestore()
    assertTrue(handler.expectingResult("record1"))
  }

  @Test
  fun saverRestoresAwaitingResult() = runTest {
    handler.prepareForResult("record1", "resultKey1")
    assertTrue(handler.expectingResult("record1"))
    handler.sendResult("record1", TestPopResult)

    stateRestorationTester.emulateSavedInstanceStateRestore()
    assertTrue(handler.expectingResult("record1"))

    val result = handler.awaitResult("record1", "resultKey1")
    assertEquals(TestPopResult, result)
  }
}

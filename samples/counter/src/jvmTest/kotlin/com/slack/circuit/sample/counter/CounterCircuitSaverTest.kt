// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter

import com.slack.circuit.runtime.screen.restoreScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CounterCircuitSaverTest {
  private val saver = buildCircuitSaver()

  @Test
  fun counterScreenRoundTrip() {
    val saved = assertNotNull(saver.save(CounterScreen))

    assertEquals(CounterScreen, saver.restoreScreen<CounterScreen>(saved))
  }

  @Test
  fun primeScreenRoundTrip() {
    val screen = PrimeScreen(41)
    val saved = assertNotNull(saver.save(screen))

    assertEquals(screen, saver.restoreScreen<PrimeScreen>(saved))
  }
}

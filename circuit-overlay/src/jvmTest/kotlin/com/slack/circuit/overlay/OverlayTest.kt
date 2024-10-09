// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.overlay

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.slack.circuit.overlay.OverlayState.UNAVAILABLE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.test.runTest

class OverlayTest {
  @Test
  fun noOverlayMeansNullData() = runTest {
    moleculeFlow(RecompositionMode.Immediate) {
        val overlayHost = rememberOverlayHost()
        overlayHost.currentOverlayData
      }
      .distinctUntilChanged()
      .test { assertNull(awaitItem()) }
  }

  @Test
  fun overlayHasData() = runTest {
    moleculeFlow(RecompositionMode.Immediate) {
        val overlayHost = rememberOverlayHost()
        LaunchedEffect(overlayHost) { overlayHost.show(TestOverlay()) }
        overlayHost.currentOverlayData
      }
      .distinctUntilChanged()
      .test {
        assertNull(awaitItem())
        assertNotNull(awaitItem())
      }
  }

  @Test
  fun overlayFinishedHasNullDataAgain() = runTest {
    val testOverlay = TestOverlay()
    moleculeFlow(RecompositionMode.Immediate) {
        val overlayHost = rememberOverlayHost()
        val overlayHostData by rememberUpdatedState(overlayHost.currentOverlayData)
        key(overlayHost) { overlayHostData?.let { data -> data.overlay.Content(data::finish) } }
        LaunchedEffect(overlayHost) { overlayHost.show(testOverlay) }
        overlayHostData
      }
      .distinctUntilChanged()
      .test {
        assertNull(awaitItem())
        assertNotNull(awaitItem())
        testOverlay.finish("Done!")
        assertNull(awaitItem())
      }
  }

  @Test
  fun overlayStateIsUnavailableByDefault() = runTest {
    moleculeFlow(RecompositionMode.Immediate) { LocalOverlayState.current }
      .test { assertEquals(UNAVAILABLE, awaitItem()) }
  }
}

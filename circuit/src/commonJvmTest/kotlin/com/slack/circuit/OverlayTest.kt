/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.slack.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.test.runTest

class OverlayTest {
  @Test
  fun noOverlayMeansNullData() = runTest {
    moleculeFlow(RecompositionClock.Immediate) {
        val overlayHost = rememberOverlayHost()
        overlayHost.currentOverlayData
      }
      .distinctUntilChanged()
      .test { assertNull(awaitItem()) }
  }

  @Test
  fun overlayHasData() = runTest {
    moleculeFlow(RecompositionClock.Immediate) {
        val overlayHost = rememberOverlayHost()
        LaunchedEffect(overlayHost) {
          overlayHost.show(
            object : Overlay<String> {
              @Composable override fun Content(navigator: OverlayNavigator<String>) {}
            }
          )
        }
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
    val resultState = mutableStateOf<String?>(null)
    moleculeFlow(RecompositionClock.Immediate) {
        val overlayHost = rememberOverlayHost()
        val overlayHostData by rememberUpdatedState(overlayHost.currentOverlayData)
        key(overlayHostData) { overlayHostData?.let { data -> data.overlay.Content(data::finish) } }
        LaunchedEffect(overlayHost) {
          overlayHost.show(
            object : Overlay<String> {
              @Composable
              override fun Content(navigator: OverlayNavigator<String>) {
                val resultStateValue = resultState.value
                if (resultStateValue != null) {
                  navigator.finish(resultStateValue)
                }
              }
            }
          )
        }
        overlayHostData
      }
      .distinctUntilChanged()
      .test {
        assertNull(awaitItem())
        assertNotNull(awaitItem())
        resultState.value = "Done!"
        assertNull(awaitItem())
      }
  }
}

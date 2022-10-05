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
package com.slack.circuit.star.home

import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeNavPresenterTest {
  @Test
  fun changeIndices() = runTest {
    moleculeFlow(RecompositionClock.Immediate) { HomeNavPresenter() }
      .test {
        // Initial index is 0.
        val firstState = awaitItem()
        assertThat(firstState.index).isEqualTo(0)

        // Clicking the same index does nothing.
        firstState.eventSink(HomeNavScreen.Event.ClickNavItem(0))
        expectNoEvents()

        // Changing the index emits a new state.
        firstState.eventSink(HomeNavScreen.Event.ClickNavItem(1))
        assertThat(awaitItem().index).isEqualTo(1)
      }
  }
}

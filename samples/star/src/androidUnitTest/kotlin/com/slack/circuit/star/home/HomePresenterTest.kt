// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.home

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.star.home.HomeScreen.Event.ClickNavItem
import com.slack.circuit.test.FakeNavigator
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomePresenterTest {
  @Test
  fun changeIndices() = runTest {
    moleculeFlow(RecompositionMode.Immediate) { HomePresenter(FakeNavigator(HomeScreen)) }
      .test {
        // Initial index is 0.
        val firstState = awaitItem()
        assertThat(firstState.selectedIndex).isEqualTo(0)

        // Clicking the same index does nothing.
        firstState.eventSink(ClickNavItem(0))
        expectNoEvents()

        // Changing the index emits a new state.
        firstState.eventSink(ClickNavItem(1))
        assertThat(awaitItem().selectedIndex).isEqualTo(1)
      }
  }
}

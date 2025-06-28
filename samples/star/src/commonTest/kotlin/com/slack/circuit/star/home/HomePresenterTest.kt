// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.home

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.presenterTestOf
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class HomePresenterTest {
  @Test
  fun changeIndices() = runTest {
    presenterTestOf({ HomePresenter(FakeNavigator(HomeScreen)) }) {
      // Initial index is 0.
      val firstState = awaitItem()
      assertThat(firstState.selectedIndex).isEqualTo(0)

      // Clicking the same index does nothing.
      firstState.eventSink(HomeScreen.Event.ClickNavItem(0))
      expectNoEvents()

      // Changing the index emits a new state.
      firstState.eventSink(HomeScreen.Event.ClickNavItem(1))
      assertThat(awaitItem().selectedIndex).isEqualTo(1)
    }
  }
}

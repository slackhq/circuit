// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.slack.circuit.foundation.LocalRecordLifecycle
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.StaticScreen
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What a record body reads about its own placement. [RecordHost] provides both. */
private data class RecordSignals(val isVisible: Boolean, val isActive: Boolean)

@RunWith(RobolectricTestRunner::class)
class RecordHostTest {

  @get:Rule val composeTestRule = createComposeRule()

  /**
   * `isActive` alone can't gate on-screen work: a warming record deliberately runs active so its
   * interop views attach, and work started there (marking read, impressions) would run for a screen
   * the user never opened.
   */
  @Test
  fun `a warming record reads not visible while staying active`() {
    var latest: RecordSignals? = null
    val host =
      RecordHost<TestRecord>(TestRecord) {
        val lifecycle = LocalRecordLifecycle.current
        latest = RecordSignals(lifecycle.isVisible, lifecycle.isActive)
      }
    composeTestRule.setContent { host.slotContent() }

    assertEquals(RecordSignals(isVisible = false, isActive = true), signals(latest))

    host.placement = Placement.Settled
    composeTestRule.waitForIdle()
    assertEquals(RecordSignals(isVisible = true, isActive = true), signals(latest))

    host.placement = Placement.Parked
    composeTestRule.waitForIdle()
    assertEquals(RecordSignals(isVisible = false, isActive = false), signals(latest))
  }

  private fun signals(latest: RecordSignals?) =
    requireNotNull(latest) { "record body never composed" }
}

private data object TestRecordScreen : StaticScreen

private data object TestRecord : NavArgument {
  override val key: String = "test-record"
  override val screen: Screen = TestRecordScreen
}

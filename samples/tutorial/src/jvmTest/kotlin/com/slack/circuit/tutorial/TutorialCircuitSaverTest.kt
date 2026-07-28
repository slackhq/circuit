// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tutorial

import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.restoreScreen
import com.slack.circuit.tutorial.intro.DetailScreen as IntroDetailScreen
import com.slack.circuit.tutorial.intro.InboxScreen as IntroInboxScreen
import com.slack.circuit.tutorial.sharedelements.DetailScreen as SharedElementsDetailScreen
import com.slack.circuit.tutorial.sharedelements.InboxScreen as SharedElementsInboxScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TutorialCircuitSaverTest {

  @Test
  fun screensRoundTrip() {
    assertRoundTrips(IntroInboxScreen)
    assertRoundTrips(IntroDetailScreen("intro-email"))
    assertRoundTrips(SharedElementsInboxScreen)
    assertRoundTrips(SharedElementsDetailScreen("shared-elements-email"))
  }

  private inline fun <reified T : Screen> assertRoundTrips(screen: T) {
    val saver = buildCircuitSaver()
    val saved = assertNotNull(saver.save(screen))

    assertEquals(screen, saver.restoreScreen<T>(saved))
  }
}

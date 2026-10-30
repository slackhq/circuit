// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.LocalRecordLifecycle
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.navstack.rememberSaveableNavStack
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.StaticScreen
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private data class KeepMountedScreen(val id: Int) : StaticScreen

private data object KeepMountedState : CircuitUiState

/** One `enter`/`leave` of a record body, i.e. one screen build and teardown. */
private data class BodyEvent(val enter: Boolean, val id: Int)

/**
 * Guards against a wasted screen rebuild in [SlideOverNavDecoration], and pins that a root reset
 * crossfades through the same shared record layout the slides use.
 *
 * A settle prunes mounted records. Pruning one the next pass re-mounts takes its `movableContentOf`
 * out of the subcompose set and back, disposing and rebuilding the whole subtree for a screen the
 * user is leaving. That subtree includes the `DisposableEffect` that adds a hosted fragment.
 *
 * These tests count body enter/leave events, the cost of that rebuild.
 */
@RunWith(RobolectricTestRunner::class)
class SlideOverAdjacentRetentionTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `walking back two records rebuilds neither`() {
    val recorder = BodyRecorder()
    // Retention guards a movableContentOf relocation, not a warm hit, so it must hold with preload
    // off too.
    val navigator = start(recorder, preloadEnabled = false)

    settleNav(navigator) { it.goTo(KeepMountedScreen(1)) }
    settleNav(navigator) { it.goTo(KeepMountedScreen(2)) }
    settleNav(navigator) { it.backward() }
    settleNav(navigator) { it.backward() }

    // Record 0 is two steps behind at the settle on record 2. Dropping its slot there relocates its
    // movableContentOf on the way back, rebuilding the screen.
    assertEquals(
      listOf(0, 1, 2),
      recorder.events.filter { it.enter }.map { it.id },
      "a record rebuilt walking back, got ${recorder.events}",
    )
  }

  @Test
  fun `pop still disposes the record that left the stack`() {
    val recorder = BodyRecorder()
    val navigator = start(recorder, preloadEnabled = true)
    settleNav(navigator) { it.goTo(KeepMountedScreen(1)) }
    settleNav(navigator) { it.pop() }
    val events = recorder.events

    // A pop removes record 1 for good, so it must dispose rather than park.
    assertTrue(
      events.contains(BodyEvent(enter = false, id = 1)),
      "expected popped record 1 to be disposed, got $events",
    )
    assertEquals(1, events.count { it.enter && it.id == 0 }, "record 0 rebuilt, got $events")
  }

  @Test
  fun `a retained record parks paused rather than sharing the settled placement`() {
    val recorder = BodyRecorder()
    val navigator = start(recorder, preloadEnabled = true)

    repeat(STACK_WALK) { i -> settleNav(navigator) { it.goTo(KeepMountedScreen(i + 1)) } }
    repeat(STACK_WALK) { settleNav(navigator) { it.backward() } }

    // Retained records park rather than accumulate: the on-screen record plus KeepMountedDepth
    // neighbours, however far the stack is walked. Back at the root only the forward side has any.
    val live = recorder.liveCount()
    assertEquals(3, live, "expected 3 live record bodies, got $live from ${recorder.events}")
    // Only the on-screen record runs active; the retained neighbour is paused.
    assertEquals(
      setOf(0),
      recorder.activeIds,
      "expected only the settled record active, got ${recorder.activeIds}",
    )
  }

  @Test
  fun `root reset crossfades in place rather than sliding`() {
    val recorder = BodyRecorder()
    val navigator = start(recorder, preloadEnabled = false)
    composeTestRule.mainClock.autoAdvance = false

    composeTestRule.runOnUiThread { navigator.resetRoot(KeepMountedScreen(1)) }
    composeTestRule.mainClock.advanceTimeBy(MID_TRANSITION_MS)

    // A root reset has no adjacent to slide from, so the destination fades in where it already
    // sits. A slide would still have it most of the way off the trailing edge this far in.
    val left = composeTestRule.onNodeWithTag(recordTag(1)).getBoundsInRoot().left
    assertTrue(left.value < 1f, "expected the destination in place, was at $left")

    composeTestRule.mainClock.autoAdvance = true
    composeTestRule.mainClock.advanceTimeBy(SETTLE_MS)
    composeTestRule.waitForIdle()

    // Settling drops the replaced root and keeps the destination it already built.
    assertEquals(
      listOf(
        BodyEvent(enter = true, id = 0),
        BodyEvent(enter = true, id = 1),
        BodyEvent(enter = false, id = 0),
      ),
      recorder.events,
    )
  }

  /** Hosts record 0 under the decoration and returns its navigator, idle and settled. */
  private fun start(recorder: BodyRecorder, preloadEnabled: Boolean): Navigator {
    var navigatorRef: Navigator? = null
    composeTestRule.setContent {
      val circuit = remember {
        Circuit.Builder()
          .addStaticUi<KeepMountedScreen, KeepMountedState> { screen, modifier ->
            RecordingBody(screen.id, recorder, modifier)
          }
          .build()
      }
      CircuitCompositionLocals(circuit, CircuitSaver.NoOp) {
        val navStack = rememberSaveableNavStack(KeepMountedScreen(0))
        val navigator = rememberCircuitNavigator(navStack, enableBackHandler = false)
        navigatorRef = navigator
        NavigableCircuitContent(
          navigator = navigator,
          navStack = navStack,
          modifier = Modifier.fillMaxSize(),
          decoration = remember { SlideOverNavDecoration(preloadEnabled = preloadEnabled) },
        )
      }
    }
    composeTestRule.waitForIdle()
    return requireNotNull(navigatorRef) { "navigator was never captured from setContent" }
  }

  /** Runs [block] then lets the ~300ms transition finish, so events land on a settled tree. */
  private fun settleNav(navigator: Navigator, block: (Navigator) -> Unit) {
    composeTestRule.runOnUiThread { block(navigator) }
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(SETTLE_MS)
    composeTestRule.waitForIdle()
  }
}

/** Collects what each record body did. [Stable] so it can be passed to a composable. */
@Stable
private class BodyRecorder {
  val events = mutableListOf<BodyEvent>()
  val activeIds = mutableSetOf<Int>()

  /** Live bodies: entered but not yet disposed. */
  fun liveCount(): Int = events.count { it.enter } - events.count { !it.enter }
}

@Composable
private fun RecordingBody(id: Int, recorder: BodyRecorder, modifier: Modifier = Modifier) {
  // Stands in for a hosted fragment's DisposableEffect. Re-running it is the wasted build.
  DisposableEffect(Unit) {
    recorder.events += BodyEvent(enter = true, id = id)
    onDispose { recorder.events += BodyEvent(enter = false, id = id) }
  }
  val isActive = LocalRecordLifecycle.current.isActive
  DisposableEffect(isActive) {
    if (isActive) recorder.activeIds += id else recorder.activeIds -= id
    onDispose {}
  }
  Box(modifier.fillMaxSize().testTag(recordTag(id)))
}

private fun recordTag(id: Int) = "record-$id"

// Comfortably past the 300ms transition so each navigation lands settled.
private const val SETTLE_MS = 1_000L
// Halfway through the 300ms transition, where a slide and a crossfade look nothing alike.
private const val MID_TRANSITION_MS = 150L
private const val STACK_WALK = 12

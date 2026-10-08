// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalNavStageApi::class, ExperimentalSharedTransitionApi::class)

package com.slack.circuitx.navstage

import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.navstack.rememberSaveableNavStack
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins how often each record's body is built and torn down as panes animate within a stage and move
 * between stages, and that saveable and retained state follow a record across a move.
 */
@Config(minSdk = 34, maxSdk = 36)
@RunWith(RobolectricTestRunner::class)
class NavStageRecordRetentionTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val events = mutableListOf<BodyEvent>()
  private val instances = mutableMapOf<String, MutableList<Instance>>()
  private var nextId = 0
  private var multiPane by mutableStateOf(true)

  private val circuit =
    Circuit.Builder()
      .addPresenterFactory { screen, _, _ ->
        presenterOf {
          val retained = rememberRetained { nextId++ }
          RecordState((screen as RecordScreen).label, retained)
        }
      }
      .addUiFactory { _, _ ->
        ui<RecordState> { state, modifier ->
          val saved = rememberSaveable { nextId++ }
          DisposableEffect(state.label) {
            events += BodyEvent(enter = true, state.label)
            instances.getOrPut(state.label) { mutableListOf() } += Instance(saved, state.retained)
            onDispose { events += BodyEvent(enter = false, state.label) }
          }
          BasicText(state.label, modifier.fillMaxSize().testTag(state.label))
        }
      }
      .build()

  @Test
  fun swappingDetailDoesNotRebuildList() {
    val navigator = start(ListRecord, DetailRecord1)

    settle { navigator.goTo(DetailRecord2) }

    assertEquals(
      listOf(enter(ListRecord), enter(DetailRecord1), enter(DetailRecord2), leave(DetailRecord1)),
      events,
    )
  }

  @Test
  fun poppingDetailWithinStageDoesNotRebuildList() {
    val navigator = start(ListRecord, DetailRecord1, DetailRecord2)

    settle { navigator.pop() }

    assertEquals(
      listOf(enter(ListRecord), enter(DetailRecord2), enter(DetailRecord1), leave(DetailRecord2)),
      events,
    )
  }

  @Test
  fun unfoldingMovesDetailAcrossStagesOnceKeepingItsState() {
    multiPane = false
    start(ListRecord, DetailRecord1)

    settle { multiPane = true }

    assertEquals(listOf(true, false, true), lifecycleOf(DetailRecord1))
    assertEquals(listOf(true), lifecycleOf(ListRecord))
    assertStateFollowed(DetailRecord1)
  }

  @Test
  fun foldingMovesDetailAcrossStagesOnceKeepingItsState() {
    start(ListRecord, DetailRecord1)

    settle { multiPane = false }

    assertEquals(listOf(true, false, true), lifecycleOf(DetailRecord1))
    assertEquals(listOf(true, false), lifecycleOf(ListRecord))
    assertStateFollowed(DetailRecord1)
  }

  @Test
  fun poppingToListMovesListAcrossStagesOnceKeepingItsState() {
    val navigator = start(ListRecord, DetailRecord1)

    settle { navigator.pop() }

    assertEquals(listOf(true, false, true), lifecycleOf(ListRecord))
    assertEquals(listOf(true, false), lifecycleOf(DetailRecord1))
    assertStateFollowed(ListRecord)
  }

  @Test
  fun settledStagesComposeOnlyTheirVisibleRecords() {
    val navigator = start(ListRecord, DetailRecord1)

    settle { navigator.goTo(DetailRecord2) }
    settle { multiPane = false }
    settle { multiPane = true }
    settle { navigator.pop() }
    settle { navigator.pop() }

    assertEquals(setOf(ListRecord.label), composedLabels())
  }

  private fun start(vararg screens: RecordScreen): Navigator {
    lateinit var navigator: Navigator
    composeTestRule.setContent {
      CircuitCompositionLocals(circuit, CircuitSaver.NoOp) {
        SharedElementTransitionLayout {
          val navStack = rememberSaveableNavStack(screens.toList())
          navigator = rememberCircuitNavigator(navStack = navStack, onRootPop = {})
          NavigableCircuitContent(
            navigator = navigator,
            navStack = navStack,
            decoration = decoration,
          )
        }
      }
    }
    composeTestRule.waitForIdle()
    return navigator
  }

  private val decoration =
    NavStageDecoration(
      strategies =
        listOf(
          ListDetailNavStageStrategy(
            isListPane = { it is TestListPane },
            isDetailPane = { it is TestDetailPane },
            isMultiPane = { multiPane },
          )
        ),
      stageTransition = GestureNavStageTransition(),
    )

  private fun settle(block: () -> Unit) {
    composeTestRule.runOnIdle(block)
    composeTestRule.waitForIdle()
  }

  /** Enters (true) and leaves (false) of [screen]'s body, in order. */
  private fun lifecycleOf(screen: RecordScreen): List<Boolean> =
    events.filter { it.label == screen.label }.map { it.enter }

  /** Every instance of [screen] after the first restored the first's saveable and retained ids. */
  private fun assertStateFollowed(screen: RecordScreen) {
    val built = instances.getValue(screen.label)
    assertEquals(List(built.size) { built.first() }, built)
  }

  private fun composedLabels(): Set<String> {
    val live = mutableMapOf<String, Int>()
    events.forEach { live.merge(it.label, if (it.enter) 1 else -1, Int::plus) }
    return live.filterValues { it > 0 }.keys
  }
}

private fun enter(screen: RecordScreen) = BodyEvent(enter = true, screen.label)

private fun leave(screen: RecordScreen) = BodyEvent(enter = false, screen.label)

private data class BodyEvent(val enter: Boolean, val label: String)

private data class Instance(val saved: Int, val retained: Int)

private data class RecordState(val label: String, val retained: Int) : CircuitUiState

private sealed class RecordScreen(val label: String) : Screen

private data object ListRecord : RecordScreen("list"), TestListPane

private data object DetailRecord1 : RecordScreen("detail1"), TestDetailPane

private data object DetailRecord2 : RecordScreen("detail2"), TestDetailPane

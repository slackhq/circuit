// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.backhandler.LocalCompatNavigationEventDispatcherOwner
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEvent.Companion.EDGE_LEFT
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.internal.test.TestContentTags.TAG_GO_NEXT
import com.slack.circuit.internal.test.TestContentTags.TAG_LABEL
import com.slack.circuit.internal.test.TestCountPresenter
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.createTestCircuit
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.popRoot
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class NavigatorBackHandlerTest {

  @get:Rule val composeTestRule = createComposeRule()

  @OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
  @Test
  fun nestedNavigatorRootPopBackHandler() {
    val dispatcher = BackDispatcher()
    val circuit = createTestCircuit(rememberType = TestCountPresenter.RememberType.Standard)
    var outerBackCount by mutableStateOf(0)
    lateinit var navigator: Navigator
    composeTestRule.setContent {
      CompositionLocalProvider(
        LocalCompatNavigationEventDispatcherOwner provides dispatcher,
        LocalLifecycleOwner provides StartedLifecycleOwner,
      ) {
        CircuitCompositionLocals(circuit) {
          @Suppress("DEPRECATION") BackHandler(enabled = true) { outerBackCount++ }
          val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
          val circuitNavigator =
            rememberCircuitNavigator(
              backStack = backStack,
              onRootPop = { dispatcher.onBack() },
              enableBackHandler = true,
            )
          navigator = rememberInterceptingNavigator(circuitNavigator)
          NavigableCircuitContent(navigator = navigator, backStack = backStack)
        }
      }
    }
    composeTestRule.run {
      // Navigate to Screen B
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      // Navigate to Screen C
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      // Pop through the onRootPop into the back handler
      navigator.popRoot()
      waitForIdle()
      assertEquals(1, outerBackCount)
    }
  }

  @OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
  @Test
  fun nestedNavigatorRootDispatchedBackHandler() {
    val dispatcher = BackDispatcher()
    val circuit = createTestCircuit(rememberType = TestCountPresenter.RememberType.Standard)
    var outerBackCount by mutableStateOf(0)
    lateinit var navigator: Navigator
    composeTestRule.setContent {
      CompositionLocalProvider(
        LocalCompatNavigationEventDispatcherOwner provides dispatcher,
        LocalLifecycleOwner provides StartedLifecycleOwner,
      ) {
        CircuitCompositionLocals(circuit) {
          @Suppress("DEPRECATION") // Migrate to NavigationEventHandler
          BackHandler(enabled = true) { outerBackCount++ }
          val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
          val circuitNavigator =
            rememberCircuitNavigator(
              backStack = backStack,
              onRootPop = { dispatcher.onBack() },
              enableBackHandler = true,
            )
          navigator = rememberInterceptingNavigator(circuitNavigator)
          NavigableCircuitContent(navigator = navigator, backStack = backStack)
        }
      }
    }
    composeTestRule.run {
      // Navigate to Screen B
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      // Navigate to Screen C
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      // Back through the onRootPop into the back handler
      dispatcher.onBack()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      dispatcher.onBack()
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      dispatcher.onBack()
      waitForIdle()
      assertEquals(1, outerBackCount)
    }
  }
}

private class BackDispatcher : NavigationEventDispatcherOwner {

  private val input = DirectNavigationEventInput()
  override val navigationEventDispatcher: NavigationEventDispatcher = NavigationEventDispatcher()

  init {
    navigationEventDispatcher.addInput(input)
  }

  fun onBack() {
    input.backStarted(NavigationEvent(swipeEdge = EDGE_LEFT))
    input.backProgressed(NavigationEvent(swipeEdge = EDGE_LEFT, progress = 0.5f))
    input.backCompleted()
  }
}

private val StartedLifecycleOwner =
  object : Lifecycle(), LifecycleOwner {
    override val lifecycle: Lifecycle = this
    override val currentState: State = State.STARTED

    override fun addObserver(observer: LifecycleObserver) = Unit

    override fun removeObserver(observer: LifecycleObserver) = Unit
  }

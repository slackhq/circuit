// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import androidx.compose.ui.test.junit4.createComposeRule
import com.slack.circuit.internal.test.TestPopResult
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.FakeNavigationEventListener.GoToEvent
import com.slack.circuitx.navigation.intercepting.FakeNavigationEventListener.OnBackStackChangedEvent
import com.slack.circuitx.navigation.intercepting.FakeNavigationEventListener.PopEvent
import com.slack.circuitx.navigation.intercepting.FakeNavigationEventListener.ResetRootEvent
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor.Companion.SuccessConsumed
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

private val ADDITIONAL_SCREENS = arrayOf<Screen>(TestScreen.ScreenB, TestScreen.ScreenA)
private val INITIAL_ADDITIONAL_BACKSTACK =
  listOf(TestScreen.ScreenA, TestScreen.ScreenB, TestScreen.RootAlpha)

class NavigationEventListenerTest {

  @get:Rule val composeTestRule = createComposeRule()
  private val fakeInterceptor = FakeNavigationInterceptor()
  private val singleInterceptor = listOf(fakeInterceptor)
  private val fakeEventListener = FakeNavigationEventListener()
  private val singleEventListener = listOf(fakeEventListener)

  @Test
  fun `onBackStackChanged initial state`() = runTest {
    composeTestRule.run {
      val (_, _) =
        setTestContent(eventListeners = singleEventListener, initialScreen = TestScreen.RootBeta)
      assertEquals(
        OnBackStackChangedEvent(listOf(TestScreen.RootBeta)),
        fakeEventListener.awaitOnBackStackChanged(),
      )
    }
  }

  @Test
  fun `onBackStackChanged after navigation`() = runTest {
    composeTestRule.run {
      val (_, interceptingNavigator) = setTestContent(eventListeners = singleEventListener)
      fakeEventListener.skipOnBackStackChanged(1)
      interceptingNavigator.goTo(TestScreen.ScreenA)
      awaitIdle()
      assertEquals(
        OnBackStackChangedEvent(listOf(TestScreen.ScreenA, TestScreen.RootAlpha)),
        fakeEventListener.awaitOnBackStackChanged(),
      )
    }
  }

  @Test
  fun `onBackStackChanged multiple changes`() = runTest {
    composeTestRule.run {
      val (_, interceptingNavigator) = setTestContent(eventListeners = singleEventListener)
      fakeEventListener.skipOnBackStackChanged(1)
      interceptingNavigator.goTo(TestScreen.ScreenB)
      awaitIdle()
      assertEquals(
        OnBackStackChangedEvent(INITIAL_ADDITIONAL_BACKSTACK.subList(1, 3)),
        fakeEventListener.awaitOnBackStackChanged(),
      )
      interceptingNavigator.goTo(TestScreen.ScreenA)
      awaitIdle()
      assertEquals(
        OnBackStackChangedEvent(INITIAL_ADDITIONAL_BACKSTACK),
        fakeEventListener.awaitOnBackStackChanged(),
      )
    }
  }

  @Test
  fun `goTo normal navigation`() = runTest {
    composeTestRule.run {
      val (_, interceptingNavigator) = setTestContent(eventListeners = singleEventListener)
      interceptingNavigator.goTo(TestScreen.ScreenA)
      assertEquals(GoToEvent(TestScreen.ScreenA), fakeEventListener.awaitGoTo())
    }
  }

  @Test
  fun `goTo multiple sequential calls`() = runTest {
    composeTestRule.run {
      val (_, interceptingNavigator) = setTestContent(eventListeners = singleEventListener)
      interceptingNavigator.goTo(TestScreen.ScreenA)
      assertEquals(GoToEvent(TestScreen.ScreenA), fakeEventListener.awaitGoTo())
      interceptingNavigator.goTo(TestScreen.ScreenB)
      assertEquals(GoToEvent(TestScreen.ScreenB), fakeEventListener.awaitGoTo())
      interceptingNavigator.goTo(TestScreen.ScreenC)
      assertEquals(GoToEvent(TestScreen.ScreenC), fakeEventListener.awaitGoTo())
    }
  }

  @Test
  fun `goTo if intercepted reports no events`() = runTest {
    composeTestRule.run {
      fakeInterceptor.queueGoTo(SuccessConsumed)
      val (_, interceptingNavigator) =
        setTestContent(interceptors = singleInterceptor, eventListeners = singleEventListener)
      interceptingNavigator.goTo(TestScreen.ScreenA)
      fakeEventListener.assertGoToIsEmpty()
    }
  }

  @Test
  fun `pop normal operation`() = runTest {
    composeTestRule.run {
      val (_, interceptingNavigator) =
        setTestContent(eventListeners = singleEventListener, additionalScreens = ADDITIONAL_SCREENS)
      interceptingNavigator.pop()
      assertEquals(PopEvent(INITIAL_ADDITIONAL_BACKSTACK), fakeEventListener.awaitPop())
    }
  }

  @Test
  fun `pop multiple sequential pops`() = runTest {
    composeTestRule.run {
      val (_, interceptingNavigator) =
        setTestContent(eventListeners = singleEventListener, additionalScreens = ADDITIONAL_SCREENS)
      interceptingNavigator.pop()
      assertEquals(PopEvent(INITIAL_ADDITIONAL_BACKSTACK), fakeEventListener.awaitPop())
      interceptingNavigator.pop()
      assertEquals(
        PopEvent(INITIAL_ADDITIONAL_BACKSTACK.subList(1, 3)),
        fakeEventListener.awaitPop(),
      )
      interceptingNavigator.pop()
      assertEquals(
        PopEvent(INITIAL_ADDITIONAL_BACKSTACK.subList(2, 3)),
        fakeEventListener.awaitPop(),
      )
    }
  }

  @Test
  fun `pop if intercepted reports no events`() = runTest {
    composeTestRule.run {
      fakeInterceptor.queuePop(SuccessConsumed)
      val (_, interceptingNavigator) =
        setTestContent(
          interceptors = singleInterceptor,
          eventListeners = singleEventListener,
          additionalScreens = ADDITIONAL_SCREENS,
        )
      interceptingNavigator.pop()
      fakeEventListener.assertPopIsEmpty()
    }
  }

  @Test
  fun `pop with a pop result`() = runTest {
    composeTestRule.run {
      val (_, interceptingNavigator) =
        setTestContent(eventListeners = singleEventListener, additionalScreens = ADDITIONAL_SCREENS)
      interceptingNavigator.pop(TestPopResult.PopResultA)
      assertEquals(
        PopEvent(INITIAL_ADDITIONAL_BACKSTACK, TestPopResult.PopResultA),
        fakeEventListener.awaitPop(),
      )
    }
  }

  @Test
  fun `resetRoot normal operation`() = runTest {
    composeTestRule.run {
      val (_, interceptingNavigator) =
        setTestContent(eventListeners = singleEventListener, additionalScreens = ADDITIONAL_SCREENS)
      interceptingNavigator.resetRoot(TestScreen.RootBeta)
      assertEquals(ResetRootEvent(TestScreen.RootBeta), fakeEventListener.awaitResetRoot())
    }
  }

  @Test
  fun `resetRoot if intercepted reports no events`() = runTest {
    composeTestRule.run {
      fakeInterceptor.queueResetRoot(SuccessConsumed)
      val (_, interceptingNavigator) =
        setTestContent(interceptors = singleInterceptor, eventListeners = singleEventListener)
      interceptingNavigator.resetRoot(TestScreen.ScreenA)
      fakeEventListener.assertResetRootIsEmpty()
    }
  }

  @Test
  fun `resetRoot saveState true`() = runTest {
    composeTestRule.run {
      val (_, interceptingNavigator) =
        setTestContent(eventListeners = singleEventListener, additionalScreens = ADDITIONAL_SCREENS)
      interceptingNavigator.resetRoot(TestScreen.RootBeta, saveState = true)
      assertEquals(
        ResetRootEvent(TestScreen.RootBeta, saveState = true),
        fakeEventListener.awaitResetRoot(),
      )
    }
  }

  @Test
  fun `resetRoot restoreState true`() = runTest {
    composeTestRule.run {
      val (_, interceptingNavigator) =
        setTestContent(eventListeners = singleEventListener, additionalScreens = ADDITIONAL_SCREENS)
      interceptingNavigator.resetRoot(TestScreen.RootBeta, restoreState = true)
      assertEquals(
        ResetRootEvent(TestScreen.RootBeta, restoreState = true),
        fakeEventListener.awaitResetRoot(),
      )
    }
  }

  @Test
  fun `resetRoot saveState false`() = runTest {
    composeTestRule.run {
      val (_, interceptingNavigator) =
        setTestContent(eventListeners = singleEventListener, additionalScreens = ADDITIONAL_SCREENS)
      interceptingNavigator.resetRoot(TestScreen.RootBeta, saveState = false)
      assertEquals(
        ResetRootEvent(TestScreen.RootBeta, saveState = false),
        fakeEventListener.awaitResetRoot(),
      )
    }
  }

  @Test
  fun `resetRoot restoreState false`() = runTest {
    composeTestRule.run {
      val (_, interceptingNavigator) =
        setTestContent(eventListeners = singleEventListener, additionalScreens = ADDITIONAL_SCREENS)
      interceptingNavigator.resetRoot(TestScreen.RootBeta, restoreState = false)
      assertEquals(
        ResetRootEvent(TestScreen.RootBeta, restoreState = false),
        fakeEventListener.awaitResetRoot(),
      )
    }
  }

  @Test
  fun `resetRoot multiple sequential calls`() = runTest {
    composeTestRule.run {
      val (_, interceptingNavigator) =
        setTestContent(eventListeners = singleEventListener, additionalScreens = ADDITIONAL_SCREENS)
      interceptingNavigator.resetRoot(TestScreen.RootBeta)
      assertEquals(ResetRootEvent(TestScreen.RootBeta), fakeEventListener.awaitResetRoot())
      interceptingNavigator.resetRoot(TestScreen.ScreenC)
      assertEquals(ResetRootEvent(TestScreen.ScreenC), fakeEventListener.awaitResetRoot())
      interceptingNavigator.resetRoot(TestScreen.ScreenA)
      assertEquals(ResetRootEvent(TestScreen.ScreenA), fakeEventListener.awaitResetRoot())
    }
  }
}

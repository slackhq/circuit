package com.slack.circuitx.navigation.intercepting

import androidx.compose.ui.test.junit4.createComposeRule
import com.slack.circuit.internal.test.TestPopResult
import com.slack.circuit.internal.test.TestScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class InterceptingNavigatorTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `Navigator delegation check`() = runTest {
    composeTestRule.run {
      val (fakeNavigator, interceptingNavigator) = setTestContent()

      // Verify goTo is delegated.
      interceptingNavigator.goTo(TestScreen.ScreenA)
      val goToEvent = fakeNavigator.awaitNextGoTo()
      assertTrue(goToEvent.success)
      assertEquals(TestScreen.ScreenA, goToEvent.screen)

      // Verify pop is delegated.
      interceptingNavigator.pop(TestPopResult.PopResultA)
      val popEvent = fakeNavigator.awaitPop()
      assertEquals(TestScreen.ScreenA, popEvent.poppedScreen)
      assertEquals(TestPopResult.PopResultA, popEvent.result)

      // Verify resetRoot is delegated.
      interceptingNavigator.resetRoot(TestScreen.RootBeta, saveState = true, restoreState = true)
      val resetRootEvent = fakeNavigator.awaitResetRoot()
      assertEquals(TestScreen.RootBeta, resetRootEvent.newRoot)
      assertTrue(resetRootEvent.saveState)
      assertTrue(resetRootEvent.restoreState)

      // Navigate around a bit to ensure the back stack matches.
      interceptingNavigator.goTo(TestScreen.ScreenA)
      interceptingNavigator.goTo(TestScreen.ScreenB)
      interceptingNavigator.pop()
      interceptingNavigator.goTo(TestScreen.ScreenC)
      interceptingNavigator.goTo(TestScreen.ScreenB)
      interceptingNavigator.pop()
      assertEquals(fakeNavigator.peekBackStack(), interceptingNavigator.peekBackStack())
    }
  }
}

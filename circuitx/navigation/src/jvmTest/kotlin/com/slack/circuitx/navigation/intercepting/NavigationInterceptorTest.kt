// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import androidx.compose.ui.test.junit4.createComposeRule
import com.slack.circuit.internal.test.TestPopResult
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor.Companion.SuccessConsumed
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor.Companion.Skipped
import com.slack.circuitx.navigation.intercepting.FakeNavigationInterceptor.GoToEvent
import com.slack.circuitx.navigation.intercepting.FakeNavigationInterceptor.PopEvent
import com.slack.circuitx.navigation.intercepting.FakeNavigationInterceptor.ResetRootEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class NavigationInterceptorTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val fakeInterceptor1 = FakeNavigationInterceptor()
  private val fakeInterceptor2 = FakeNavigationInterceptor()
  private val singleInterceptor = persistentListOf(fakeInterceptor1)
  private val interceptors = persistentListOf(fakeInterceptor1, fakeInterceptor2)

  @Test
  fun `Single interceptor goTo success`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueGoTo(SuccessConsumed)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      assertTrue(interceptingNavigator.goTo(TestScreen.ScreenA))
      assertEquals(GoToEvent(TestScreen.ScreenA, SuccessConsumed), fakeInterceptor1.awaitGoTo())
      fakeNavigator.assertGoToIsEmpty()
    }
  }

  @Test
  fun `Single interceptor goTo skipped`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueGoTo(Skipped)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      assertTrue(interceptingNavigator.goTo(TestScreen.ScreenA))
      assertEquals(GoToEvent(TestScreen.ScreenA, Skipped), fakeInterceptor1.awaitGoTo())
      assertEquals(TestScreen.ScreenA, fakeNavigator.awaitNextScreen())
    }
  }

  @Test
  fun `Single interceptor goTo failure consumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueGoTo(FailConsumed)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      // Consumed so the delegate should not be called and nav should fail.
      assertFalse(interceptingNavigator.goTo(TestScreen.ScreenA))
      assertEquals(GoToEvent(TestScreen.ScreenA, FailConsumed), fakeInterceptor1.awaitGoTo())
      fakeNavigator.assertGoToIsEmpty()
    }
  }

  @Test
  fun `Single interceptor goTo failure unconsumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueGoTo(FailUnconsumed)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      // Not consumed so the delegate should be called and nav should succeed.
      assertTrue(interceptingNavigator.goTo(TestScreen.ScreenA))
      assertEquals(GoToEvent(TestScreen.ScreenA, FailUnconsumed), fakeInterceptor1.awaitGoTo())
      assertEquals(TestScreen.ScreenA, fakeNavigator.awaitNextScreen())
    }
  }

  @Test
  fun `Single interceptor goTo Rewrite`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueGoTo(InterceptedGoToResult.Rewrite(TestScreen.RootBeta), Skipped)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      // Rewrite is not intercepted so delegate should be called and nav should succeed.
      assertTrue(interceptingNavigator.goTo(TestScreen.ScreenA))
      assertEquals(
        GoToEvent(TestScreen.ScreenA, InterceptedGoToResult.Rewrite(TestScreen.RootBeta)),
        fakeInterceptor1.awaitGoTo(),
      )
      assertEquals(GoToEvent(TestScreen.RootBeta, Skipped), fakeInterceptor1.awaitGoTo())
      assertEquals(TestScreen.RootBeta, fakeNavigator.awaitNextScreen())
    }
  }

  @Test
  fun `Single interceptor goTo Rewrite, intercept Rewrite consumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueGoTo(
        InterceptedGoToResult.Rewrite(TestScreen.RootBeta),
        SuccessConsumed,
      )
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      // Rewrite is intercepted so delegate should not be called and nav should succeed.
      assertTrue(interceptingNavigator.goTo(TestScreen.ScreenA))
      assertEquals(
        GoToEvent(TestScreen.ScreenA, InterceptedGoToResult.Rewrite(TestScreen.RootBeta)),
        fakeInterceptor1.awaitGoTo(),
      )
      assertEquals(GoToEvent(TestScreen.RootBeta, SuccessConsumed), fakeInterceptor1.awaitGoTo())
      fakeNavigator.assertGoToIsEmpty()
    }
  }

  @Test
  fun `Single interceptor goTo Rewrite, intercept Rewrite unconsumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueGoTo(
        InterceptedGoToResult.Rewrite(TestScreen.RootBeta),
        SuccessUnconsumed,
      )
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      // Rewrite is intercepted so delegate should be called and nav should succeed.
      assertTrue(interceptingNavigator.goTo(TestScreen.ScreenA))
      assertEquals(
        GoToEvent(TestScreen.ScreenA, InterceptedGoToResult.Rewrite(TestScreen.RootBeta)),
        fakeInterceptor1.awaitGoTo(),
      )
      assertEquals(GoToEvent(TestScreen.RootBeta, SuccessUnconsumed), fakeInterceptor1.awaitGoTo())
      assertEquals(TestScreen.RootBeta, fakeNavigator.awaitNextScreen())
    }
  }

  @Test
  fun `Multiple interceptors goTo success`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueGoTo(SuccessConsumed)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = interceptors)
      // Intercepted by the first so the second one and the navigator should not be called.
      assertTrue(interceptingNavigator.goTo(TestScreen.ScreenA))
      assertEquals(GoToEvent(TestScreen.ScreenA, SuccessConsumed), fakeInterceptor1.awaitGoTo())
      fakeInterceptor2.assertGoToIsEmpty()
      fakeNavigator.assertGoToIsEmpty()
    }
  }

  @Test
  fun `Multiple interceptors goTo skipped`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueGoTo(Skipped)
      fakeInterceptor2.queueGoTo(Skipped)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = interceptors)
      // Skipped by the first and the second one, so only the navigator should be called.
      assertTrue(interceptingNavigator.goTo(TestScreen.ScreenA))
      assertEquals(GoToEvent(TestScreen.ScreenA, Skipped), fakeInterceptor1.awaitGoTo())
      assertEquals(GoToEvent(TestScreen.ScreenA, Skipped), fakeInterceptor2.awaitGoTo())
      assertEquals(TestScreen.ScreenA, fakeNavigator.awaitNextScreen())
    }
  }

  @Test
  fun `Multiple interceptors goTo failure consumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueGoTo(FailConsumed)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = interceptors)
      // Consumed by the first so the second one and the navigator should not be called.
      assertFalse(interceptingNavigator.goTo(TestScreen.ScreenA))
      assertEquals(GoToEvent(TestScreen.ScreenA, FailConsumed), fakeInterceptor1.awaitGoTo())
      fakeInterceptor2.assertGoToIsEmpty()
      fakeNavigator.assertGoToIsEmpty()
    }
  }

  @Test
  fun `Multiple interceptors goTo failure unconsumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueGoTo(FailUnconsumed)
      fakeInterceptor2.queueGoTo(Skipped)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = interceptors)
      // Unconsumed by the first so the second one and the navigator should be called.
      assertTrue(interceptingNavigator.goTo(TestScreen.ScreenA))
      assertEquals(GoToEvent(TestScreen.ScreenA, FailUnconsumed), fakeInterceptor1.awaitGoTo())
      assertEquals(GoToEvent(TestScreen.ScreenA, Skipped), fakeInterceptor2.awaitGoTo())
      assertEquals(TestScreen.ScreenA, fakeNavigator.awaitNextScreen())
    }
  }

  @Test
  fun `Single interceptor pop success consumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queuePop(SuccessConsumed)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      fakeNavigator.goTo(TestScreen.ScreenA)
      val peekBackStack = fakeNavigator.peekBackStack()
      assertNull(interceptingNavigator.pop(TestPopResult.PopResultA))
      assertEquals(
        PopEvent(peekBackStack, TestPopResult.PopResultA, SuccessConsumed),
        fakeInterceptor1.awaitPop(),
      )
      fakeNavigator.assertPopIsEmpty()
    }
  }

  @Test
  fun `Single interceptor pop skipped`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queuePop(Skipped)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      fakeNavigator.goTo(TestScreen.ScreenA)
      val peekBackStack = fakeNavigator.peekBackStack()
      assertEquals(TestScreen.ScreenA, interceptingNavigator.pop(TestPopResult.PopResultA))
      assertEquals(
        PopEvent(peekBackStack, TestPopResult.PopResultA, Skipped),
        fakeInterceptor1.awaitPop(),
      )
      val popEvent = fakeNavigator.awaitPop()
      assertEquals(TestScreen.ScreenA, popEvent.poppedScreen)
      assertEquals(TestPopResult.PopResultA, popEvent.result)
    }
  }

  @Test
  fun `Single interceptor pop failure consumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queuePop(FailConsumed)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      fakeNavigator.goTo(TestScreen.ScreenA)
      val peekBackStack = fakeNavigator.peekBackStack()
      assertNull(interceptingNavigator.pop(TestPopResult.PopResultA))
      assertEquals(
        PopEvent(peekBackStack, TestPopResult.PopResultA, FailConsumed),
        fakeInterceptor1.awaitPop(),
      )
      fakeNavigator.assertPopIsEmpty()
    }
  }

  @Test
  fun `Single interceptor pop failure unconsumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queuePop(FailUnconsumed)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      fakeNavigator.goTo(TestScreen.ScreenA)
      val peekBackStack = fakeNavigator.peekBackStack()
      // Not consumed so the delegate should be called and nav should succeed.
      assertEquals(TestScreen.ScreenA, interceptingNavigator.pop(TestPopResult.PopResultA))
      assertEquals(
        PopEvent(peekBackStack, TestPopResult.PopResultA, FailUnconsumed),
        fakeInterceptor1.awaitPop(),
      )
      val popEvent = fakeNavigator.awaitPop()
      assertEquals(TestScreen.ScreenA, popEvent.poppedScreen)
      assertEquals(TestPopResult.PopResultA, popEvent.result)
    }
  }

  @Test
  fun `Multiple interceptors pop success consumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queuePop(SuccessConsumed)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = interceptors)
      fakeNavigator.goTo(TestScreen.ScreenA)
      val peekBackStack = fakeNavigator.peekBackStack()
      // Intercepted by the first so the second one and the navigator should not be called.
      assertNull(interceptingNavigator.pop(TestPopResult.PopResultA))
      assertEquals(
        PopEvent(peekBackStack, TestPopResult.PopResultA, SuccessConsumed),
        fakeInterceptor1.awaitPop(),
      )
      fakeInterceptor2.assertPopIsEmpty()
      fakeNavigator.assertPopIsEmpty()
    }
  }

  @Test
  fun `Multiple interceptors pop success unconsumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queuePop(SuccessConsumed)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = interceptors)
      fakeNavigator.goTo(TestScreen.ScreenA)
      val peekBackStack = fakeNavigator.peekBackStack()
      // Intercepted by the first so the second one and the navigator should not be called.
      assertNull(interceptingNavigator.pop(TestPopResult.PopResultA))
      assertEquals(
        PopEvent(peekBackStack, TestPopResult.PopResultA, SuccessConsumed),
        fakeInterceptor1.awaitPop(),
      )
      fakeInterceptor2.assertPopIsEmpty()
      fakeNavigator.assertPopIsEmpty()
    }
  }

  @Test
  fun `Multiple interceptors pop skipped`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queuePop(Skipped)
      fakeInterceptor2.queuePop(Skipped)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = interceptors)
      fakeNavigator.goTo(TestScreen.ScreenA)
      val peekBackStack = fakeNavigator.peekBackStack()
      // Not intercepted by the first or second, so the navigator should be called.
      assertEquals(TestScreen.ScreenA, interceptingNavigator.pop(TestPopResult.PopResultA))
      assertEquals(
        PopEvent(peekBackStack, TestPopResult.PopResultA, Skipped),
        fakeInterceptor1.awaitPop(),
      )
      assertEquals(
        PopEvent(peekBackStack, TestPopResult.PopResultA, Skipped),
        fakeInterceptor2.awaitPop(),
      )
      val popEvent = fakeNavigator.awaitPop()
      assertEquals(TestScreen.ScreenA, popEvent.poppedScreen)
      assertEquals(TestPopResult.PopResultA, popEvent.result)
    }
  }

  @Test
  fun `Multiple interceptors pop failure consumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queuePop(FailConsumed)
      fakeInterceptor2.queuePop(Skipped)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = interceptors)
      fakeNavigator.goTo(TestScreen.ScreenA)
      val peekBackStack = fakeNavigator.peekBackStack()
      // Consumed by the first so the second and the navigator should not be called.
      assertNull(interceptingNavigator.pop(TestPopResult.PopResultA))
      assertEquals(
        PopEvent(peekBackStack, TestPopResult.PopResultA, FailConsumed),
        fakeInterceptor1.awaitPop(),
      )
      fakeInterceptor2.assertPopIsEmpty()
      fakeNavigator.assertPopIsEmpty()
    }
  }

  @Test
  fun `Multiple interceptors pop failure unconsumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queuePop(FailUnconsumed)
      fakeInterceptor2.queuePop(Skipped)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = interceptors)
      fakeNavigator.goTo(TestScreen.ScreenA)
      val peekBackStack = fakeNavigator.peekBackStack()
      // Unconsumed by the first so the second and the navigator should be called.
      assertEquals(TestScreen.ScreenA, interceptingNavigator.pop(TestPopResult.PopResultA))
      assertEquals(
        PopEvent(peekBackStack, TestPopResult.PopResultA, FailUnconsumed),
        fakeInterceptor1.awaitPop(),
      )
      assertEquals(
        PopEvent(peekBackStack, TestPopResult.PopResultA, Skipped),
        fakeInterceptor2.awaitPop(),
      )
      val popEvent = fakeNavigator.awaitPop()
      assertEquals(TestScreen.ScreenA, popEvent.poppedScreen)
      assertEquals(TestPopResult.PopResultA, popEvent.result)
    }
  }

  @Test
  fun `Single interceptor resetRoot success`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueResetRoot(SuccessConsumed)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      assertTrue(interceptingNavigator.resetRoot(TestScreen.RootBeta).isEmpty())
      assertEquals(
        ResetRootEvent(TestScreen.RootBeta, SuccessConsumed),
        fakeInterceptor1.awaitResetRoot(),
      )
      fakeNavigator.assertResetRootIsEmpty()
    }
  }

  @Test
  fun `Single interceptor resetRoot skipped`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueResetRoot(Skipped)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      val peekBackStack = fakeNavigator.peekBackStack()
      assertEquals(peekBackStack, interceptingNavigator.resetRoot(TestScreen.RootBeta))
      assertEquals(ResetRootEvent(TestScreen.RootBeta, Skipped), fakeInterceptor1.awaitResetRoot())
      val event = fakeNavigator.awaitResetRoot()
      assertEquals(TestScreen.RootBeta, event.newRoot)
    }
  }

  @Test
  fun `Single interceptor resetRoot failure consumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueResetRoot(FailConsumed)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      // Consumed so the delegate should not be called and nav should fail.
      assertFalse(interceptingNavigator.resetRoot(TestScreen.RootBeta).isNotEmpty())
      assertEquals(
        ResetRootEvent(TestScreen.RootBeta, FailConsumed),
        fakeInterceptor1.awaitResetRoot(),
      )
      fakeNavigator.assertResetRootIsEmpty()
    }
  }

  @Test
  fun `Single interceptor resetRoot failure unconsumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueResetRoot(FailUnconsumed)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      // Not consumed so the delegate should be called and nav should succeed.
      val peekBackStack = fakeNavigator.peekBackStack()
      assertEquals(peekBackStack, interceptingNavigator.resetRoot(TestScreen.RootBeta))
      assertEquals(
        ResetRootEvent(TestScreen.RootBeta, FailUnconsumed),
        fakeInterceptor1.awaitResetRoot(),
      )
      val event = fakeNavigator.awaitResetRoot()
      assertEquals(TestScreen.RootBeta, event.newRoot)
    }
  }

  @Test
  fun `Single interceptor resetRoot Rewrite`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueResetRoot(
        InterceptedResetRootResult.Rewrite(
          TestScreen.RootBeta,
          saveState = true,
          restoreState = true,
        ),
        Skipped,
      )
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      val peekBackStack = fakeNavigator.peekBackStack()
      // Rewrite is not intercepted so delegate should be called and nav should succeed.
      assertEquals(peekBackStack, interceptingNavigator.resetRoot(TestScreen.ScreenA))
      assertEquals(
        ResetRootEvent(
          TestScreen.ScreenA,
          InterceptedResetRootResult.Rewrite(
            TestScreen.RootBeta,
            saveState = true,
            restoreState = true,
          ),
        ),
        fakeInterceptor1.awaitResetRoot(),
      )
      assertEquals(
        ResetRootEvent(TestScreen.RootBeta, Skipped, saveState = true, restoreState = true),
        fakeInterceptor1.awaitResetRoot(),
      )
      val event = fakeNavigator.awaitResetRoot()
      assertEquals(TestScreen.RootBeta, event.newRoot)
      assertTrue(event.saveState)
      assertTrue(event.restoreState)
    }
  }

  @Test
  fun `Single interceptor resetRoot Rewrite, intercept Rewrite consumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueResetRoot(
        InterceptedResetRootResult.Rewrite(
          TestScreen.RootBeta,
          saveState = false,
          restoreState = false,
        ),
        SuccessConsumed,
      )
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      // Rewrite is intercepted so delegate should not be called and nav should succeed.
      assertTrue(interceptingNavigator.resetRoot(TestScreen.ScreenA).isEmpty())
      assertEquals(
        ResetRootEvent(
          TestScreen.ScreenA,
          InterceptedResetRootResult.Rewrite(
            TestScreen.RootBeta,
            saveState = false,
            restoreState = false,
          ),
        ),
        fakeInterceptor1.awaitResetRoot(),
      )
      assertEquals(
        ResetRootEvent(TestScreen.RootBeta, SuccessConsumed),
        fakeInterceptor1.awaitResetRoot(),
      )
      fakeNavigator.assertGoToIsEmpty()
    }
  }

  @Test
  fun `Single interceptor resetRoot Rewrite, intercept Rewrite unconsumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueResetRoot(
        InterceptedResetRootResult.Rewrite(
          TestScreen.RootBeta,
          saveState = false,
          restoreState = false,
        ),
        SuccessUnconsumed,
      )
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = singleInterceptor)
      val peekBackStack = fakeNavigator.peekBackStack()
      // Rewrite is intercepted so delegate should be called and nav should succeed.
      assertEquals(peekBackStack, interceptingNavigator.resetRoot(TestScreen.ScreenA))
      assertEquals(
        ResetRootEvent(
          TestScreen.ScreenA,
          InterceptedResetRootResult.Rewrite(
            TestScreen.RootBeta,
            saveState = false,
            restoreState = false,
          ),
        ),
        fakeInterceptor1.awaitResetRoot(),
      )
      assertEquals(
        ResetRootEvent(TestScreen.RootBeta, SuccessUnconsumed),
        fakeInterceptor1.awaitResetRoot(),
      )
      assertEquals(TestScreen.RootBeta, fakeNavigator.awaitResetRoot().newRoot)
    }
  }

  @Test
  fun `Multiple interceptors resetRoot success`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueResetRoot(SuccessConsumed)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = interceptors)
      // Intercepted by the first so the second one and the navigator should not be called.
      assertTrue(interceptingNavigator.resetRoot(TestScreen.RootBeta).isEmpty())
      assertEquals(
        ResetRootEvent(TestScreen.RootBeta, SuccessConsumed),
        fakeInterceptor1.awaitResetRoot(),
      )
      fakeInterceptor2.assertResetRootIsEmpty()
      fakeNavigator.assertResetRootIsEmpty()
    }
  }

  @Test
  fun `Multiple interceptors resetRoot skipped`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueResetRoot(Skipped)
      fakeInterceptor2.queueResetRoot(Skipped)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = interceptors)
      // Skipped by the first and the second one, so only the navigator should be called.
      val peekBackStack = fakeNavigator.peekBackStack()
      assertEquals(peekBackStack, interceptingNavigator.resetRoot(TestScreen.RootBeta))
      assertEquals(ResetRootEvent(TestScreen.RootBeta, Skipped), fakeInterceptor1.awaitResetRoot())
      assertEquals(ResetRootEvent(TestScreen.RootBeta, Skipped), fakeInterceptor2.awaitResetRoot())
      assertEquals(TestScreen.RootBeta, fakeNavigator.awaitResetRoot().newRoot)
    }
  }

  @Test
  fun `Multiple interceptors resetRoot failure consumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueResetRoot(FailConsumed)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = interceptors)
      // Consumed by the first so the second one and the navigator should not be called.
      assertTrue(interceptingNavigator.resetRoot(TestScreen.ScreenA).isEmpty())
      assertEquals(
        ResetRootEvent(TestScreen.ScreenA, FailConsumed),
        fakeInterceptor1.awaitResetRoot(),
      )
      fakeInterceptor2.assertResetRootIsEmpty()
      fakeNavigator.assertResetRootIsEmpty()
    }
  }

  @Test
  fun `Multiple interceptors resetRoot failure unconsumed`() = runTest {
    composeTestRule.run {
      fakeInterceptor1.queueResetRoot(FailUnconsumed)
      fakeInterceptor2.queueResetRoot(Skipped)
      val (fakeNavigator, interceptingNavigator) = setTestContent(interceptors = interceptors)
      val peekBackStack = fakeNavigator.peekBackStack()
      // Unconsumed by the first so the second one and the navigator should be called.
      assertEquals(peekBackStack, interceptingNavigator.resetRoot(TestScreen.RootBeta))
      assertEquals(
        ResetRootEvent(TestScreen.RootBeta, FailUnconsumed),
        fakeInterceptor1.awaitResetRoot(),
      )
      assertEquals(ResetRootEvent(TestScreen.RootBeta, Skipped), fakeInterceptor2.awaitResetRoot())
      assertEquals(TestScreen.RootBeta, fakeNavigator.awaitResetRoot().newRoot)
    }
  }
}

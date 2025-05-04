package com.slack.circuitx.navigation.intercepting

import androidx.compose.ui.test.junit4.createComposeRule
import com.slack.circuit.internal.test.TestScreen
import kotlin.test.assertEquals
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class FailureNotifierTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val failureNotifier = FakeFailureNotifier()
  private val fakeInterceptor = FakeNavigationInterceptor()
  private val singleInterceptor = persistentListOf(fakeInterceptor)

  @Test
  fun `goToInterceptorFailure consumed failure`() = runTest {
    composeTestRule.run {
      fakeInterceptor.queueGoTo(FailConsumed)
      val (_, interceptingNavigator) =
        setTestContent(interceptors = singleInterceptor, notifier = failureNotifier)
      interceptingNavigator.goTo(TestScreen.ScreenA)
      assertEquals(FailConsumed, failureNotifier.awaitGoToFailure())
    }
  }

  @Test
  fun `popInterceptorFailure consumed failure`() = runTest {
    composeTestRule.run {
      fakeInterceptor.queuePop(FailConsumed)
      val (_, interceptingNavigator) =
        setTestContent(interceptors = singleInterceptor, notifier = failureNotifier)
      interceptingNavigator.pop()
      assertEquals(FailConsumed, failureNotifier.awaitPopFailure())
    }
  }

  @Test
  fun `rootResetInterceptorFailure consumed failure`() = runTest {
    composeTestRule.run {
      fakeInterceptor.queueResetRoot(FailConsumed)
      val (_, interceptingNavigator) =
        setTestContent(interceptors = singleInterceptor, notifier = failureNotifier)
      interceptingNavigator.resetRoot(TestScreen.RootBeta)
      assertEquals(FailConsumed, failureNotifier.awaitRootResetFailure())
    }
  }

  @Test
  fun `goToInterceptorFailure unconsumed failure`() = runTest {
    composeTestRule.run {
      fakeInterceptor.queueGoTo(FailUnconsumed)
      val (_, interceptingNavigator) =
        setTestContent(interceptors = singleInterceptor, notifier = failureNotifier)
      interceptingNavigator.goTo(TestScreen.ScreenA)
      assertEquals(FailUnconsumed, failureNotifier.awaitGoToFailure())
    }
  }

  @Test
  fun `popInterceptorFailure unconsumed failure`() = runTest {
    composeTestRule.run {
      fakeInterceptor.queuePop(FailUnconsumed)
      val (_, interceptingNavigator) =
        setTestContent(interceptors = singleInterceptor, notifier = failureNotifier)
      interceptingNavigator.pop()
      assertEquals(FailUnconsumed, failureNotifier.awaitPopFailure())
    }
  }

  @Test
  fun `rootResetInterceptorFailure unconsumed failure`() = runTest {
    composeTestRule.run {
      fakeInterceptor.queueResetRoot(FailUnconsumed)
      val (_, interceptingNavigator) =
        setTestContent(interceptors = singleInterceptor, notifier = failureNotifier)
      interceptingNavigator.resetRoot(TestScreen.RootBeta)
      assertEquals(FailUnconsumed, failureNotifier.awaitRootResetFailure())
    }
  }

  @Test
  fun `goToInterceptorFailure failure with a reason`() = runTest {
    composeTestRule.run {
      val failure = InterceptedResult.Failure(consumed = true, reason = Throwable("Test failure"))
      fakeInterceptor.queueGoTo(failure)
      val (_, interceptingNavigator) =
        setTestContent(interceptors = singleInterceptor, notifier = failureNotifier)
      interceptingNavigator.goTo(TestScreen.ScreenA)
      assertEquals(failure, failureNotifier.awaitGoToFailure())
    }
  }

  @Test
  fun `popInterceptorFailure failure with a reason`() = runTest {
    composeTestRule.run {
      val failure = InterceptedResult.Failure(consumed = true, reason = Throwable("Test failure"))
      fakeInterceptor.queuePop(failure)
      val (_, interceptingNavigator) =
        setTestContent(interceptors = singleInterceptor, notifier = failureNotifier)
      interceptingNavigator.pop()
      assertEquals(failure, failureNotifier.awaitPopFailure())
    }
  }

  @Test
  fun `rootResetInterceptorFailure failure with a reason`() = runTest {
    composeTestRule.run {
      val failure = InterceptedResult.Failure(consumed = true, reason = Throwable("Test failure"))
      fakeInterceptor.queueResetRoot(failure)
      val (_, interceptingNavigator) =
        setTestContent(interceptors = singleInterceptor, notifier = failureNotifier)
      interceptingNavigator.resetRoot(TestScreen.RootBeta)
      assertEquals(failure, failureNotifier.awaitRootResetFailure())
    }
  }

  @Test
  fun `All Failure Types in One Run`() = runTest {
    composeTestRule.run {
      val failureGoTo =
        InterceptedResult.Failure(consumed = true, reason = Throwable("Test goto failure"))
      val failurePop =
        InterceptedResult.Failure(consumed = true, reason = Throwable("Test pop failure"))
      val failureReset =
        InterceptedResult.Failure(consumed = true, reason = Throwable("Test reset failure"))
      fakeInterceptor.queueGoTo(failureGoTo)
      fakeInterceptor.queuePop(failurePop)
      fakeInterceptor.queueResetRoot(failureReset)
      val (_, interceptingNavigator) =
        setTestContent(interceptors = singleInterceptor, notifier = failureNotifier)
      interceptingNavigator.goTo(TestScreen.ScreenA)
      interceptingNavigator.pop()
      interceptingNavigator.resetRoot(TestScreen.RootBeta)
      assertEquals(failureGoTo, failureNotifier.awaitGoToFailure())
      assertEquals(failurePop, failureNotifier.awaitPopFailure())
      assertEquals(failureReset, failureNotifier.awaitRootResetFailure())
    }
  }
}

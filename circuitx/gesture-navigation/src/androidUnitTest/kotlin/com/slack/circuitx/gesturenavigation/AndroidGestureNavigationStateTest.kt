// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.slack.circuit.internal.test.TestCountPresenter.RememberType
import com.slack.circuit.runtime.Navigator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Config(minSdk = 34)
@RunWith(ParameterizedRobolectricTestRunner::class)
class AndroidGestureNavigationStateTest(
  private val useKeys: Boolean,
  private val useSwipe: Boolean,
  private val rememberType: RememberType,
) : GestureNavigationStateTest {
  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(
      name = "{0}_useKeys={1}_useSwipe={2}_rememberType={3}"
    )
    fun params() =
      parameterizedParams()
        .combineWithParameters(true, false) // useKeys
        .combineWithParameters(true, false) // useSwipe
        .combineWithParameters(RememberType.Retained, RememberType.Saveable)
  }

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  override fun SemanticsNodeInteractionsProvider.swipeRight() {
    composeTestRule.activityRule.scenario.performGestureNavigationBackSwipe()
  }

  private fun decoratorFactory(navigator: Navigator): AndroidPredictiveNavDecorator.Factory {
    return AndroidPredictiveNavDecorator.Factory(onBackInvoked = navigator::pop)
  }

  @Test
  fun stateScopedToBackstack() {
    composeTestRule.run {
      testStateScopedToBackstack(
        useKeys = useKeys,
        useSwipe = useSwipe,
        rememberType = rememberType,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Test
  fun stateScopedToBackstack_resetRoots() {
    composeTestRule.run {
      testStateScopedToBackstack_resetRoots(
        useKeys = useKeys,
        useSwipe = useSwipe,
        rememberType = rememberType,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }
}

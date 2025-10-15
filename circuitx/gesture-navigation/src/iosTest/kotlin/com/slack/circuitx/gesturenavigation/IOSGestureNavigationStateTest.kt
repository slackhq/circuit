// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.runComposeUiTest
import com.slack.circuit.internal.test.TestCountPresenter.RememberType
import com.slack.circuit.runtime.Navigator
import kotlin.test.Ignore
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class IOSGestureNavigationStateTest : GestureNavigationStateTest {

  override fun SemanticsNodeInteractionsProvider.swipeRight() {
    // Need an instrument test to have the LocalBackGestureDispatcher setup.
    TODO("Revisit testing with swipeRight() after the upstream navigation event changes.")
  }

  private fun decoratorFactory(navigator: Navigator): IOSPredictiveBackNavDecorator.Factory {
    return IOSPredictiveBackNavDecorator.Factory(onBackInvoked = navigator::pop)
  }

  @Ignore
  @Test
  fun `stateScopedToBackstack_useKeys-true_useSwipe-true_rememberType-Retained`() {
    runComposeUiTest {
      testStateScopedToBackstack(
        useKeys = true,
        useSwipe = true,
        rememberType = RememberType.Retained,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Ignore
  @Test
  fun `stateScopedToBackstack_useKeys-true_useSwipe-true_rememberType-Saveable`() {
    runComposeUiTest {
      testStateScopedToBackstack(
        useKeys = true,
        useSwipe = true,
        rememberType = RememberType.Saveable,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Test
  fun `stateScopedToBackstack_useKeys-true_useSwipe-false_rememberType-Retained`() {
    runComposeUiTest {
      testStateScopedToBackstack(
        useKeys = true,
        useSwipe = false,
        rememberType = RememberType.Retained,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Test
  fun `stateScopedToBackstack_useKeys-true_useSwipe-false_rememberType-Saveable`() {
    runComposeUiTest {
      testStateScopedToBackstack(
        useKeys = true,
        useSwipe = false,
        rememberType = RememberType.Saveable,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Ignore
  @Test
  fun `stateScopedToBackstack_useKeys-false_useSwipe-true_rememberType-Retained`() {
    runComposeUiTest {
      testStateScopedToBackstack(
        useKeys = false,
        useSwipe = true,
        rememberType = RememberType.Retained,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Ignore
  @Test
  fun `stateScopedToBackstack_useKeys-false_useSwipe-true_rememberType-Saveable`() {
    runComposeUiTest {
      testStateScopedToBackstack(
        useKeys = false,
        useSwipe = true,
        rememberType = RememberType.Saveable,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Test
  fun `stateScopedToBackstack_useKeys-false_useSwipe-false_rememberType-Retained`() {
    runComposeUiTest {
      testStateScopedToBackstack(
        useKeys = false,
        useSwipe = false,
        rememberType = RememberType.Retained,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Test
  fun `stateScopedToBackstack_useKeys-false_useSwipe-false_rememberType-Saveable`() {
    runComposeUiTest {
      testStateScopedToBackstack(
        useKeys = false,
        useSwipe = false,
        rememberType = RememberType.Saveable,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Ignore
  @Test
  fun `stateScopedToBackstack_resetRoots_useKeys-true_useSwipe-true_rememberType-Retained`() {
    runComposeUiTest {
      testStateScopedToBackstack_resetRoots(
        useKeys = true,
        useSwipe = true,
        rememberType = RememberType.Retained,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Ignore
  @Test
  fun `stateScopedToBackstack_resetRoots_useKeys-true_useSwipe-true_rememberType-Saveable`() {
    runComposeUiTest {
      testStateScopedToBackstack_resetRoots(
        useKeys = true,
        useSwipe = true,
        rememberType = RememberType.Saveable,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Test
  fun `stateScopedToBackstack_resetRoots_useKeys-true_useSwipe-false_rememberType-Retained`() {
    runComposeUiTest {
      testStateScopedToBackstack_resetRoots(
        useKeys = true,
        useSwipe = false,
        rememberType = RememberType.Retained,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Test
  fun `stateScopedToBackstack_resetRoots_useKeys-true_useSwipe-false_rememberType-Saveable`() {
    runComposeUiTest {
      testStateScopedToBackstack_resetRoots(
        useKeys = true,
        useSwipe = false,
        rememberType = RememberType.Saveable,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Ignore
  @Test
  fun `stateScopedToBackstack_resetRoots_useKeys-false_useSwipe-true_rememberType-Retained`() {
    runComposeUiTest {
      testStateScopedToBackstack_resetRoots(
        useKeys = false,
        useSwipe = true,
        rememberType = RememberType.Retained,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Ignore
  @Test
  fun `stateScopedToBackstack_resetRoots_useKeys-false_useSwipe-true_rememberType-Saveable`() {
    runComposeUiTest {
      testStateScopedToBackstack_resetRoots(
        useKeys = false,
        useSwipe = true,
        rememberType = RememberType.Saveable,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Test
  fun `stateScopedToBackstack_resetRoots_useKeys-false_useSwipe-false_rememberType-Retained`() {
    runComposeUiTest {
      testStateScopedToBackstack_resetRoots(
        useKeys = false,
        useSwipe = false,
        rememberType = RememberType.Retained,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }

  @Test
  fun `stateScopedToBackstack_resetRoots_useKeys-false_useSwipe-false_rememberType-Saveable`() {
    runComposeUiTest {
      testStateScopedToBackstack_resetRoots(
        useKeys = false,
        useSwipe = false,
        rememberType = RememberType.Saveable,
        decoratorFactory = ::decoratorFactory,
        setContent = ::setContent,
      )
    }
  }
}

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.runtime.remember
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.internal.test.TestContentTags.TAG_COUNT
import com.slack.circuit.internal.test.TestContentTags.TAG_GO_NEXT
import com.slack.circuit.internal.test.TestContentTags.TAG_INCREASE_COUNT
import com.slack.circuit.internal.test.TestContentTags.TAG_LABEL
import com.slack.circuit.internal.test.TestContentTags.TAG_POP
import com.slack.circuit.internal.test.TestContentTags.TAG_RESET_ROOT_ALPHA
import com.slack.circuit.internal.test.TestContentTags.TAG_RESET_ROOT_BETA
import com.slack.circuit.internal.test.TestCountPresenter.RememberType
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.createTestCircuit
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class GestureNavigationStateTest {

  @Test
  fun `stateScopedToBackstack_useKeys-true_useSwipe-true_rememberType-Retained`() {
    runComposeUiTest {
      testStateScopedToBackstack(
        useKeys = true,
        useSwipe = true,
        rememberType = RememberType.Retained,
      )
    }
  }

  @Test
  fun `stateScopedToBackstack_useKeys-true_useSwipe-true_rememberType-Saveable`() {
    runComposeUiTest {
      testStateScopedToBackstack(
        useKeys = true,
        useSwipe = true,
        rememberType = RememberType.Saveable,
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
      )
    }
  }

  @Test
  fun `stateScopedToBackstack_useKeys-false_useSwipe-true_rememberType-Retained`() {
    runComposeUiTest {
      testStateScopedToBackstack(
        useKeys = false,
        useSwipe = true,
        rememberType = RememberType.Retained,
      )
    }
  }

  @Test
  fun `stateScopedToBackstack_useKeys-false_useSwipe-true_rememberType-Saveable`() {
    runComposeUiTest {
      testStateScopedToBackstack(
        useKeys = false,
        useSwipe = true,
        rememberType = RememberType.Saveable,
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
      )
    }
  }

  fun ComposeUiTest.testStateScopedToBackstack(
    useKeys: Boolean,
    useSwipe: Boolean,
    rememberType: RememberType,
  ) {
    val circuit = createTestCircuit(useKeys = useKeys, rememberType = rememberType)
    setContent {
      CircuitCompositionLocals(circuit) {
        val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
        val navigator =
          rememberCircuitNavigator(
            backStack = backStack,
            onRootPop = {}, // no-op for tests
          )
        NavigableCircuitContent(
          navigator = navigator,
          backStack = backStack,
          decoratorFactory =
            remember { IOSPredictiveBackNavDecorator.Factory(onBackInvoked = navigator::pop) },
        )
      }
    }

    // Current: Screen A. Increase count to 1
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("A")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("0")
    println("Increasing A")
    onTopNavigationRecordNodeWithTag(TAG_INCREASE_COUNT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("1")

    // Navigate to Screen B. Increase count to 1
    println("Going to B")
    onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("0")
    println("Increasing B")
    onTopNavigationRecordNodeWithTag(TAG_INCREASE_COUNT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("1")

    // Navigate to Screen C. Increase count to 1
    println("Going to C")
    onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("C")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("0")
    println("Increasing C")
    onTopNavigationRecordNodeWithTag(TAG_INCREASE_COUNT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("1")

    // Pop to Screen B. Increase count from 1 to 2.
    println("Pop to B")
    pop(useSwipe)
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("1")
    println("Increasing B")
    onTopNavigationRecordNodeWithTag(TAG_INCREASE_COUNT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("2")

    // Navigate to Screen C. Assert that it's state was not retained
    println("Go to C")
    onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("C")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("0")

    // Pop to Screen B. Assert that it's state was retained
    println("Popping to B")
    pop(useSwipe)
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("2")

    // Pop to Screen A. Assert that it's state was retained
    println("Popping to A")
    pop(useSwipe)
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("A")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("1")

    // Navigate to Screen B. Assert that it's state was not retained
    onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("0")
  }

  @Test
  fun `stateScopedToBackstack_resetRoots_useKeys-true_useSwipe-true_rememberType-Retained`() {
    runComposeUiTest {
      testStateScopedToBackstack_resetRoots(
        useKeys = true,
        useSwipe = true,
        rememberType = RememberType.Retained,
      )
    }
  }

  @Test
  fun `stateScopedToBackstack_resetRoots_useKeys-true_useSwipe-true_rememberType-Saveable`() {
    runComposeUiTest {
      testStateScopedToBackstack_resetRoots(
        useKeys = true,
        useSwipe = true,
        rememberType = RememberType.Saveable,
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
      )
    }
  }

  @Test
  fun `stateScopedToBackstack_resetRoots_useKeys-false_useSwipe-true_rememberType-Retained`() {
    runComposeUiTest {
      testStateScopedToBackstack_resetRoots(
        useKeys = false,
        useSwipe = true,
        rememberType = RememberType.Retained,
      )
    }
  }

  @Test
  fun `stateScopedToBackstack_resetRoots_useKeys-false_useSwipe-true_rememberType-Saveable`() {
    runComposeUiTest {
      testStateScopedToBackstack_resetRoots(
        useKeys = false,
        useSwipe = true,
        rememberType = RememberType.Saveable,
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
      )
    }
  }

  fun ComposeUiTest.testStateScopedToBackstack_resetRoots(
    useKeys: Boolean,
    useSwipe: Boolean,
    rememberType: RememberType,
  ) {
    val circuit =
      createTestCircuit(
        useKeys = useKeys,
        rememberType = rememberType,
        saveStateOnRootChange = true,
        restoreStateOnRootChange = true,
      )

    setContent {
      CircuitCompositionLocals(circuit) {
        val backStack = rememberSaveableBackStack(TestScreen.RootAlpha)
        val navigator =
          rememberCircuitNavigator(
            backStack = backStack,
            onRootPop = {}, // no-op for tests
          )
        NavigableCircuitContent(
          navigator = navigator,
          backStack = backStack,
          decoratorFactory =
            remember { IOSPredictiveBackNavDecorator.Factory(onBackInvoked = navigator::pop) },
        )
      }
    }

    // Current: Root Alpha. Navigate to Screen A
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("Root Alpha")
    onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()

    // Current: Screen A. Increase count to 1
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("A")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("0")
    println("Increasing A")
    onTopNavigationRecordNodeWithTag(TAG_INCREASE_COUNT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("1")

    // Navigate to Screen B. Increase count to 1
    println("Going to B")
    onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("0")
    println("Increasing B")
    onTopNavigationRecordNodeWithTag(TAG_INCREASE_COUNT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("1")

    // Navigate to Screen C. Increase count to 1
    println("Going to C")
    onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("C")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("0")
    println("Increasing C")
    onTopNavigationRecordNodeWithTag(TAG_INCREASE_COUNT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("1")

    // Pop to Screen B. Increase count from 1 to 2.
    println("Pop to B")
    pop(useSwipe)
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("1")
    println("Increasing B")
    onTopNavigationRecordNodeWithTag(TAG_INCREASE_COUNT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("2")

    // Navigate to Screen C. Assert that it's state was not retained
    println("Go to C")
    onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("C")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("0")

    // Pop to Screen B. Assert that it's state was retained
    println("Popping to B")
    pop(useSwipe)
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("2")

    // So at this point:
    // Active: Root Alpha, Screen A (count: 1), Screen B: (count: 2)
    // Retained: empty

    // Let's switch to Root B
    onTopNavigationRecordNodeWithTag(TAG_RESET_ROOT_BETA).performClick()
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("Root Beta")

    // Navigate to Screen A, and increase count to 2
    onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("A")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("0")
    onTopNavigationRecordNodeWithTag(TAG_INCREASE_COUNT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_INCREASE_COUNT).performClick()
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("2")

    // So at this point:
    // Active: Root Beta, Screen A (count: 2)
    // Retained: Root Alpha, Screen A (count: 1), Screen B: (count: 2)

    // Let's switch back to Root Alpha
    onTopNavigationRecordNodeWithTag(TAG_RESET_ROOT_ALPHA).performClick()
    // Root Alpha should now be active. The top record for Root Alpha is Screen B: (count: 2)
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("2")

    // Pop to Screen A
    pop(useSwipe)
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("A")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("1")

    // Let's switch back to Root B
    onTopNavigationRecordNodeWithTag(TAG_RESET_ROOT_BETA).performClick()
    // Root Beta should now be active. The top record for Root Beta  is Screen A: (count: 2)
    onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("A")
    onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("2")
  }

  private fun ComposeUiTest.pop(useSwipe: Boolean) {
    if (useSwipe) {
      swipeRight()
    } else {
      onTopNavigationRecordNodeWithTag(TAG_POP).performClick()
    }
  }
}

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.internal.test.TestContentTags.TAG_COUNT
import com.slack.circuit.internal.test.TestContentTags.TAG_GO_NEXT
import com.slack.circuit.internal.test.TestContentTags.TAG_INCREASE_COUNT
import com.slack.circuit.internal.test.TestContentTags.TAG_LABEL
import com.slack.circuit.internal.test.TestContentTags.TAG_POP
import com.slack.circuit.internal.test.TestContentTags.TAG_RESET_ROOT_ALPHA
import com.slack.circuit.internal.test.TestContentTags.TAG_RESET_ROOT_BETA
import com.slack.circuit.internal.test.TestCountPresenter
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.createTestCircuit
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(ComposeUiTestRunner::class)
class NavigableCircuitSaveableStateTest {

  @get:Rule val composeTestRule = createComposeRule()

  //  @Test fun saveableStateScopedToBackstackWithKeys() = saveableStateScopedToBackstack(true)
  //
  //  @Test fun saveableStateScopedToBackstackWithoutKeys() = saveableStateScopedToBackstack(false)
  //
  //  @Test
  //  fun saveableStateScopedToBackstackResetRootsWithKeys() =
  //    saveableStateScopedToBackstackResetRoots(true)
  //
  //  @Test
  //  fun saveableStateScopedToBackstackResetRootsWithoutKeys() =
  //    saveableStateScopedToBackstackResetRoots(false)

  private fun saveableStateScopedToBackstack(useKeys: Boolean) {
    composeTestRule.run {
      val circuit =
        createTestCircuit(
          useKeys = useKeys,
          rememberType = TestCountPresenter.RememberType.Saveable,
        )

      setContent {
        CircuitCompositionLocals(circuit) {
          val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
          val navigator =
            rememberCircuitNavigator(
              backStack = backStack,
              onRootPop = {}, // no-op for tests
            )
          NavigableCircuitContent(navigator = navigator, backStack = backStack)
        }
      }

      // Current: Screen A. Increase count to 1
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen B. Increase count to 1
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen C. Increase count to 1
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Pop to Screen B. Increase count from 1 to 2.
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // Navigate to Screen C. Assert that it's state was not saved
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")

      // Pop to Screen B. Assert that it's state was saved
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // Pop to Screen A. Assert that it's state was saved
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen B. Assert that it's state was not saved
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
    }
  }

  private fun saveableStateScopedToBackstackResetRoots(useKeys: Boolean) {
    composeTestRule.run {
      val circuit =
        createTestCircuit(
          useKeys = useKeys,
          rememberType = TestCountPresenter.RememberType.Saveable,
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
          NavigableCircuitContent(navigator = navigator, backStack = backStack)
        }
      }

      // Current: Root Alpha. Navigate to Screen A
      onNodeWithTag(TAG_LABEL).assertTextEquals("Root Alpha")
      onNodeWithTag(TAG_GO_NEXT).performClick()

      // Current: Screen A. Increase count to 1
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen B. Increase count to 1
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen C. Increase count to 1
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Pop to Screen B. Increase count from 1 to 2.
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // Navigate to Screen C. Assert that it's state was not retained
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")

      // Pop to Screen B. Assert that it's state was retained
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // So at this point:
      // Active: Root Alpha, Screen A (count: 1), Screen B: (count: 2)
      // Retained: empty

      // Let's switch to Root B
      onNodeWithTag(TAG_RESET_ROOT_BETA).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("Root Beta")

      // Navigate to Screen A, and increase count to 2
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // So at this point:
      // Active: Root Beta, Screen A (count: 2)
      // Retained: Root Alpha, Screen A (count: 1), Screen B: (count: 2)

      // Let's switch back to Root Alpha
      onNodeWithTag(TAG_RESET_ROOT_ALPHA).performClick()
      // Root Alpha should now be active. The top record for Root Alpha is Screen B: (count: 2)
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // Pop to Screen A
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Let's switch back to Root B
      onNodeWithTag(TAG_RESET_ROOT_BETA).performClick()
      // Root Beta should now be active. The top record for Root Beta  is Screen A: (count: 2)
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")
    }
  }

  @Test
  fun saveableStateClearRemovedRecordStates() {
    val circuit = createTestCircuit(rememberType = TestCountPresenter.RememberType.Saveable)
    val saveableStateRegistry = SaveableStateRegistry(emptyMap(), { true })
    composeTestRule.setContent {
      CircuitCompositionLocals(circuit) {
        val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
        val navigator = rememberCircuitNavigator(backStack = backStack)
        CompositionLocalProvider(LocalSaveableStateRegistry provides saveableStateRegistry) {
          NavigableCircuitContent(navigator = navigator, backStack = backStack)
        }
      }
    }

    fun areStructuresEqual(obj1: Any?, obj2: Any?): Boolean =
      when {
        obj1 == null && obj2 == null -> true
        obj1 == null || obj2 == null -> false
        obj1 is Map<*, *> && obj2 is Map<*, *> ->
          obj1.keys.size == obj2.keys.size &&
            obj1.entries.indices.all { i ->
              val value1 = obj1.entries.elementAt(i).value
              val value2 = obj2.entries.elementAt(i).value
              areStructuresEqual(value1, value2)
            }

        obj1 is List<*> && obj2 is List<*> ->
          obj1.size == obj2.size && obj1.indices.all { i -> areStructuresEqual(obj1[i], obj2[i]) }

        obj1::class != obj2::class -> false
        else -> true
      }

    composeTestRule.run {
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")

      // Navigate to Screen B
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      // Pop to Screen A
      onNodeWithTag(TAG_POP).performClick()

      val savedStates1 = saveableStateRegistry.performSave()

      // Navigate to Screen B
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      // Pop to Screen A
      onNodeWithTag(TAG_POP).performClick()

      val savedStates2 = saveableStateRegistry.performSave()
      assertTrue(areStructuresEqual(savedStates1, savedStates2))
    }
  }
}

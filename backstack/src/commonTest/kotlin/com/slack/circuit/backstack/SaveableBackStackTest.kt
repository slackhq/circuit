// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0

package com.slack.circuit.backstack

import androidx.compose.runtime.saveable.SaverScope
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.slack.circuit.internal.test.TestScreen
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaveableBackStackTest {

  @Test
  fun test_save_and_restore_backstack_state() {
    val backStack = SaveableBackStack(TestScreen.RootAlpha)
    backStack.push(TestScreen.ScreenA)
    backStack.push(TestScreen.ScreenB)

    assertThat(backStack.entryList).hasSize(3)
    assertThat(backStack.stateStore).isEmpty()

    // Now save state
    backStack.saveState()
    // Assert that state store contains the current back stack
    assertThat(backStack.stateStore).hasSize(1)
    backStack.stateStore[TestScreen.RootAlpha].let { saved ->
      assertThat(saved).isNotNull()
      saved!!
      assertThat(saved[0].screen).isEqualTo(TestScreen.ScreenB)
      assertThat(saved[1].screen).isEqualTo(TestScreen.ScreenA)
      assertThat(saved[2].screen).isEqualTo(TestScreen.RootAlpha)
    }

    // Now pop everything off and add RootB + Screen C
    backStack.popUntil { false }
    backStack.push(TestScreen.RootBeta)
    backStack.push(TestScreen.ScreenC)

    // Assert that the entry list contains the new items
    assertThat(backStack.entryList).hasSize(2)
    assertThat(backStack.entryList[0].screen).isEqualTo(TestScreen.ScreenC)
    assertThat(backStack.entryList[1].screen).isEqualTo(TestScreen.RootBeta)

    // Now pop everything off and restore RootA
    backStack.popUntil { false }
    backStack.restoreState(TestScreen.RootAlpha)

    // Assert that the RootA stack was restored
    assertThat(backStack.entryList).hasSize(3)
    assertThat(backStack.entryList[0].screen).isEqualTo(TestScreen.ScreenB)
    assertThat(backStack.entryList[1].screen).isEqualTo(TestScreen.ScreenA)
    assertThat(backStack.entryList[2].screen).isEqualTo(TestScreen.RootAlpha)
    // And that the state store is now empty
    assertThat(backStack.stateStore).isEmpty()
  }

  @Test
  fun test_saveable_save_and_restore() {
    val backStack = SaveableBackStack(TestScreen.RootAlpha)
    backStack.push(TestScreen.ScreenA)
    backStack.push(TestScreen.ScreenB)

    val saved = save(backStack)
    assertThat(saved).isNotNull()
    saved!!

    val restored = SaveableBackStack.Saver.restore(saved)
    assertThat(restored).isNotNull()
    restored!!

    assertThat(backStack.entryList.toList()).isEqualTo(restored.entryList.toList())
    assertThat(backStack.stateStore.toMap()).isEqualTo(restored.stateStore.toMap())
  }

  @Test
  fun test_saveable_save_and_restore_with_backstack_state() {
    val backStack = SaveableBackStack(TestScreen.RootAlpha)
    backStack.push(TestScreen.ScreenA)
    backStack.push(TestScreen.ScreenB)

    // Now save state and pop everything off
    backStack.saveState()
    // Now pop everything off and add RootB + Screen C
    backStack.popUntil { false }
    backStack.push(TestScreen.RootBeta)
    backStack.push(TestScreen.ScreenC)

    val saved = save(backStack)
    assertThat(saved).isNotNull()
    saved!!

    val restored = SaveableBackStack.Saver.restore(saved)
    assertThat(restored).isNotNull()
    restored!!

    assertThat(backStack.entryList.toList()).isEqualTo(restored.entryList.toList())
    assertThat(backStack.stateStore.toMap()).isEqualTo(restored.stateStore.toMap())
  }

  @Test
  fun test_guard_pushing_same_top_record() {
    val backStack = SaveableBackStack(TestScreen.RootAlpha)
    assertTrue(backStack.push(TestScreen.ScreenA))
    assertTrue(backStack.push(TestScreen.ScreenB))
    assertFalse(backStack.push(TestScreen.ScreenB))
  }

  @Test
  fun test_forward_incompatibility() {
    val backStack = SaveableBackStack(TestScreen.RootAlpha)
    backStack.push(TestScreen.ScreenA)
    backStack.push(TestScreen.ScreenB)
    assertFalse(backStack.forward())
  }

  @Test
  fun test_backward_incompatibility() {
    val backStack = SaveableBackStack(TestScreen.RootAlpha)
    backStack.push(TestScreen.ScreenA)
    backStack.push(TestScreen.ScreenB)
    assertFalse(backStack.backward())
  }
}

private fun save(backStack: SaveableBackStack) =
  with(SaveableBackStack.Saver) {
    val scope = SaverScope { true }
    scope.save(backStack)
  }

// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0

package com.slack.circuit.backstack

import androidx.compose.runtime.saveable.SaverScope
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.internal.test.TestScreen
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaveableNavStackTest {

  @Test
  fun test_basic_forward_and_backward_navigation() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)
    navStack.push(TestScreen.ScreenC)

    assertThat(navStack.historyList).hasSize(4)
    assertThat(navStack.size).isEqualTo(4)
    assertThat(navStack.canGoForward).isFalse()
    assertThat(navStack.topRecord?.screen).isEqualTo(TestScreen.ScreenC)

    // Navigate back
    val popped = navStack.pop()
    assertThat(popped?.screen).isEqualTo(TestScreen.ScreenB)
    assertThat(navStack.historyList).hasSize(4) // History list doesn't shrink
    assertThat(navStack.size).isEqualTo(3) // But visible size does
    assertThat(navStack.forwardSize).isEqualTo(1)
    assertThat(navStack.canGoForward).isTrue()
    assertThat(navStack.topRecord?.screen).isEqualTo(TestScreen.ScreenB)

    // Navigate back again
    navStack.pop()
    assertThat(navStack.size).isEqualTo(2)
    assertThat(navStack.forwardSize).isEqualTo(2)
    assertThat(navStack.topRecord?.screen).isEqualTo(TestScreen.ScreenA)

    // Navigate forward
    val forwarded = navStack.forward()
    assertThat(forwarded?.screen).isEqualTo(TestScreen.ScreenB)
    assertThat(navStack.size).isEqualTo(3)
    assertThat(navStack.forwardSize).isEqualTo(1)
    assertThat(navStack.topRecord?.screen).isEqualTo(TestScreen.ScreenB)

    // Navigate forward again
    navStack.forward()
    assertThat(navStack.size).isEqualTo(4)
    assertThat(navStack.forwardSize).isEqualTo(0)
    assertThat(navStack.canGoForward).isFalse()
    assertThat(navStack.topRecord?.screen).isEqualTo(TestScreen.ScreenC)
  }

  @Test
  fun test_forward_stack_cleared_on_push() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)

    // Navigate back to build up forward history
    navStack.pop()
    assertThat(navStack.forwardSize).isEqualTo(1)
    assertThat(navStack.canGoForward).isTrue()

    // Push a new screen - should clear forward history
    navStack.push(TestScreen.ScreenC)
    assertThat(navStack.forwardSize).isEqualTo(0)
    assertThat(navStack.canGoForward).isFalse()
    assertThat(navStack.topRecord?.screen).isEqualTo(TestScreen.ScreenC)
    // History list should only contain items up to the new push
    assertThat(navStack.historyList).hasSize(3)
  }

  @Test
  fun test_cannot_pop_root() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    assertThat(navStack.historyList).hasSize(1)
    assertThat(navStack.size).isEqualTo(1)

    // Try to pop the root - should return null
    val popped = navStack.pop()
    assertThat(popped).isNull()
    assertThat(navStack.historyList).hasSize(1)
    assertThat(navStack.size).isEqualTo(1)
    assertThat(navStack.forwardSize).isEqualTo(0)
  }

  @Test
  fun test_forward_returns_null_when_empty() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)

    assertThat(navStack.canGoForward).isFalse()
    val forwarded = navStack.forward()
    assertThat(forwarded).isNull()
  }

  @Test
  fun test_pop_with_clear_forward() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)

    assertThat(navStack.historyList).hasSize(3)
    assertThat(navStack.forwardSize).isEqualTo(0)

    // Use pop(clearForward = true) - should remove forward history
    val popped = navStack.pop(clearForward = true)
    assertThat(popped?.screen).isEqualTo(TestScreen.ScreenA)
    assertThat(navStack.historyList).hasSize(2)
    assertThat(navStack.size).isEqualTo(2)
    assertThat(navStack.forwardSize).isEqualTo(0)
    assertThat(navStack.canGoForward).isFalse()
  }

  @Test
  fun test_clear_forward() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)

    // Navigate back to build up forward history
    navStack.pop()
    navStack.pop()
    assertThat(navStack.forwardSize).isEqualTo(2)
    assertThat(navStack.canGoForward).isTrue()
    assertThat(navStack.historyList).hasSize(3)

    // Clear forward history
    navStack.clearForward()
    assertThat(navStack.forwardSize).isEqualTo(0)
    assertThat(navStack.canGoForward).isFalse()
    assertThat(navStack.historyList).hasSize(1)
  }

  @Test
  fun test_contains_record_includes_forward_history() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    val recordA = SaveableBackStack.Record(TestScreen.ScreenA)
    val recordB = SaveableBackStack.Record(TestScreen.ScreenB)

    navStack.push(recordA)
    navStack.push(recordB)

    // Both records should be in history list
    assertThat(navStack.containsRecord(recordA, includeSaved = false)).isTrue()
    assertThat(navStack.containsRecord(recordB, includeSaved = false)).isTrue()

    // Pop to move pointer back
    navStack.pop()

    // Both A and B should still be in history list
    assertThat(navStack.containsRecord(recordA, includeSaved = false)).isTrue()
    assertThat(navStack.containsRecord(recordB, includeSaved = false)).isTrue()
  }

  @Test
  fun test_is_record_reachable_includes_forward_history() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    val recordA = SaveableBackStack.Record(TestScreen.ScreenA)
    val recordB = SaveableBackStack.Record(TestScreen.ScreenB)

    navStack.push(recordA)
    navStack.push(recordB)

    // Pop to move pointer back
    navStack.pop()

    // Both keys should still be reachable
    assertThat(navStack.isRecordReachable(recordB.key, depth = 10, includeSaved = false)).isTrue()
    assertThat(navStack.isRecordReachable(recordA.key, depth = 10, includeSaved = false)).isTrue()
  }

  @Test
  fun test_save_and_restore_state() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)

    assertThat(navStack.historyList).hasSize(3)
    assertThat(navStack.stateStore).isEmpty()

    // Save state (saves both history and current index)
    navStack.saveState()
    assertThat(navStack.stateStore).hasSize(1)

    navStack.stateStore[TestScreen.RootAlpha].let { saved ->
      assertThat(saved).isNotNull()
      saved!!
      assertThat(saved.first).hasSize(3)
      assertThat(saved.first[0].screen).isEqualTo(TestScreen.RootAlpha)
      assertThat(saved.first[1].screen).isEqualTo(TestScreen.ScreenA)
      assertThat(saved.first[2].screen).isEqualTo(TestScreen.ScreenB)
      assertThat(saved.second).isEqualTo(2) // Current index
    }

    // Clear and add new navigation
    navStack.historyList.clear()
    navStack.push(TestScreen.RootBeta)
    navStack.push(TestScreen.ScreenC)

    assertThat(navStack.historyList).hasSize(2)

    // Restore state
    val restored = navStack.restoreState(TestScreen.RootAlpha)
    assertThat(restored).isTrue()

    assertThat(navStack.historyList).hasSize(3)
    assertThat(navStack.topRecord?.screen).isEqualTo(TestScreen.ScreenB)
    assertThat(navStack.stateStore).isEmpty()
  }

  @Test
  fun test_saveable_save_and_restore() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)

    // Navigate back to populate forward history
    navStack.pop()
    assertThat(navStack.size).isEqualTo(2)
    assertThat(navStack.forwardSize).isEqualTo(1)

    val saved = save(navStack)
    assertThat(saved).isNotNull()
    saved!!

    val restored = SaveableNavStack.Saver.restore(saved)
    assertThat(restored).isNotNull()
    restored!!

    assertThat(navStack.historyList.toList()).isEqualTo(restored.historyList.toList())
    assertThat(navStack.currentIndex).isEqualTo(restored.currentIndex)
    assertThat(navStack.stateStore.toMap()).isEqualTo(restored.stateStore.toMap())
  }

  @Test
  fun test_saveable_save_and_restore_with_state_store() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)

    // Save state and switch to different root
    navStack.saveState()
    navStack.historyList.clear()
    navStack.push(TestScreen.RootBeta)
    navStack.push(TestScreen.ScreenC)

    // Navigate back to populate forward history
    navStack.pop()

    val saved = save(navStack)
    assertThat(saved).isNotNull()
    saved!!

    val restored = SaveableNavStack.Saver.restore(saved)
    assertThat(restored).isNotNull()
    restored!!

    assertThat(navStack.historyList.toList()).isEqualTo(restored.historyList.toList())
    assertThat(navStack.currentIndex).isEqualTo(restored.currentIndex)
    assertThat(navStack.stateStore.toMap()).isEqualTo(restored.stateStore.toMap())
  }

  @Test
  fun test_guard_pushing_same_top_record() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    assertTrue(navStack.push(TestScreen.ScreenA))
    assertTrue(navStack.push(TestScreen.ScreenB))
    assertFalse(navStack.push(TestScreen.ScreenB))
  }

  @Test
  fun test_forward_size() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)
    navStack.push(TestScreen.ScreenC)

    assertThat(navStack.forwardSize).isEqualTo(0)

    navStack.pop()
    assertThat(navStack.forwardSize).isEqualTo(1)

    navStack.pop()
    assertThat(navStack.forwardSize).isEqualTo(2)

    navStack.forward()
    assertThat(navStack.forwardSize).isEqualTo(1)
  }
}

private fun save(navStack: SaveableNavStack) =
  with(SaveableNavStack.Saver) {
    val scope = SaverScope { true }
    scope.save(navStack)
  }

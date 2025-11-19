// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.backstack

import androidx.compose.runtime.saveable.SaverScope
import com.slack.circuit.internal.test.TestScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SaveableNavStackTest {

  @Test
  fun test_initial_state() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)

    assertEquals(1, navStack.size)
    assertEquals(TestScreen.RootAlpha, navStack.topRecord?.screen)
    assertEquals(TestScreen.RootAlpha, navStack.currentRecord?.screen)
    assertEquals(TestScreen.RootAlpha, navStack.rootRecord?.screen)
    assertFalse(navStack.isEmpty)
    assertTrue(navStack.isAtRoot)
    assertTrue(navStack.isAtTop)
    assertFalse(navStack.canGoBack)
    assertFalse(navStack.canGoForward)
  }

  @Test
  fun test_push_records() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    assertTrue(navStack.push(TestScreen.ScreenA))
    assertTrue(navStack.push(TestScreen.ScreenB))

    assertEquals(3, navStack.size)
    assertEquals(TestScreen.ScreenB, navStack.topRecord?.screen)
    assertEquals(TestScreen.ScreenB, navStack.currentRecord?.screen)
    assertEquals(TestScreen.RootAlpha, navStack.rootRecord?.screen)
    assertFalse(navStack.isAtRoot)
    assertTrue(navStack.isAtTop)
  }

  @Test
  fun test_move_backward_and_forward() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)
    navStack.push(TestScreen.ScreenC)

    // [C, B, A, RootAlpha] - current at C (index 0)
    assertEquals(TestScreen.ScreenC, navStack.currentRecord?.screen)
    assertTrue(navStack.canGoBack)
    assertFalse(navStack.canGoForward)

    // Move backward to B
    assertTrue(navStack.move(NavStack.Direction.Backward))
    assertEquals(TestScreen.ScreenB, navStack.currentRecord?.screen)
    assertTrue(navStack.canGoBack)
    assertTrue(navStack.canGoForward)

    // Move backward to A
    assertTrue(navStack.move(NavStack.Direction.Backward))
    assertEquals(TestScreen.ScreenA, navStack.currentRecord?.screen)
    assertTrue(navStack.canGoBack)
    assertTrue(navStack.canGoForward)

    // Move backward to Root
    assertTrue(navStack.move(NavStack.Direction.Backward))
    assertEquals(TestScreen.RootAlpha, navStack.currentRecord?.screen)
    assertTrue(navStack.isAtRoot)
    assertFalse(navStack.canGoBack)
    assertTrue(navStack.canGoForward)

    // Can't move backward from root
    assertFalse(navStack.move(NavStack.Direction.Backward))

    // Move forward to A
    assertTrue(navStack.move(NavStack.Direction.Forward))
    assertEquals(TestScreen.ScreenA, navStack.currentRecord?.screen)

    // Move forward to B
    assertTrue(navStack.move(NavStack.Direction.Forward))
    assertEquals(TestScreen.ScreenB, navStack.currentRecord?.screen)

    // Move forward to C
    assertTrue(navStack.move(NavStack.Direction.Forward))
    assertEquals(TestScreen.ScreenC, navStack.currentRecord?.screen)
    assertTrue(navStack.isAtTop)
    assertTrue(navStack.canGoBack)
    assertFalse(navStack.canGoForward)

    // Can't move forward from top
    assertFalse(navStack.move(NavStack.Direction.Forward))
  }

  @Test
  fun test_push_truncates_forward_history() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)
    navStack.push(TestScreen.ScreenC)

    // [C, B, A, RootAlpha] - current at C
    assertEquals(4, navStack.size)

    // Move back to B
    navStack.move(NavStack.Direction.Backward)
    assertEquals(TestScreen.ScreenB, navStack.currentRecord?.screen)

    // Add a new screen - should truncate C
    navStack.push(TestScreen.RootBeta)

    // [RootBeta, B, A, RootAlpha] - current at Beta
    assertEquals(4, navStack.size)
    assertEquals(TestScreen.RootBeta, navStack.topRecord?.screen)
    assertEquals(TestScreen.RootBeta, navStack.currentRecord?.screen)
    assertFalse(navStack.canGoForward)

    // Verify the stack contents
    val screens = navStack.map { it.screen }
    assertEquals(
      listOf(TestScreen.RootBeta, TestScreen.ScreenB, TestScreen.ScreenA, TestScreen.RootAlpha),
      screens
    )
  }

  @Test
  fun test_pop() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)
    navStack.push(TestScreen.ScreenC)

    // Move back to B
    navStack.move(NavStack.Direction.Backward)
    navStack.move(NavStack.Direction.Backward)
    assertEquals(TestScreen.ScreenA, navStack.currentRecord?.screen)

    // Remove (removes A, current stays at same index pointing to Root)
    val removed = navStack.pop()
    assertEquals(TestScreen.ScreenA, removed?.screen)
    assertEquals(TestScreen.RootAlpha, navStack.currentRecord?.screen)
    assertEquals(1, navStack.size)
  }

  @Test
  fun test_save_and_restore_state() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)

    // Move back to A
    navStack.move(NavStack.Direction.Backward)
    assertEquals(TestScreen.ScreenA, navStack.currentRecord?.screen)

    // Save state
    navStack.saveState()
    assertEquals(1, navStack.stateStore.size)

    val saved = navStack.stateStore[TestScreen.RootAlpha]
    assertNotNull(saved)
    assertEquals(3, saved.entries.size)
    assertEquals(1, saved.currentIndex) // Index of A in [B, A, Root]
    assertEquals(TestScreen.ScreenB, saved.entries[0].screen)
    assertEquals(TestScreen.ScreenA, saved.entries[1].screen)
    assertEquals(TestScreen.RootAlpha, saved.entries[2].screen)

    // Clear the nav stack
    while (navStack.size > 0) {
      navStack.pop()
    }
    assertEquals(0, navStack.size)

    // Add a different root
    navStack.push(TestScreen.RootBeta)
    navStack.push(TestScreen.ScreenC)
    assertEquals(TestScreen.ScreenC, navStack.currentRecord?.screen)

    // Clear again and restore the saved state
    while (navStack.size > 0) {
      navStack.pop()
    }
    assertTrue(navStack.restoreState(TestScreen.RootAlpha))

    // Verify restored state
    assertEquals(3, navStack.size)
    assertEquals(TestScreen.ScreenA, navStack.currentRecord?.screen)
    assertEquals(TestScreen.ScreenB, navStack.topRecord?.screen)
    assertEquals(TestScreen.RootAlpha, navStack.rootRecord?.screen)
    assertTrue(navStack.stateStore.isEmpty())
  }

  @Test
  fun test_saveable_save_and_restore() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)

    // Move back to A
    navStack.move(NavStack.Direction.Backward)

    val saved = save(navStack)
    assertNotNull(saved)

    val restored = SaveableNavStack.Saver.restore(saved)
    assertNotNull(restored)

    assertEquals(navStack.size, restored.size)
    assertEquals(navStack.currentRecord?.screen, restored.currentRecord?.screen)
    assertEquals(navStack.entryList.toList(), restored.entryList.toList())
    assertEquals(navStack.stateStore.toMap(), restored.stateStore.toMap())
  }

  @Test
  fun test_saveable_save_and_restore_with_saved_state() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)

    // Move back and save state
    navStack.move(NavStack.Direction.Backward)
    navStack.saveState()

    // Add different screens
    while (navStack.size > 0) {
      navStack.pop()
    }
    navStack.push(TestScreen.RootBeta)
    navStack.push(TestScreen.ScreenC)

    val saved = save(navStack)
    assertNotNull(saved)

    val restored = SaveableNavStack.Saver.restore(saved)
    assertNotNull(restored)

    assertEquals(navStack.size, restored.size)
    assertEquals(navStack.currentRecord?.screen, restored.currentRecord?.screen)
    assertEquals(navStack.entryList.toList(), restored.entryList.toList())
    assertEquals(navStack.stateStore.size, restored.stateStore.size)

    // Verify the saved state was preserved
    val restoredSaved = restored.stateStore[TestScreen.RootAlpha]
    assertNotNull(restoredSaved)
    assertEquals(3, restoredSaved.entries.size)
    assertEquals(1, restoredSaved.currentIndex)
  }

  @Test
  fun test_guard_pushing_same_top_record() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    assertTrue(navStack.push(TestScreen.ScreenA))
    assertTrue(navStack.push(TestScreen.ScreenB))
    assertFalse(navStack.push(TestScreen.ScreenB))
  }

  @Test
  fun test_iteration() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)
    navStack.push(TestScreen.ScreenC)

    val screens = navStack.map { it.screen }
    assertEquals(
      listOf(TestScreen.ScreenC, TestScreen.ScreenB, TestScreen.ScreenA, TestScreen.RootAlpha),
      screens
    )
  }

  @Test
  fun test_contains_record() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    val recordA = SaveableNavStack.Record(TestScreen.ScreenA)
    navStack.push(recordA)
    navStack.push(TestScreen.ScreenB)

    assertTrue(navStack.containsRecord(recordA, includeSaved = false))

    // Save state and clear
    navStack.saveState()
    while (navStack.size > 0) {
      navStack.pop()
    }

    assertFalse(navStack.containsRecord(recordA, includeSaved = false))
    assertTrue(navStack.containsRecord(recordA, includeSaved = true))
  }

  @Test
  fun test_is_record_reachable() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    val recordA = SaveableNavStack.Record(TestScreen.ScreenA)
    navStack.push(recordA)
    navStack.push(TestScreen.ScreenB)
    navStack.push(TestScreen.ScreenC)

    // Stack is [C, B, A, Root]
    assertTrue(navStack.isRecordReachable(recordA.key, depth = 10, includeSaved = false))
    assertFalse(navStack.isRecordReachable(recordA.key, depth = 1, includeSaved = false))
  }

  @Test
  fun test_snapshot_captures_current_state() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)
    navStack.push(TestScreen.ScreenC)

    // Move back to B
    navStack.move(NavStack.Direction.Backward)
    assertEquals(TestScreen.ScreenB, navStack.currentRecord?.screen)

    // Create snapshot
    val snapshot = navStack.snapshot()

    // Verify snapshot captures correct state
    assertEquals(4, snapshot.entries.size)
    assertEquals(1, snapshot.currentIndex) // Index of B in [C, B, A, Root]
    assertEquals(TestScreen.ScreenC, snapshot.entries[0].screen)
    assertEquals(TestScreen.ScreenB, snapshot.entries[1].screen)
    assertEquals(TestScreen.ScreenA, snapshot.entries[2].screen)
    assertEquals(TestScreen.RootAlpha, snapshot.entries[3].screen)
  }

  @Test
  fun test_snapshot_is_immutable() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)

    // Create snapshot
    val snapshot = navStack.snapshot()
    val originalSize = snapshot.entries.size
    val originalIndex = snapshot.currentIndex

    // Modify the nav stack
    navStack.push(TestScreen.ScreenC)
    navStack.move(NavStack.Direction.Backward)

    // Verify snapshot hasn't changed
    assertEquals(originalSize, snapshot.entries.size)
    assertEquals(originalIndex, snapshot.currentIndex)
    assertEquals(
      listOf(TestScreen.ScreenB, TestScreen.ScreenA, TestScreen.RootAlpha),
      snapshot.entries.map { it.screen }
    )
  }

  @Test
  fun test_snapshot_empty_stack() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)

    // Remove everything
    navStack.pop()

    val snapshot = navStack.snapshot()
    assertTrue(snapshot.entries.isEmpty())
    assertEquals(0, snapshot.currentIndex)
  }

  @Test
  fun test_snapshot_iteration() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)

    val snapshot = navStack.snapshot()

    // Test that snapshot is iterable
    val screens = snapshot.map { it.screen }
    assertEquals(
      listOf(TestScreen.ScreenB, TestScreen.ScreenA, TestScreen.RootAlpha),
      screens
    )
  }

  @Test
  fun test_snapshot_reflects_navigation_state() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)
    navStack.push(TestScreen.ScreenC)

    // At top
    val snapshot1 = navStack.snapshot()
    assertEquals(0, snapshot1.currentIndex)

    // Move to root
    navStack.move(NavStack.Direction.Backward)
    navStack.move(NavStack.Direction.Backward)
    navStack.move(NavStack.Direction.Backward)
    val snapshot2 = navStack.snapshot()
    assertEquals(3, snapshot2.currentIndex)

    // Move to middle
    navStack.move(NavStack.Direction.Forward)
    val snapshot3 = navStack.snapshot()
    assertEquals(2, snapshot3.currentIndex)
  }

  @Test
  fun test_saved_state_snapshot_is_iterable() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)

    navStack.saveState()
    val snapshot = navStack.stateStore[TestScreen.RootAlpha]
    assertNotNull(snapshot)

    val screens = snapshot.map { it.screen }
    assertEquals(
      listOf(TestScreen.ScreenB, TestScreen.ScreenA, TestScreen.RootAlpha),
      screens
    )
  }

  @Test
  fun test_pop_at_boundaries() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)

    // At top, remove forward
    val removed = navStack.pop()
    assertEquals(TestScreen.ScreenA, removed?.screen)
    assertEquals(TestScreen.RootAlpha, navStack.currentRecord?.screen)

    // At root after removal
    val removed2 = navStack.pop()
    assertEquals(TestScreen.RootAlpha, removed2?.screen)
    assertEquals(0, navStack.size)
  }
}

private fun save(navStack: SaveableNavStack) =
  with(SaveableNavStack.Saver) {
    val scope = SaverScope { true }
    scope.save(navStack)
  }

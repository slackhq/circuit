// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.navstack

import androidx.compose.runtime.saveable.SaverScope
import com.slack.circuit.foundation.navstack.SaveableNavStack.Record
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.DefaultCircuitSaver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveableNavStackTest {

  @Test
  fun test_save_and_restore_navstack_state() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)

    assertEquals(3, navStack.entryList.size)
    assertTrue(navStack.stateStore.isEmpty())

    // Now save state
    navStack.saveState()
    // Assert that state store contains the current nav stack
    assertEquals(1, navStack.stateStore.size)
    navStack.stateStore[TestScreen.RootAlpha].let { saved ->
      assertNotNull(saved)
      assertEquals(0, saved.currentIndex)
      assertEquals(TestScreen.ScreenB, saved.entries[0].screen)
      assertEquals(TestScreen.ScreenA, saved.entries[1].screen)
      assertEquals(TestScreen.RootAlpha, saved.entries[2].screen)
    }

    // Now pop everything and add RootB + Screen C
    navStack.popUntil { false }
    navStack.push(TestScreen.RootBeta)
    navStack.push(TestScreen.ScreenC)

    // Assert that the entry list contains the new items
    assertEquals(2, navStack.entryList.size)
    assertEquals(TestScreen.ScreenC, navStack.entryList[0].screen)
    assertEquals(TestScreen.RootBeta, navStack.entryList[1].screen)

    // Now pop everything off and restore RootA
    navStack.popUntil { false }
    navStack.restoreState(TestScreen.RootAlpha)

    // Assert that the RootA stack was restored
    assertEquals(3, navStack.entryList.size)
    assertEquals(TestScreen.ScreenB, navStack.entryList[0].screen)
    assertEquals(TestScreen.ScreenA, navStack.entryList[1].screen)
    assertEquals(TestScreen.RootAlpha, navStack.entryList[2].screen)
    // And that the state store is now empty
    assertTrue(navStack.stateStore.isEmpty())
  }

  @Test
  fun test_saveable_save_and_restore() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)
    navStack.backward()

    val saved = save(navStack)
    assertNotNull(saved)

    val restored = SaveableNavStack.Saver(DefaultCircuitSaver).restore(saved)
    assertNotNull(restored)

    assertEquals(navStack.entryList.toList(), restored.entryList.toList())
    assertEquals(navStack.stateStore.toMap(), restored.stateStore.toMap())
  }

  @Test
  fun test_saveable_save_and_restore_with_navstack_state() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)

    // Now save state and pop everything off
    navStack.saveState()
    // Now pop everything off and add RootB + Screen C
    while (navStack.pop() != null) {
      // pop all
    }
    navStack.push(TestScreen.RootBeta)
    navStack.push(TestScreen.ScreenC)

    val saved = save(navStack)
    assertNotNull(saved)

    val restored = SaveableNavStack.Saver(DefaultCircuitSaver).restore(saved)
    assertNotNull(restored)

    assertEquals(navStack.entryList.toList(), restored.entryList.toList())
    assertEquals(navStack.stateStore.toMap(), restored.stateStore.toMap())
  }

  @Test
  fun test_saveable_restore_with_noop_saver_returns_null() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    val navStackList =
      SaveableNavStack.SaveableNavStackList(
        entries = navStack.entryList.toList(),
        currentIndex = 0,
      )

    val saved = save(navStack, CircuitSaver.NoOp)
    assertNotNull(saved)
    assertNull(SaveableNavStack.Saver(CircuitSaver.NoOp).restore(saved))

    val savedList = save(navStackList, CircuitSaver.NoOp)
    assertNotNull(savedList)
    assertNull(SaveableNavStack.SaveableNavStackList.Saver(CircuitSaver.NoOp).restore(savedList))
  }

  @Test
  fun test_saveable_restore_keeps_active_record_when_topward_record_drops() {
    assertRestoreCurrentAfterDropping(
      dropped = TestScreen.ScreenC,
      expectedCurrent = TestScreen.ScreenB,
    )
  }

  @Test
  fun test_saveable_restore_keeps_active_record_when_rootward_record_drops() {
    assertRestoreCurrentAfterDropping(
      dropped = TestScreen.ScreenA,
      expectedCurrent = TestScreen.ScreenB,
    )
  }

  @Test
  fun test_saveable_restore_selects_nearest_rootward_record_when_active_drops() {
    assertRestoreCurrentAfterDropping(
      dropped = TestScreen.ScreenB,
      expectedCurrent = TestScreen.ScreenA,
    )
  }

  @Test
  fun test_saveable_restore_selects_nearest_topward_record_when_active_root_drops() {
    val circuitSaver = droppingSaver(TestScreen.RootAlpha)
    val navStack = navStackWithCurrent(TestScreen.RootAlpha)
    val navStackList =
      SaveableNavStack.SaveableNavStackList(
        entries = navStack.entryList.toList(),
        currentIndex = navStack.entryList.lastIndex,
      )

    val restored =
      SaveableNavStack.Saver(circuitSaver).restore(assertNotNull(save(navStack, circuitSaver)))
    assertNotNull(restored)
    assertEquals(TestScreen.ScreenA, restored.currentRecord?.screen)

    val restoredList =
      SaveableNavStack.SaveableNavStackList.Saver(circuitSaver)
        .restore(assertNotNull(save(navStackList, circuitSaver)))
    assertNull(restoredList)
  }

  @Test
  fun test_saveable_restore_discards_snapshot_when_its_root_drops() {
    val circuitSaver = droppingSaver(TestScreen.RootAlpha)
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.saveState()
    navStack.popUntil { false }
    navStack.push(TestScreen.RootBeta)
    navStack.push(TestScreen.ScreenC)

    val restored =
      SaveableNavStack.Saver(circuitSaver).restore(assertNotNull(save(navStack, circuitSaver)))
    assertNotNull(restored)
    assertEquals(
      listOf(TestScreen.ScreenC, TestScreen.RootBeta),
      restored.entryList.map { it.screen },
    )
    assertTrue(restored.stateStore.isEmpty())
  }

  @Test
  fun test_guard_pushing_same_current_record() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    assertTrue(navStack.push(TestScreen.ScreenA))
    assertTrue(navStack.push(TestScreen.ScreenB))
    assertFalse(navStack.push(TestScreen.ScreenB))
  }

  @Test
  fun test_forward_and_backward_navigation() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)
    navStack.push(TestScreen.ScreenC)

    // At the top (ScreenC), currentIndex = 0
    // entryList = [ScreenC, ScreenB, ScreenA, RootAlpha]
    assertEquals(TestScreen.ScreenC, navStack.currentRecord?.screen)
    assertEquals(TestScreen.ScreenC, navStack.topRecord?.screen)

    // Go backward (increase currentIndex to 1)
    assertTrue(navStack.backward())
    assertEquals(TestScreen.ScreenB, navStack.currentRecord?.screen)
    assertEquals(TestScreen.ScreenC, navStack.topRecord?.screen) // top stays the same

    // Go backward again (increase currentIndex to 2)
    assertTrue(navStack.backward())
    assertEquals(TestScreen.ScreenA, navStack.currentRecord?.screen)

    // Go backward again (increase currentIndex to 3)
    assertTrue(navStack.backward())
    assertEquals(TestScreen.RootAlpha, navStack.currentRecord?.screen)

    // Can't go backward anymore
    assertFalse(navStack.backward())
    assertEquals(TestScreen.RootAlpha, navStack.currentRecord?.screen)

    // Go forward (decrease currentIndex to 2)
    assertTrue(navStack.forward())
    assertEquals(TestScreen.ScreenA, navStack.currentRecord?.screen)

    // Go forward again (decrease currentIndex to 1)
    assertTrue(navStack.forward())
    assertEquals(TestScreen.ScreenB, navStack.currentRecord?.screen)

    // Go forward again (decrease currentIndex to 0)
    assertTrue(navStack.forward())
    assertEquals(TestScreen.ScreenC, navStack.currentRecord?.screen)

    // Can't go forward anymore
    assertFalse(navStack.forward())
    assertEquals(TestScreen.ScreenC, navStack.currentRecord?.screen)
  }

  @Test
  fun test_push_truncates_forward_history() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)
    navStack.push(TestScreen.ScreenC)

    // Go back three times to RootAlpha
    navStack.backward()
    navStack.backward()
    navStack.backward()
    assertEquals(TestScreen.RootAlpha, navStack.currentRecord?.screen)

    // Push a new screen (ScreenA again), should truncate forward history (old ScreenA, ScreenB,
    // ScreenC)
    navStack.push(TestScreen.ScreenA)
    assertEquals(2, navStack.entryList.size)
    assertEquals(TestScreen.ScreenA, navStack.entryList[0].screen)
    assertEquals(TestScreen.RootAlpha, navStack.entryList[1].screen)

    // Can't go forward anymore
    assertFalse(navStack.forward())
  }

  @Test
  fun test_pop_truncates_forward_history() {
    val navStack = SaveableNavStack(TestScreen.RootAlpha)
    navStack.push(TestScreen.ScreenA)
    navStack.push(TestScreen.ScreenB)
    navStack.push(TestScreen.ScreenC)

    // Go back three times to RootAlpha
    navStack.backward()
    navStack.backward()
    navStack.backward()
    assertEquals(TestScreen.RootAlpha, navStack.currentRecord?.screen)

    // Pop should truncate forward history and remove current
    val popped = navStack.pop()
    assertEquals(TestScreen.RootAlpha, popped?.screen)
    assertTrue(navStack.entryList.isEmpty())

    // Can't go forward or backward
    assertFalse(navStack.forward())
    assertFalse(navStack.backward())
  }

  @Test
  fun isRecordReachable_depth_zero_matches_current() {
    val root = Record(TestScreen.RootAlpha, key = "root")
    val a = Record(TestScreen.ScreenA, key = "a")
    val navStack = SaveableNavStack(root)
    navStack.push(a)
    // entryList = [a(0), root(1)], currentIndex = 0
    assertTrue(navStack.isRecordReachable("a", depth = 0, includeSaved = false))
    assertFalse(navStack.isRecordReachable("root", depth = 0, includeSaved = false))
  }

  @Test
  fun isRecordReachable_negative_depth_returns_false() {
    val navStack = SaveableNavStack(Record(TestScreen.RootAlpha, key = "root"))
    assertFalse(navStack.isRecordReachable("root", depth = -1, includeSaved = false))
  }

  @Test
  fun isRecordReachable_from_top() {
    val root = Record(TestScreen.RootAlpha, key = "root")
    val a = Record(TestScreen.ScreenA, key = "a")
    val b = Record(TestScreen.ScreenB, key = "b")
    val c = Record(TestScreen.ScreenC, key = "c")
    val navStack = SaveableNavStack(root)
    navStack.push(a)
    navStack.push(b)
    navStack.push(c)
    // entryList = [c(0), b(1), a(2), root(3)], currentIndex = 0

    // depth=1: current (c) + 1 backward (b)
    assertTrue(navStack.isRecordReachable("c", depth = 1, includeSaved = false))
    assertTrue(navStack.isRecordReachable("b", depth = 1, includeSaved = false))
    assertFalse(navStack.isRecordReachable("a", depth = 1, includeSaved = false))

    // depth=2: current (c) + 2 backward (b, a)
    assertTrue(navStack.isRecordReachable("a", depth = 2, includeSaved = false))
    assertFalse(navStack.isRecordReachable("root", depth = 2, includeSaved = false))
  }

  @Test
  fun isRecordReachable_from_middle() {
    val root = Record(TestScreen.RootAlpha, key = "root")
    val a = Record(TestScreen.ScreenA, key = "a")
    val b = Record(TestScreen.ScreenB, key = "b")
    val c = Record(TestScreen.ScreenC, key = "c")
    val navStack = SaveableNavStack(root)
    navStack.push(a)
    navStack.push(b)
    navStack.push(c)
    // Move to b (currentIndex = 1)
    navStack.backward()
    // entryList = [c(0), b(1), a(2), root(3)], currentIndex = 1

    // depth=1: 1 forward (c), current (b), 1 backward (a)
    assertTrue(navStack.isRecordReachable("c", depth = 1, includeSaved = false))
    assertTrue(navStack.isRecordReachable("b", depth = 1, includeSaved = false))
    assertTrue(navStack.isRecordReachable("a", depth = 1, includeSaved = false))
    assertFalse(navStack.isRecordReachable("root", depth = 1, includeSaved = false))

    // depth=2: 2 forward (c), current (b), 2 backward (a, root)
    assertTrue(navStack.isRecordReachable("root", depth = 2, includeSaved = false))
  }

  @Test
  fun isRecordReachable_from_root() {
    val root = Record(TestScreen.RootAlpha, key = "root")
    val a = Record(TestScreen.ScreenA, key = "a")
    val navStack = SaveableNavStack(root)
    navStack.push(a)
    // Move to root (currentIndex = 1)
    navStack.backward()
    // entryList = [a(0), root(1)], currentIndex = 1

    // depth=1: forward [0]=a, current [1]=root
    assertTrue(navStack.isRecordReachable("a", depth = 1, includeSaved = false))
    assertTrue(navStack.isRecordReachable("root", depth = 1, includeSaved = false))
  }

  @Test
  fun isRecordReachable_saved_state() {
    val root = Record(TestScreen.RootAlpha, key = "root")
    val a = Record(TestScreen.ScreenA, key = "a")
    val navStack = SaveableNavStack(root)
    navStack.push(a)
    // entryList = [a(0), root(1)], currentIndex = 0

    navStack.saveState()
    navStack.popUntil { false }
    navStack.push(Record(TestScreen.RootBeta, key = "rootBeta"))
    // entryList = [rootBeta(0)], currentIndex = 0
    // stateStore has [a(0), root(1)] with currentIndex=0

    // "a" not in current stack
    assertFalse(navStack.isRecordReachable("a", depth = 0, includeSaved = false))
    // "a" is the current record in the saved snapshot, reachable at depth=0
    assertTrue(navStack.isRecordReachable("a", depth = 0, includeSaved = true))
    // "root" is 1 backward from current in saved snapshot, needs depth=1
    assertFalse(navStack.isRecordReachable("root", depth = 0, includeSaved = true))
    assertTrue(navStack.isRecordReachable("root", depth = 1, includeSaved = true))
  }
}

private fun save(navStack: SaveableNavStack, circuitSaver: CircuitSaver = DefaultCircuitSaver) =
  with(SaveableNavStack.Saver(circuitSaver)) {
    val scope = SaverScope { true }
    scope.save(navStack)
  }

private fun save(
  navStackList: SaveableNavStack.SaveableNavStackList,
  circuitSaver: CircuitSaver = DefaultCircuitSaver,
) =
  with(SaveableNavStack.SaveableNavStackList.Saver(circuitSaver)) {
    val scope = SaverScope { true }
    scope.save(navStackList)
  }

private fun navStackWithCurrent(current: TestScreen): SaveableNavStack =
  SaveableNavStack(TestScreen.RootAlpha).apply {
    push(TestScreen.ScreenA)
    push(TestScreen.ScreenB)
    push(TestScreen.ScreenC)
    while (currentRecord?.screen != current) {
      assertTrue(backward())
    }
  }

private fun assertRestoreCurrentAfterDropping(
  dropped: TestScreen,
  expectedCurrent: TestScreen,
) {
  val circuitSaver = droppingSaver(dropped)
  val navStack = navStackWithCurrent(TestScreen.ScreenB)
  val navStackList =
    SaveableNavStack.SaveableNavStackList(
      entries = navStack.entryList.toList(),
      currentIndex = 1,
    )

  val restored =
    SaveableNavStack.Saver(circuitSaver).restore(assertNotNull(save(navStack, circuitSaver)))
  assertNotNull(restored)
  assertEquals(expectedCurrent, restored.currentRecord?.screen)

  val restoredList =
    SaveableNavStack.SaveableNavStackList.Saver(circuitSaver)
      .restore(assertNotNull(save(navStackList, circuitSaver)))
  assertNotNull(restoredList)
  assertEquals(expectedCurrent, restoredList.active.screen)
}

private fun droppingSaver(vararg dropped: CircuitSaveable): CircuitSaver =
  object : CircuitSaver() {
    override fun save(value: CircuitSaveable): Any? = value.takeUnless { it in dropped }

    override fun restore(saved: Any): CircuitSaveable? = saved as? CircuitSaveable
  }

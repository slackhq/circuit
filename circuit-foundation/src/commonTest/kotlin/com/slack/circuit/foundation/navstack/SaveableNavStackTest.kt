// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.navstack

import androidx.compose.runtime.saveable.SaverScope
import com.slack.circuit.internal.test.TestScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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

    val restored = SaveableNavStack.Saver.restore(saved)
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

    val restored = SaveableNavStack.Saver.restore(saved)
    assertNotNull(restored)

    assertEquals(navStack.entryList.toList(), restored.entryList.toList())
    assertEquals(navStack.stateStore.toMap(), restored.stateStore.toMap())
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
}

private fun save(navStack: SaveableNavStack) =
  with(SaveableNavStack.Saver) {
    val scope = SaverScope { true }
    scope.save(navStack)
  }

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
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.DefaultCircuitSaver
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

    val restored = SaveableBackStack.Saver(DefaultCircuitSaver).restore(saved)
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

    val restored = SaveableBackStack.Saver(DefaultCircuitSaver).restore(saved)
    assertThat(restored).isNotNull()
    restored!!

    assertThat(backStack.entryList.toList()).isEqualTo(restored.entryList.toList())
    assertThat(backStack.stateStore.toMap()).isEqualTo(restored.stateStore.toMap())
  }

  @Test
  fun test_saveable_restore_with_noop_saver_returns_null() {
    val backStack = SaveableBackStack(TestScreen.RootAlpha)
    backStack.push(TestScreen.ScreenA)

    val saved = save(backStack, CircuitSaver.NoOp)
    assertThat(saved).isNotNull()
    saved!!

    assertNull(SaveableBackStack.Saver(CircuitSaver.NoOp).restore(saved))
  }

  @Test
  fun test_saveable_restore_drops_unrestorable_records() {
    // Drops one screen on save, the rest of the stack should restore without it.
    val droppingSaver = droppingSaver(TestScreen.ScreenA)
    val backStack = SaveableBackStack(TestScreen.RootAlpha)
    backStack.push(TestScreen.ScreenA)
    backStack.push(TestScreen.ScreenB)

    val saved = save(backStack, droppingSaver)
    assertThat(saved).isNotNull()
    saved!!

    val restored = SaveableBackStack.Saver(droppingSaver).restore(saved)
    assertThat(restored).isNotNull()
    restored!!
    assertThat(restored.entryList.map { it.screen })
      .isEqualTo(listOf(TestScreen.ScreenB, TestScreen.RootAlpha))
  }

  @Test
  fun test_saveable_restore_allows_current_stack_root_to_drop() {
    val circuitSaver = droppingSaver(TestScreen.RootAlpha)
    val backStack = SaveableBackStack(TestScreen.RootAlpha)
    backStack.push(TestScreen.ScreenA)
    backStack.push(TestScreen.ScreenB)

    val saved = save(backStack, circuitSaver)
    assertThat(saved).isNotNull()
    val restored = SaveableBackStack.Saver(circuitSaver).restore(saved!!)
    assertThat(restored).isNotNull()
    restored!!
    assertThat(restored.entryList.map { it.screen })
      .isEqualTo(listOf(TestScreen.ScreenB, TestScreen.ScreenA))
  }

  @Test
  fun test_saveable_restore_discards_snapshot_when_its_root_drops() {
    val circuitSaver = droppingSaver(TestScreen.RootAlpha)
    val backStack = SaveableBackStack(TestScreen.RootAlpha)
    backStack.push(TestScreen.ScreenA)
    backStack.saveState()
    backStack.popUntil { false }
    backStack.push(TestScreen.RootBeta)
    backStack.push(TestScreen.ScreenC)

    val saved = save(backStack, circuitSaver)
    assertThat(saved).isNotNull()
    val restored = SaveableBackStack.Saver(circuitSaver).restore(saved!!)
    assertThat(restored).isNotNull()
    restored!!
    assertThat(restored.entryList.map { it.screen })
      .isEqualTo(listOf(TestScreen.ScreenC, TestScreen.RootBeta))
    assertThat(restored.stateStore).isEmpty()
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

private fun save(
  backStack: SaveableBackStack,
  circuitSaver: CircuitSaver = DefaultCircuitSaver,
) =
  with(SaveableBackStack.Saver(circuitSaver)) {
    val scope = SaverScope { true }
    scope.save(backStack)
  }

private fun droppingSaver(vararg dropped: CircuitSaveable): CircuitSaver =
  object : CircuitSaver() {
    override fun save(value: CircuitSaveable): Any? = value.takeUnless { it in dropped }

    override fun restore(saved: Any): CircuitSaveable? = saved as? CircuitSaveable
  }

// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.screen

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompositeCircuitSaverTest {
  @Test
  fun saveUsesFirstClaimingSaver() {
    val calls = mutableListOf<String>()
    val first =
      TestSaver(
        canSave = {
          calls += "first canSave"
          false
        },
        save = {
          calls += "first save"
          "first"
        },
      )
    val second =
      TestSaver(
        canSave = {
          calls += "second canSave"
          true
        },
        save = {
          calls += "second save"
          "second"
        },
      )
    val third =
      TestSaver(
        canSave = {
          calls += "third canSave"
          true
        }
      )

    assertEquals("second", (first + second + third).save(TestScreen))
    assertEquals(listOf("first canSave", "second canSave", "second save"), calls)
  }

  @Test
  fun nullSaveResultIsTerminal() {
    var fallbackCalled = false
    val first = TestSaver(save = { null })
    val fallback = TestSaver(save = { fallbackCalled = true })

    assertNull((first + fallback).save(TestScreen))
    assertFalse(fallbackCalled)
  }

  @Test
  fun saveFailureIsTerminal() {
    var fallbackCalled = false
    val first = TestSaver(save = { error("save failed") })
    val fallback = TestSaver(save = { fallbackCalled = true })

    val failure = assertFailsWith<IllegalStateException> { (first + fallback).save(TestScreen) }
    assertEquals("save failed", failure.message)
    assertFalse(fallbackCalled)
  }

  @Test
  fun saveFailsWhenNoSaverClaimsValue() {
    val composite = UnclaimedSaver() + UnclaimedSaver()

    val failure = assertFailsWith<IllegalArgumentException> { composite.save(TestScreen) }
    assertContains(failure.message.orEmpty(), "append CircuitSaver.NoOp")
    assertContains(failure.message.orEmpty(), "CircuitSaver.Dropping { ... }")
  }

  @Test
  fun restoreUsesFirstClaimingSaver() {
    val calls = mutableListOf<String>()
    val first =
      TestSaver(
        canRestore = {
          calls += "first canRestore"
          false
        },
        restore = {
          calls += "first restore"
          TestScreen
        },
      )
    val second =
      TestSaver(
        canRestore = {
          calls += "second canRestore"
          true
        },
        restore = {
          calls += "second restore"
          TestScreen
        },
      )
    val third =
      TestSaver(
        canRestore = {
          calls += "third canRestore"
          true
        }
      )

    assertEquals(TestScreen, (first + second + third).restoreScreen<TestScreen>("saved"))
    assertEquals(listOf("first canRestore", "second canRestore", "second restore"), calls)
  }

  @Test
  fun nullRestoreResultIsTerminal() {
    var fallbackCalled = false
    val first = TestSaver(restore = { null })
    val fallback =
      TestSaver(
        restore = {
          fallbackCalled = true
          TestScreen
        }
      )

    assertNull((first + fallback).restoreScreen<TestScreen>("saved"))
    assertFalse(fallbackCalled)
  }

  @Test
  fun restoreFailureIsTerminal() {
    var fallbackCalled = false
    val first = TestSaver(restore = { error("restore failed") })
    val fallback =
      TestSaver(
        restore = {
          fallbackCalled = true
          TestScreen
        }
      )

    val failure =
      assertFailsWith<IllegalStateException> {
        (first + fallback).restoreScreen<TestScreen>("saved")
      }
    assertEquals("restore failed", failure.message)
    assertFalse(fallbackCalled)
  }

  @Test
  fun restoreReturnsNullWhenNoSaverClaimsValue() {
    var absentCalled = false
    val composite = UnclaimedSaver() + UnclaimedSaver()

    assertNull(composite.restoreScreen<TestScreen>("saved", onAbsent = { absentCalled = true }))
    assertTrue(absentCalled)
  }

  @Test
  fun chainedCompositesPreserveOrder() {
    val calls = mutableListOf<String>()
    fun saver(name: String, claims: Boolean) =
      TestSaver(
        canSave = {
          calls += name
          claims
        },
        save = { name },
      )

    val composite =
      (saver("first", false) + saver("second", false)) +
        (saver("third", false) + saver("fourth", true))

    assertEquals("fourth", composite.save(TestScreen))
    assertEquals(listOf("first", "second", "third", "fourth"), calls)
  }

  @Test
  fun noOpClaimsValuesAndStopsTheChain() {
    var fallbackCalled = false
    val fallback =
      TestSaver(
        save = {
          fallbackCalled = true
          it
        },
        restore = {
          fallbackCalled = true
          TestScreen
        },
      )
    val composite = CircuitSaver.NoOp + fallback

    assertNull(composite.save(TestScreen))
    assertNull(composite.restoreScreen<TestScreen>("saved"))
    assertFalse(fallbackCalled)
  }

  @Test
  fun droppingReportsDroppedValuesAndStopsTheChain() {
    val dropped = mutableListOf<CircuitSaveable>()
    var fallbackCalled = false
    val fallback =
      TestSaver(
        save = {
          fallbackCalled = true
          it
        }
      )
    val composite = CircuitSaver.Dropping(dropped::add) + fallback

    assertNull(composite.save(TestScreen))
    assertEquals(listOf<CircuitSaveable>(TestScreen), dropped)
    assertNull(composite.restoreScreen<TestScreen>("saved"))
    assertFalse(fallbackCalled)
  }

  private class TestSaver(
    private val canSave: (CircuitSaveable) -> Boolean = { true },
    private val canRestore: (Any) -> Boolean = { true },
    private val save: (CircuitSaveable) -> Any? = { it },
    private val restore: (Any) -> CircuitSaveable? = { it as? CircuitSaveable },
  ) : CircuitSaver() {
    override fun save(value: CircuitSaveable): Any? = save.invoke(value)

    protected override fun canSave(value: CircuitSaveable): Boolean = canSave.invoke(value)

    protected override fun canRestore(saved: Any): Boolean = canRestore.invoke(saved)

    protected override fun restore(saved: Any): CircuitSaveable? = restore.invoke(saved)
  }

  private class UnclaimedSaver : CircuitSaver() {
    override fun save(value: CircuitSaveable): Any? = error("Saver should not be called.")

    protected override fun restore(saved: Any): CircuitSaveable? =
      error("Saver should not be called.")
  }

  private data object TestScreen : Screen
}

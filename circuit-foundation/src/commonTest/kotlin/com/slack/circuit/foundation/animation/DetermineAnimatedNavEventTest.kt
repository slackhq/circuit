// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.animation

import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.navigation.navStackListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(InternalCircuitApi::class)
class DetermineAnimatedNavEventTest {

  @Test
  fun `different root is a root reset`() {
    val initial = navStackListOf(activeItem = "A")
    val target = navStackListOf(activeItem = "B")
    assertEquals(AnimatedNavEvent.RootReset, determineAnimatedNavEvent(initial, target))
  }

  @Test
  fun `different root is a root reset even when the active item is unchanged`() {
    val initial = navStackListOf(activeItem = "A", backwardItems = listOf("R1"))
    val target = navStackListOf(activeItem = "A", backwardItems = listOf("R2"))
    assertEquals(AnimatedNavEvent.RootReset, determineAnimatedNavEvent(initial, target))
  }

  @Test
  fun `unchanged active item with different forward history is not animated`() {
    val initial = navStackListOf(activeItem = "A")
    val target = navStackListOf(forwardItems = listOf("F"), activeItem = "A")
    assertNull(determineAnimatedNavEvent(initial, target))
  }

  @Test
  fun `going to a new item is a go to`() {
    val initial = navStackListOf(activeItem = "A", backwardItems = listOf("R"))
    val target = navStackListOf(activeItem = "B", backwardItems = listOf("A", "R"))
    assertEquals(AnimatedNavEvent.GoTo, determineAnimatedNavEvent(initial, target))
  }

  @Test
  fun `removing the active item is a pop`() {
    val initial = navStackListOf(activeItem = "A", backwardItems = listOf("R"))
    val target = navStackListOf(activeItem = "R")
    assertEquals(AnimatedNavEvent.Pop, determineAnimatedNavEvent(initial, target))
  }

  @Test
  fun `moving the active item into forward history is backward`() {
    val initial = navStackListOf(activeItem = "A", backwardItems = listOf("R"))
    val target = navStackListOf(forwardItems = listOf("A"), activeItem = "R")
    assertEquals(AnimatedNavEvent.Backward, determineAnimatedNavEvent(initial, target))
  }

  @Test
  fun `taking the next item out of forward history is forward`() {
    val initial = navStackListOf(forwardItems = listOf("A"), activeItem = "R")
    val target = navStackListOf(activeItem = "A", backwardItems = listOf("R"))
    assertEquals(AnimatedNavEvent.Forward, determineAnimatedNavEvent(initial, target))
  }

  @Test
  fun `going to an item that stays in forward history is a go to`() {
    val initial = navStackListOf(forwardItems = listOf("A"), activeItem = "R")
    val target =
      navStackListOf(forwardItems = listOf("A"), activeItem = "A", backwardItems = listOf("R"))
    assertEquals(AnimatedNavEvent.GoTo, determineAnimatedNavEvent(initial, target))
  }
}

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.test.FakeNavigator.GoToEvent
import com.slack.circuit.test.FakeNavigator.ResetRootEvent
import kotlinx.coroutines.test.runTest
import kotlinx.parcelize.Parcelize
import org.junit.Test

class FakeNavigatorTest {
  @Test
  fun `resetRoot - ensure resetRoot returns old screens in proper order`() = runTest {
    val navigator = FakeNavigator(TestScreen1)
    navigator.goTo(TestScreen2)
    assertThat(navigator.peek()).isEqualTo(TestScreen2)
    assertThat(navigator.peekBackStack()).isEqualTo(listOf(TestScreen2, TestScreen1))

    val oldScreens = navigator.resetRoot(TestScreen3)

    assertThat(oldScreens).isEqualTo(listOf(TestScreen2, TestScreen1))
    assertThat(navigator.awaitResetRoot()).isEqualTo(ResetRootEvent(TestScreen3, oldScreens))
    assertThat(navigator.peek()).isEqualTo(TestScreen3)
    assertThat(navigator.peekBackStack()).isEqualTo(listOf(TestScreen3))
  }

  @Test
  fun resetRoot() = runTest {
    val navigator = FakeNavigator(TestScreen1)

    val oldScreens = navigator.resetRoot(TestScreen1)

    assertThat(oldScreens).containsExactly(TestScreen1)
    assertThat(navigator.awaitResetRoot()).isEqualTo(ResetRootEvent(TestScreen1, oldScreens))
  }

  @Test
  fun goTo() = runTest {
    val navigator = FakeNavigator(TestScreen1)
    // Go to a second screen
    assertThat(navigator.goTo(TestScreen2)).isTrue()
    assertThat(navigator.awaitNextScreen()).isEqualTo(TestScreen2)
    // Going to the same screen won't navigate
    assertThat(navigator.goTo(TestScreen2)).isFalse()
    assertThat(navigator.awaitNextGoTo()).isEqualTo(GoToEvent(TestScreen2, success = false))
    // Go to a third screen
    assertThat(navigator.goTo(TestScreen3)).isTrue()
    assertThat(navigator.awaitNextScreen()).isEqualTo(TestScreen3)
  }
}

@Parcelize private data object TestScreen1 : Screen

@Parcelize private data object TestScreen2 : Screen

@Parcelize private data object TestScreen3 : Screen

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.runtime.Screen
import kotlinx.coroutines.test.runTest
import kotlinx.parcelize.Parcelize
import org.junit.Test

class FakeNavigatorTest {
  @Test
  fun `resetRoot - ensure resetRoot returns old screens in proper order`() = runTest {
    val navigator = FakeNavigator()
    navigator.goTo(TestScreen1)
    navigator.goTo(TestScreen2)

    val oldScreens = navigator.resetRoot(TestScreen3)

    assertThat(oldScreens).isEqualTo(listOf(TestScreen2, TestScreen1))
    assertThat(navigator.awaitResetRoot()).isEqualTo(TestScreen3)
  }

  @Test
  fun resetRoot() = runTest {
    val navigator = FakeNavigator()

    val oldScreens = navigator.resetRoot(TestScreen1)

    assertThat(oldScreens).isEmpty()
    assertThat(navigator.awaitResetRoot()).isEqualTo(TestScreen1)
  }
}

@Parcelize private data object TestScreen1 : Screen

@Parcelize private data object TestScreen2 : Screen

@Parcelize private data object TestScreen3 : Screen

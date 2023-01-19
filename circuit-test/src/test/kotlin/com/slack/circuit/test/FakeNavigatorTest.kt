// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.NavigableScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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

private object TestScreen1 : NavigableScreen {
  override val route: String
    get() = "test1"

  override fun describeContents(): Int {
    throw NotImplementedError()
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    throw NotImplementedError()
  }
}

private object TestScreen2 : NavigableScreen by TestScreen1 {
  override val route: String
    get() = "test2"
}

private object TestScreen3 : NavigableScreen by TestScreen1 {
  override val route: String
    get() = "test3"
}

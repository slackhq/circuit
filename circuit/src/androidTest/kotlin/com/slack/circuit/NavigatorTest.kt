// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.backstack.SaveableBackStack
import kotlin.test.assertFailsWith
import kotlin.test.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
    get() = "test2"
}

@RunWith(RobolectricTestRunner::class)
class NavigatorTest {
  @Test
  fun errorWhenBackstackIsEmpty() {
    val backstack = SaveableBackStack()
    val t = assertFailsWith<IllegalStateException> { NavigatorImpl(backstack) {} }
    assertThat(t).hasMessageThat().contains("Backstack size must not be empty.")
  }

  @Test
  fun popAtRoot() {
    val backstack = SaveableBackStack()
    backstack.push(TestScreen1)
    backstack.push(TestScreen1)

    var onRootPop = 0
    val navigator = NavigatorImpl(backstack) { onRootPop++ }

    assertThat(backstack).hasSize(2)
    assertThat(onRootPop).isEqualTo(0)

    navigator.pop()
    assertThat(backstack).hasSize(1)
    assertThat(onRootPop).isEqualTo(0)

    navigator.pop()
    assertThat(backstack).hasSize(1)
    assertThat(onRootPop).isEqualTo(1)
  }

  @Test
  fun resetRoot() {
    val backStack = SaveableBackStack()
    backStack.push(TestScreen1)
    backStack.push(TestScreen2)

    val navigator = NavigatorImpl(backStack) { fail() }

    assertThat(backStack).hasSize(2)

    val oldScreens = navigator.resetRoot(TestScreen3)

    assertThat(backStack).hasSize(1)
    assertThat(backStack.topRecord?.screen).isEqualTo(TestScreen3)
    assertThat(oldScreens).hasSize(2)
    assertThat(oldScreens).isEqualTo(listOf(TestScreen2, TestScreen1))
  }
}

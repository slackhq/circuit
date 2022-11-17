// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.backstack.SaveableBackStack
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private object TestScreen : Screen {
  override fun describeContents(): Int {
    throw NotImplementedError()
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    throw NotImplementedError()
  }
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
    backstack.push(TestScreen)
    backstack.push(TestScreen)

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
}

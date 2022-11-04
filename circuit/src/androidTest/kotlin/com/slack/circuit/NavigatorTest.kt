/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    val t = assertFailsWith<IllegalStateException> {
      NavigatorImpl(backstack) { }
    }
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

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
import java.util.concurrent.atomic.AtomicInteger
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
  fun rootPopWhenEmpty() {
    val rootPops = AtomicInteger(0)
    val backstack = SaveableBackStack()
    val navigator =
      NavigatorImpl(
        backstack = backstack,
        popRootOnEmpty = true,
        onRootPop = { rootPops.incrementAndGet() }
      )
    assertThat(backstack).hasSize(0)
    navigator.pop()
    assertThat(rootPops.get()).isEqualTo(1)
  }

  @Test
  fun rootPopWhenOne() {
    val rootPops = AtomicInteger(0)
    val backstack = SaveableBackStack()
    backstack.push(TestScreen)
    backstack.push(TestScreen)
    val navigator =
      NavigatorImpl(
        backstack = backstack,
        popRootOnEmpty = true,
        onRootPop = { rootPops.incrementAndGet() }
      )
    assertThat(backstack).hasSize(2)
    navigator.pop()
    assertThat(rootPops.get()).isEqualTo(0)
    assertThat(backstack).hasSize(1)
    navigator.pop()
    assertThat(rootPops.get()).isEqualTo(1)
    assertThat(backstack).hasSize(0)
  }

  @Test
  fun rootPopWhenOneNoPopOnempty() {
    val rootPops = AtomicInteger(0)
    val backstack = SaveableBackStack()
    backstack.push(TestScreen)
    backstack.push(TestScreen)
    val navigator =
      NavigatorImpl(
        backstack = backstack,
        popRootOnEmpty = false,
        onRootPop = { rootPops.incrementAndGet() }
      )
    assertThat(backstack).hasSize(2)
    navigator.pop()
    assertThat(rootPops.get()).isEqualTo(0)
    assertThat(backstack).hasSize(1)
    navigator.pop()
    assertThat(rootPops.get()).isEqualTo(0)
    assertThat(backstack).hasSize(0)
    navigator.pop()
    assertThat(rootPops.get()).isEqualTo(1)
    assertThat(backstack).hasSize(0)
  }
}

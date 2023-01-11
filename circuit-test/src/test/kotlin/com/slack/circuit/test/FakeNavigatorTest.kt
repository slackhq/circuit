package com.slack.circuit.test

import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.Screen
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
    assertThat(navigator.awaitReset()).isEqualTo(TestScreen3)
  }

  @Test
  fun resetRoot() = runTest {
    val navigator = FakeNavigator()

    val oldScreens = navigator.resetRoot(TestScreen1)

    assertThat(oldScreens).isEmpty()
    assertThat(navigator.awaitReset()).isEqualTo(TestScreen1)
  }
}

private object TestScreen1 : Screen {
  override fun describeContents(): Int {
    throw NotImplementedError()
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    throw NotImplementedError()
  }
}
private object TestScreen2 : Screen by TestScreen1
private object TestScreen3 : Screen by TestScreen2

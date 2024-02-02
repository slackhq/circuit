/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit.retained.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateHolder
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.retained.rememberRetainedStateHolder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO adapt for retained more
@RunWith(AndroidJUnit4::class)
class RetainedStateHolderTest {

  @get:Rule val rule = createAndroidComposeRule<Activity>()

  private val restorationTester = StateRestorationTester(rule)

  @Test
  fun stateIsRestoredWhenGoBackToScreen1() {
    var increment = 0
    var screen by mutableStateOf(Screens.Screen1)
    var numberOnScreen1 = -1
    var restorableNumberOnScreen1 = -1
    restorationTester.setContent {
      val holder = rememberRetainedStateHolder()
      holder.RetainedStateProvider(screen) {
        if (screen == Screens.Screen1) {
          numberOnScreen1 = remember { increment++ }
          restorableNumberOnScreen1 = rememberRetained { increment++ }
        } else {
          // screen 2
          remember { 100 }
        }
      }
    }

    rule.runOnIdle {
      assertThat(numberOnScreen1).isEqualTo(0)
      assertThat(restorableNumberOnScreen1).isEqualTo(1)
      screen = Screens.Screen2
    }

    // wait for the screen switch to apply
    rule.runOnIdle {
      numberOnScreen1 = -1
      restorableNumberOnScreen1 = -1
      // switch back to screen1
      screen = Screens.Screen1
    }

    rule.runOnIdle {
      assertThat(numberOnScreen1).isEqualTo(2)
      assertThat(restorableNumberOnScreen1).isEqualTo(1)
    }
  }

  @Test
  fun simpleRestoreOnlyOneScreen() {
    var increment = 0
    var number = -1
    var restorableNumber = -1
    restorationTester.setContent {
      val holder = rememberRetainedStateHolder()
      holder.RetainedStateProvider(Screens.Screen1) {
        number = remember { increment++ }
        restorableNumber = rememberRetained { increment++ }
      }
    }

    rule.runOnIdle {
      assertThat(number).isEqualTo(0)
      assertThat(restorableNumber).isEqualTo(1)
      number = -1
      restorableNumber = -1
    }

    restorationTester.emulateSavedInstanceStateRestore()

    rule.runOnIdle {
      assertThat(number).isEqualTo(2)
      assertThat(restorableNumber).isEqualTo(1)
    }
  }

  @Test
  fun switchToScreen2AndRestore() {
    var increment = 0
    var screen by mutableStateOf(Screens.Screen1)
    var numberOnScreen2 = -1
    var restorableNumberOnScreen2 = -1
    restorationTester.setContent {
      val holder = rememberRetainedStateHolder()
      holder.RetainedStateProvider(screen) {
        if (screen == Screens.Screen2) {
          numberOnScreen2 = remember { increment++ }
          restorableNumberOnScreen2 = rememberRetained { increment++ }
        }
      }
    }

    rule.runOnIdle { screen = Screens.Screen2 }

    // wait for the screen switch to apply
    rule.runOnIdle {
      assertThat(numberOnScreen2).isEqualTo(0)
      assertThat(restorableNumberOnScreen2).isEqualTo(1)
      numberOnScreen2 = -1
      restorableNumberOnScreen2 = -1
    }

    restorationTester.emulateSavedInstanceStateRestore()

    rule.runOnIdle {
      assertThat(numberOnScreen2).isEqualTo(2)
      assertThat(restorableNumberOnScreen2).isEqualTo(1)
    }
  }

  @Test
  fun stateOfScreen1IsSavedAndRestoredWhileWeAreOnScreen2() {
    var increment = 0
    var screen by mutableStateOf(Screens.Screen1)
    var numberOnScreen1 = -1
    var restorableNumberOnScreen1 = -1
    restorationTester.setContent {
      val holder = rememberRetainedStateHolder()
      holder.RetainedStateProvider(screen) {
        if (screen == Screens.Screen1) {
          numberOnScreen1 = remember { increment++ }
          restorableNumberOnScreen1 = rememberRetained { increment++ }
        } else {
          // screen 2
          remember { 100 }
        }
      }
    }

    rule.runOnIdle {
      assertThat(numberOnScreen1).isEqualTo(0)
      assertThat(restorableNumberOnScreen1).isEqualTo(1)
      screen = Screens.Screen2
    }

    // wait for the screen switch to apply
    rule.runOnIdle {
      numberOnScreen1 = -1
      restorableNumberOnScreen1 = -1
    }

    restorationTester.emulateSavedInstanceStateRestore()

    // switch back to screen1
    rule.runOnIdle { screen = Screens.Screen1 }

    rule.runOnIdle {
      assertThat(numberOnScreen1).isEqualTo(2)
      assertThat(restorableNumberOnScreen1).isEqualTo(1)
    }
  }

  @Test
  fun weCanSkipSavingForCurrentScreen() {
    var increment = 0
    var screen by mutableStateOf(Screens.Screen1)
    var restorableStateHolder: RetainedStateHolder? = null
    var restorableNumberOnScreen1 = -1
    restorationTester.setContent {
      val holder = rememberRetainedStateHolder()
      restorableStateHolder = holder
      holder.RetainedStateProvider(screen) {
        if (screen == Screens.Screen1) {
          restorableNumberOnScreen1 = rememberRetained { increment++ }
        } else {
          // screen 2
          remember { 100 }
        }
      }
    }

    rule.runOnIdle {
      assertThat(restorableNumberOnScreen1).isEqualTo(0)
      restorableNumberOnScreen1 = -1
      restorableStateHolder!!.removeState(Screens.Screen1)
      screen = Screens.Screen2
    }

    rule.runOnIdle {
      // switch back to screen1
      screen = Screens.Screen1
    }

    rule.runOnIdle { assertThat(restorableNumberOnScreen1).isEqualTo(1) }
  }

  @Test
  fun weCanRemoveAlreadySavedState() {
    var increment = 0
    var screen by mutableStateOf(Screens.Screen1)
    var restorableStateHolder: RetainedStateHolder? = null
    var restorableNumberOnScreen1 = -1
    restorationTester.setContent {
      val holder = rememberRetainedStateHolder()
      restorableStateHolder = holder
      holder.RetainedStateProvider(screen) {
        if (screen == Screens.Screen1) {
          restorableNumberOnScreen1 = rememberRetained { increment++ }
        } else {
          // screen 2
          remember { 100 }
        }
      }
    }

    rule.runOnIdle {
      assertThat(restorableNumberOnScreen1).isEqualTo(0)
      restorableNumberOnScreen1 = -1
      screen = Screens.Screen2
    }

    rule.runOnIdle {
      // switch back to screen1
      restorableStateHolder!!.removeState(Screens.Screen1)
      screen = Screens.Screen1
    }

    rule.runOnIdle { assertThat(restorableNumberOnScreen1).isEqualTo(1) }
  }

  @Test
  fun restoringStateOfThePreviousPageAfterCreatingBundle() {
    var showFirstPage by mutableStateOf(true)
    var firstPageState: MutableState<Int>? = null

    rule.setContent {
      val holder = rememberRetainedStateHolder()
      holder.RetainedStateProvider(showFirstPage) {
        if (showFirstPage) {
          firstPageState = rememberRetained { mutableStateOf(0) }
        }
      }
    }

    rule.runOnIdle {
      assertThat(firstPageState!!.value).isEqualTo(0)
      // change the value, so we can assert this change will be restored
      firstPageState!!.value = 1
      firstPageState = null
      showFirstPage = false
    }

    rule.runOnIdle {
      rule.activity.doFakeSave()
      showFirstPage = true
    }

    rule.runOnIdle { assertThat(firstPageState!!.value).isEqualTo(1) }
  }

  @Test
  fun saveNothingWhenNoRememberRetainedIsUsedInternally() {
    var showFirstPage by mutableStateOf(true)
    val registry = RetainedStateRegistry(emptyMap())

    rule.setContent {
      CompositionLocalProvider(LocalRetainedStateRegistry provides registry) {
        val holder = rememberRetainedStateHolder()
        holder.RetainedStateProvider(showFirstPage) {}
      }
    }

    rule.runOnIdle { showFirstPage = false }

    rule.runOnIdle {
      val savedData = registry.saveAll()
      assertThat(savedData).isEqualTo(emptyMap<String, List<Any?>>())
    }
  }

  class Activity : ComponentActivity() {
    fun doFakeSave() {
      onSaveInstanceState(Bundle())
    }
  }
}

enum class Screens {
  Screen1,
  Screen2,
}

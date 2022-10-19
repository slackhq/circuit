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
package com.circuit.retained.android

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import com.slack.circuit.retained.Continuity
import com.slack.circuit.retained.LocalRetainedStateRegistryOwner
import com.slack.circuit.retained.continuityRetainedStateRegistry
import com.slack.circuit.retained.rememberRetained
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

private const val TAG_REMEMBER = "remember"
private const val TAG_RETAINED_1 = "retained1"
private const val TAG_RETAINED_2 = "retained2"

class RetainedTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val scenario: ActivityScenario<ComponentActivity>
    get() = composeTestRule.activityRule.scenario

  private val vmFactory =
    object : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST") return Continuity() as T
      }
    }

  // TODO
  //  - tests without using keys, but doesn't appear to work right in tests
  //  - tests with multiple retained state vars

  @Test
  fun singleWithKey() {
    setActivityContent { KeyContent("retainedText") }
    composeTestRule.onNodeWithTag(TAG_REMEMBER).performTextInput("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained")
    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
    // Restart the activity
    scenario.recreate()
    // Compose our content
    setActivityContent { KeyContent("retainedText") }
    // Was the text saved
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
  }

  @Test
  fun noRegistryBehavesLikeRegularRemember() {
    scenario.onActivity { it.setContent { KeyContent("retainedText") } }
    composeTestRule.onNodeWithTag(TAG_REMEMBER).performTextInput("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained")
    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
    // Restart the activity
    scenario.recreate()
    // Compose our content
    scenario.onActivity { it.setContent { KeyContent("retainedText") } }
    // Was the text saved
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("")
  }

  @Ignore("This doesn't appear to work from tests")
  @Test
  fun singleWithNoKey() {
    setActivityContent { KeyContent(null) }
    composeTestRule.onNodeWithTag(TAG_REMEMBER).performTextInput("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained")
    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
    // Restart the activity
    scenario.recreate()
    // Compose our content
    setActivityContent { KeyContent(null) }
    // Was the text saved
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
  }

  @Test
  fun multipleKeys() {
    setActivityContent { MultipleRetains() }
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).performTextInput("Text_Retained2")
    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertTextContains("Text_Retained2")
    // Restart the activity
    scenario.recreate()
    // Compose our content
    setActivityContent { MultipleRetains() }
    // Was the text saved
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertTextContains("Text_Retained2")
  }

  private fun setActivityContent(content: @Composable () -> Unit) {
    scenario.onActivity { activity ->
      activity.setContent {
        CompositionLocalProvider(
          LocalRetainedStateRegistryOwner provides continuityRetainedStateRegistry(vmFactory),
        ) {
          content()
        }
      }
    }
  }
}

@Composable
private fun KeyContent(key: String?) {
  var text1 by remember { mutableStateOf("") }
  // By default rememberSavable uses it's line number as its key, this doesn't seem
  // to work when testing, instead pass a key
  var retainedText: String by rememberRetained(key = key) { mutableStateOf("") }
  Column {
    TextField(
      modifier = Modifier.testTag(TAG_REMEMBER),
      value = text1,
      onValueChange = { text1 = it },
      label = {}
    )
    TextField(
      modifier = Modifier.testTag(TAG_RETAINED_1),
      value = retainedText,
      onValueChange = { retainedText = it },
      label = {}
    )
  }
}

@Composable
private fun MultipleRetains() {
  // By default rememberSavable uses it's line number as its key, this doesn't seem
  // to work when testing, instead pass a key
  var retainedText1: String by rememberRetained(key = "retained1") { mutableStateOf("") }
  var retainedText2: String by rememberRetained(key = "retained2") { mutableStateOf("") }
  Column {
    TextField(
      modifier = Modifier.testTag(TAG_RETAINED_1),
      value = retainedText1,
      onValueChange = { retainedText1 = it },
      label = {}
    )
    TextField(
      modifier = Modifier.testTag(TAG_RETAINED_2),
      value = retainedText2,
      onValueChange = { retainedText2 = it },
      label = {}
    )
  }
}

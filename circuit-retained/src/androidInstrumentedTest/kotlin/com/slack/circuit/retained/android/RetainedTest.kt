// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained.android

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.retained.CanRetainChecker
import com.slack.circuit.retained.LifecycleRetainedStateRegistry
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistryViewModel
import com.slack.circuit.retained.ViewModelRetainedStateRegistryFactory
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.retained.rememberRetainedStateHolder
import com.slack.circuit.retained.viewModelRetainedStateRegistry
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import leakcanary.DetectLeaksAfterTestSuccess.Companion.detectLeaksAfterTestSuccessWrapping
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

internal const val TAG_REMEMBER = "remember"
internal const val TAG_RETAINED_1 = "retained1"
internal const val TAG_RETAINED_2 = "retained2"
internal const val TAG_RETAINED_3 = "retained3"
internal const val TAG_BUTTON_SHOW = "btn_show"
internal const val TAG_BUTTON_HIDE = "btn_hide"
internal const val TAG_BUTTON_INC = "btn_inc"

class RetainedTest {
  private val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val rule =
    RuleChain.emptyRuleChain().detectLeaksAfterTestSuccessWrapping(tag = "ActivitiesDestroyed") {
      around(composeTestRule)
    }

  private val scenario: ActivityScenario<ComponentActivity>
    get() = composeTestRule.activityRule.scenario

  private class RecordingContinuityVmFactory :
    ViewModelRetainedStateRegistryFactory<RetainedStateRegistryViewModel> {
    var continuity: RetainedStateRegistryViewModel? = null

    override fun create(
      modelClass: KClass<RetainedStateRegistryViewModel>,
      extras: CreationExtras?,
    ): RetainedStateRegistryViewModel {
      return RetainedStateRegistryViewModel().also { continuity = it }
    }
  }

  private val vmFactory = RecordingContinuityVmFactory()

  @Test
  fun singleWithKey() {
    val content = @Composable { KeyContent("retainedText") }
    setActivityContent(content)
    composeTestRule.onNodeWithTag(TAG_REMEMBER).performTextInput("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained")
    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
    // Restart the activity
    scenario.recreate()
    // Compose our content
    setActivityContent(content)
    // Was the text saved
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
  }

  @Test
  fun clearingAfterDone() {
    setActivityContent {
      Column {
        var text2Enabled by rememberRetained { mutableStateOf(true) }
        val text1 by rememberRetained { mutableStateOf("Text") }

        Text(modifier = Modifier.testTag(TAG_RETAINED_1), text = text1)
        Button(
          modifier = Modifier.testTag("TAG_BUTTON"),
          onClick = { text2Enabled = !text2Enabled },
        ) {
          Text("Toggle")
        }
        if (text2Enabled) {
          val text2 by rememberRetained { mutableStateOf("Text") }
          Text(modifier = Modifier.testTag(TAG_RETAINED_2), text = text2)
        }
      }
    }

    // Hold on to our Continuity instance
    val continuity = vmFactory.continuity!!

    // We now have three groups with three retained values
    // - text2Enabled
    // - text1
    // - text2
    assertThat(continuity.peekProviders()).hasSize(3)
    assertThat(continuity.peekProviders().values.sumOf { it.size }).isEqualTo(3)

    // Now disable the second text
    composeTestRule.onNodeWithTag("TAG_BUTTON").performClick()
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertDoesNotExist()

    // text2 is now gone, so we only have two providers left now
    // - text2Enabled
    // - text1
    assertThat(continuity.peekProviders()).hasSize(2)
    assertThat(continuity.peekProviders().values.flatten()).hasSize(2)

    // Recreate the activity
    scenario.recreate()

    // After recreation, our VM now has committed our pending values.
    // - text2Enabled
    // - text1
    assertThat(continuity.peekRetained()).hasSize(2)
    assertThat(continuity.peekRetained().values.flatten()).hasSize(2)

    // Set different compose content what wouldn't reuse the previous ones
    setActivityContent {
      Row {
        val text1 by rememberRetained { mutableStateOf("Text") }
        Text(modifier = Modifier.testTag(TAG_RETAINED_1), text = text1)
      }
    }

    // Assert the GC step ran to remove unclaimed values. Need to wait for idle first
    composeTestRule.waitForIdle()
    assertThat(continuity.peekRetained()).hasSize(0)

    // We now just have one list with one provider
    // - text1
    assertThat(continuity.peekProviders()).hasSize(1)
    assertThat(continuity.peekProviders().values.single()).hasSize(1)

    // Destroy the activity, which should clear the retained values entirely
    scenario.moveToState(Lifecycle.State.DESTROYED)
    assertThat(continuity.peekRetained()).hasSize(0)
    assertThat(continuity.peekProviders()).hasSize(0)
  }

  @Test
  fun noRegistryBehavesLikeRegularRemember() {
    val content = @Composable { KeyContent("retainedText") }
    scenario.onActivity { it.setContent(content = content) }
    composeTestRule.onNodeWithTag(TAG_REMEMBER).performTextInput("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained")
    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
    // Restart the activity
    scenario.recreate()
    // Compose our content
    scenario.onActivity { it.setContent(content = content) }
    // Was the text saved
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("")
  }

  @Test
  fun singleWithNoKey() {
    val content = @Composable { KeyContent(null) }
    setActivityContent(content)
    composeTestRule.onNodeWithTag(TAG_REMEMBER).performTextInput("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained")
    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
    // Restart the activity
    scenario.recreate()
    // Compose our content
    setActivityContent(content)
    // Was the text saved
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
  }

  @Test fun multipleKeys() = testMultipleRetainedContent { MultipleRetains(useKeys = true) }

  @Test fun multipleNoKeys() = testMultipleRetainedContent { MultipleRetains(useKeys = false) }

  @Test fun nestedRegistries() = testMultipleRetainedContent { NestedRetains(useKeys = true) }

  @Test
  fun nestedRegistriesNoKeys() = testMultipleRetainedContent { NestedRetains(useKeys = false) }

  @Test fun nestedRegistriesWithPopAndPushWithKeys() = nestedRegistriesWithPopAndPush(true)

  @Test fun nestedRegistriesWithPopAndPushNoKeys() = nestedRegistriesWithPopAndPush(false)

  @Test
  fun nestedRegistriesWithPopAndPushAndCannotRetainWithKeys() =
    nestedRegistriesWithPopAndPushAndCannotRetain(true)

  @Test
  fun nestedRegistriesWithPopAndPushAndCannotRetainNoKeys() =
    nestedRegistriesWithPopAndPushAndCannotRetain(false)

  @Test
  fun singleInput() {
    val inputState = MutableStateFlow("first input")
    val content =
      @Composable {
        val input by inputState.collectAsState()
        InputsContent(input)
      }
    setActivityContent(content)
    composeTestRule.onNodeWithTag(TAG_REMEMBER).performTextInput("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained")
    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
    // Restart the activity
    scenario.recreate()
    // Compose our content
    setActivityContent(content)
    // Input didn't change, was the text saved
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
  }

  @Test
  fun changingInput() {
    val inputState = MutableStateFlow("first input")
    val content =
      @Composable {
        val input by inputState.collectAsState()
        InputsContent(input)
      }
    setActivityContent(content)
    composeTestRule.onNodeWithTag(TAG_REMEMBER).performTextInput("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained")
    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
    // New input
    inputState.value = "second input"
    // Was the text reset with the input change
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("")
  }

  @Test
  fun recreateWithChangingInput() {
    val inputState = MutableStateFlow("first input")
    val content =
      @Composable {
        val input by inputState.collectAsState()
        InputsContent(input)
      }
    setActivityContent(content)
    composeTestRule.onNodeWithTag(TAG_REMEMBER).performTextInput("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained")
    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
    // Restart the activity
    scenario.recreate()
    inputState.value = "second input"
    // Compose our content
    setActivityContent(content)
    // Was the text reset with the input change
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("")
  }

  @Test
  fun rememberObserver() {
    val subject =
      object : RememberObserver {
        var onRememberCalled: Int = 0
          private set

        var onForgottenCalled: Int = 0
          private set

        override fun onAbandoned() = Unit

        override fun onForgotten() {
          onForgottenCalled++
        }

        override fun onRemembered() {
          onRememberCalled++
        }
      }

    val content =
      @Composable {
        rememberRetained { subject }
        Unit
      }
    setActivityContent(content)

    assertThat(subject.onRememberCalled).isEqualTo(1)
    assertThat(subject.onForgottenCalled).isEqualTo(0)

    // Restart the activity
    scenario.recreate()
    // Compose our content again
    setActivityContent(content)

    // Assert that onRemembered was not called again
    assertThat(subject.onRememberCalled).isEqualTo(1)
    assertThat(subject.onForgottenCalled).isEqualTo(0)

    // Now finish the Activity
    scenario.close()

    // Assert that the observer was forgotten
    assertThat(subject.onRememberCalled).isEqualTo(1)
    assertThat(subject.onForgottenCalled).isEqualTo(1)
  }

  @Test
  fun rememberObserver_scopedCanRetainChecker() {
    val subject =
      object : RememberObserver {
        var onRememberCalled: Int = 0
          private set

        var onForgottenCalled: Int = 0
          private set

        override fun onAbandoned() = Unit

        override fun onForgotten() {
          onForgottenCalled++
        }

        override fun onRemembered() {
          onRememberCalled++
        }
      }
    val canRetainChecker = CanRetainChecker { false }

    val content =
      @Composable {
        val registry = rememberRetained { RetainedStateRegistry({ canRetainChecker.canRetain() }) }
        CompositionLocalProvider(LocalRetainedStateRegistry provides registry) {
          rememberRetained { subject }
          Unit
        }
      }
    setActivityContent(content)

    assertThat(subject.onRememberCalled).isEqualTo(1)
    assertThat(subject.onForgottenCalled).isEqualTo(0)

    // Restart the activity
    scenario.recreate()

    // Compose our content again
    setActivityContent(content)

    assertThat(subject.onRememberCalled).isEqualTo(2)
    assertThat(subject.onForgottenCalled).isEqualTo(1)

    // Now finish the Activity
    scenario.close()

    // Assert that the observer was forgotten
    assertThat(subject.onRememberCalled).isEqualTo(2)
    assertThat(subject.onForgottenCalled).isEqualTo(2)
  }

  @Test
  fun rememberObserver_scopedCanRetainCheckerSwitch() {
    val subject =
      object : RememberObserver {
        var onRememberCalled: Int = 0
          private set

        var onForgottenCalled: Int = 0
          private set

        override fun onAbandoned() = Unit

        override fun onForgotten() {
          onForgottenCalled++
        }

        override fun onRemembered() {
          onRememberCalled++
        }
      }

    var canRetainChecker by mutableStateOf(CanRetainChecker { true })

    val content =
      @Composable {
        val registry = rememberRetained { RetainedStateRegistry({ canRetainChecker.canRetain() }) }
        CompositionLocalProvider(LocalRetainedStateRegistry provides registry) {
          rememberRetained { subject }
          Unit
        }
      }
    setActivityContent(content)

    assertThat(subject.onRememberCalled).isEqualTo(1)
    assertThat(subject.onForgottenCalled).isEqualTo(0)

    // Restart the activity
    scenario.recreate()

    // Compose our content again
    setActivityContent(content)

    assertThat(subject.onRememberCalled).isEqualTo(1)
    assertThat(subject.onForgottenCalled).isEqualTo(0)

    canRetainChecker = CanRetainChecker { false }
    composeTestRule.waitForIdle()

    // Restart the activity
    scenario.recreate()

    // Compose our content again
    setActivityContent(content)

    assertThat(subject.onRememberCalled).isEqualTo(2)
    assertThat(subject.onForgottenCalled).isEqualTo(1)

    // Now finish the Activity
    scenario.close()

    // Assert that the observer was forgotten
    assertThat(subject.onRememberCalled).isEqualTo(2)
    assertThat(subject.onForgottenCalled).isEqualTo(2)
  }

  @Test
  fun rememberObserver_nestedRegistries() {
    val subject =
      object : RememberObserver {
        var onRememberCalled: Int = 0
          private set

        var onForgottenCalled: Int = 0
          private set

        override fun onAbandoned() = Unit

        override fun onForgotten() {
          onForgottenCalled++
        }

        override fun onRemembered() {
          onRememberCalled++
        }
      }

    val content =
      @Composable {
        val holder1 = rememberRetainedStateHolder()
        holder1.RetainedStateProvider("registry1") {
          val holder2 = rememberRetainedStateHolder()
          holder2.RetainedStateProvider("registry2") {
            @Suppress("UNUSED_VARIABLE") val retainedSubject = rememberRetained { subject }
          }
        }
      }
    setActivityContent(content)

    assertThat(subject.onRememberCalled).isEqualTo(1)
    assertThat(subject.onForgottenCalled).isEqualTo(0)

    // Restart the activity
    scenario.recreate()
    // Compose our content again
    setActivityContent(content)

    // Assert that onRemembered was not called again
    assertThat(subject.onRememberCalled).isEqualTo(1)
    assertThat(subject.onForgottenCalled).isEqualTo(0)

    // Now finish the Activity
    scenario.close()

    // Assert that the observer was forgotten
    assertThat(subject.onRememberCalled).isEqualTo(1)
    assertThat(subject.onForgottenCalled).isEqualTo(1)
  }

  @Test
  fun conditionalRetainBeforeSave() {
    val registry = RetainedStateRegistry()
    val content = @Composable { ConditionalRetainContent(registry) }
    setActivityContent(content)

    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertDoesNotExist()

    composeTestRule.onNodeWithTag(TAG_BUTTON_SHOW).performClick()
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextEquals("0")

    composeTestRule.onNodeWithTag(TAG_BUTTON_INC).performClick()
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextEquals("1")

    composeTestRule.onNodeWithTag(TAG_BUTTON_HIDE).performClick()
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertDoesNotExist()

    composeTestRule.onNodeWithTag(TAG_BUTTON_SHOW).performClick()
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextEquals("0")
  }

  @Test
  fun conditionalRetainAfterSave() {
    val registry = RetainedStateRegistry()
    val content = @Composable { ConditionalRetainContent(registry) }
    setActivityContent(content)

    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertDoesNotExist()

    composeTestRule.onNodeWithTag(TAG_BUTTON_SHOW).performClick()
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextEquals("0")

    composeTestRule.onNodeWithTag(TAG_BUTTON_INC).performClick()
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextEquals("1")

    composeTestRule.onNodeWithTag(TAG_BUTTON_HIDE).performClick()
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertDoesNotExist()

    registry.saveAll()

    composeTestRule.onNodeWithTag(TAG_BUTTON_SHOW).performClick()
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextEquals("0")
  }

  private fun nestedRegistriesWithPopAndPush(useKeys: Boolean) {
    val content = @Composable { NestedRetainWithPushAndPop(useKeys = useKeys) }
    setActivityContent(content)

    // Assert that Retained 1 is visible & Retained 2 does not exist
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertDoesNotExist()

    // Now click the button to show the child content
    composeTestRule.onNodeWithTag(TAG_BUTTON_SHOW).performClick()

    // Perform our initial text input
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained1")
    composeTestRule
      .onNodeWithTag(TAG_RETAINED_2)
      .assertIsDisplayed()
      .performTextInput("Text_Retained2")

    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertTextContains("Text_Retained2")

    // Now click the button to hide the nested content (aka a pop)
    composeTestRule.onNodeWithTag(TAG_BUTTON_HIDE).performClick()

    // Assert that Retained 1 is visible & Retained 2 does not exist
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertDoesNotExist()

    // Now click the button to show the nested content again (aka a push)
    composeTestRule.onNodeWithTag(TAG_BUTTON_SHOW).performClick()

    // Assert that the child content is using the retained content
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertTextContains("Text_Retained2")

    // Restart the activity
    scenario.recreate()
    // Compose our content
    setActivityContent(content)
    // Was the text saved
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertTextContains("Text_Retained2")
  }

  private fun nestedRegistriesWithPopAndPushAndCannotRetain(useKeys: Boolean) {
    val content = @Composable { NestedRetainWithPushAndPopAndCannotRetain(useKeys = useKeys) }
    setActivityContent(content)

    // Assert that Retained 1 is visible & Retained 2 does not exist
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertDoesNotExist()

    // Now click the button to show the child content
    composeTestRule.onNodeWithTag(TAG_BUTTON_SHOW).performClick()

    // Perform our initial text input
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained1")
    composeTestRule
      .onNodeWithTag(TAG_RETAINED_2)
      .assertIsDisplayed()
      .performTextInput("Text_Retained2")

    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertTextContains("Text_Retained2")

    // Now click the button to hide the nested content (aka a pop)
    composeTestRule.onNodeWithTag(TAG_BUTTON_HIDE).performClick()

    // Assert that Retained 1 is visible & Retained 2 does not exist
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertDoesNotExist()

    // Now click the button to show the nested content again (aka a push)
    composeTestRule.onNodeWithTag(TAG_BUTTON_SHOW).performClick()

    // Assert that the child content is _not_ using the retained content since can retain checker is
    // false
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assert(!hasText("Text_Retained2"))

    // Restart the activity
    scenario.recreate()
    // Compose our content
    setActivityContent(content)
    // Was the text not saved
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assert(!hasText("Text_Retained2"))
  }

  private fun setActivityContent(content: @Composable () -> Unit) {
    scenario.onActivity { activity ->
      activity.setContent {
        CompositionLocalProvider(
          LocalRetainedStateRegistry provides
            viewModelRetainedStateRegistry(LifecycleRetainedStateRegistry.KEY, vmFactory)
        ) {
          content()
        }
      }
    }
  }

  private fun testMultipleRetainedContent(content: @Composable () -> Unit) {
    setActivityContent(content)

    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).performTextInput("Text_Retained2")
    composeTestRule.onNodeWithTag(TAG_RETAINED_3).performTextInput("2")
    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertTextContains("Text_Retained2")
    composeTestRule.onNodeWithTag(TAG_RETAINED_3).assertTextContains("2", substring = true)
    // Restart the activity
    scenario.recreate()
    // Compose our content
    setActivityContent(content)
    // Was the text saved
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertTextContains("Text_Retained2")
    composeTestRule.onNodeWithTag(TAG_RETAINED_3).assertTextContains("2", substring = true)
  }
}

@Composable
internal fun KeyContent(key: String?) {
  var text1 by remember { mutableStateOf("") }
  // By default rememberSavable uses it's line number as its key, this doesn't seem
  // to work when testing, instead pass a key
  var retainedText: String by rememberRetained(key = key) { mutableStateOf("") }
  Column {
    TextField(
      modifier = Modifier.testTag(TAG_REMEMBER),
      value = text1,
      onValueChange = { text1 = it },
      label = {},
    )
    TextField(
      modifier = Modifier.testTag(TAG_RETAINED_1),
      value = retainedText,
      onValueChange = { retainedText = it },
      label = {},
    )
  }
}

@Composable
private fun MultipleRetains(useKeys: Boolean) {
  var retainedInt: Int by
    rememberRetained(key = "retainedInt".takeIf { useKeys }) { mutableStateOf(0) }
  var retainedText1: String by
    rememberRetained(key = "retained1".takeIf { useKeys }) { mutableStateOf("") }
  var retainedText2: String by
    rememberRetained(key = "retained2".takeIf { useKeys }) { mutableStateOf("") }
  Column {
    TextField(
      modifier = Modifier.testTag(TAG_RETAINED_1),
      value = retainedText1,
      onValueChange = { retainedText1 = it },
      label = {},
    )
    TextField(
      modifier = Modifier.testTag(TAG_RETAINED_2),
      value = retainedText2,
      onValueChange = { retainedText2 = it },
      label = {},
    )
    TextField(
      modifier = Modifier.testTag(TAG_RETAINED_3),
      value = retainedInt.toString(),
      onValueChange = { retainedInt = it.toInt() },
      label = {},
    )
  }
}

@Composable
private fun NestedRetains(useKeys: Boolean) {
  var retainedText1: String by
    rememberRetained(key = "retained1".takeIf { useKeys }) { mutableStateOf("") }

  Column {
    TextField(
      modifier = Modifier.testTag(TAG_RETAINED_1),
      value = retainedText1,
      onValueChange = { retainedText1 = it },
      label = {},
    )

    val nestedStateHolderLevel1 = rememberRetainedStateHolder()
    nestedStateHolderLevel1.RetainedStateProvider("registryLevel1") { NestedRetainLevel1(useKeys) }
  }
}

@Composable
private fun NestedRetainLevel1(useKeys: Boolean) {
  var retainedText2: String by
    rememberRetained(key = "retained2".takeIf { useKeys }) { mutableStateOf("") }

  TextField(
    modifier = Modifier.testTag(TAG_RETAINED_2),
    value = retainedText2,
    onValueChange = { retainedText2 = it },
    label = {},
  )

  val nestedStateHolderLevel2 = rememberRetainedStateHolder()
  nestedStateHolderLevel2.RetainedStateProvider("registryLevel2") { NestedRetainLevel2(useKeys) }
}

@Composable
private fun NestedRetainLevel2(useKeys: Boolean) {
  var retainedInt: Int by
    rememberRetained(key = "retainedInt".takeIf { useKeys }) { mutableStateOf(0) }

  TextField(
    modifier = Modifier.testTag(TAG_RETAINED_3),
    value = retainedInt.toString(),
    onValueChange = { retainedInt = it.toInt() },
    label = {},
  )
}

@Composable
private fun NestedRetainWithPushAndPop(useKeys: Boolean) {
  var retainedText1: String by
    rememberRetained(key = "retained1".takeIf { useKeys }) { mutableStateOf("") }

  Column {
    TextField(
      modifier = Modifier.testTag(TAG_RETAINED_1),
      value = retainedText1,
      onValueChange = { retainedText1 = it },
      label = {},
    )

    val showNestedContent = rememberRetained { mutableStateOf(false) }

    Button(
      onClick = { showNestedContent.value = false },
      modifier = Modifier.testTag(TAG_BUTTON_HIDE),
    ) {
      Text(text = "Hide child")
    }

    Button(
      onClick = { showNestedContent.value = true },
      modifier = Modifier.testTag(TAG_BUTTON_SHOW),
    ) {
      Text(text = "Show child")
    }

    val retainedStateHolder = rememberRetainedStateHolder()
    // Keep the retained state registry around even if showNestedContent becomes false
    if (showNestedContent.value) {
      retainedStateHolder.RetainedStateProvider("registry") { NestedRetainLevel1(useKeys) }
    }
  }
}

@Composable
private fun NestedRetainWithPushAndPopAndCannotRetain(useKeys: Boolean) {
  var retainedText1: String by
    rememberRetained(key = "retained1".takeIf { useKeys }) { mutableStateOf("") }

  Column {
    TextField(
      modifier = Modifier.testTag(TAG_RETAINED_1),
      value = retainedText1,
      onValueChange = { retainedText1 = it },
      label = {},
    )

    val showNestedContent = rememberRetained { mutableStateOf(false) }

    Button(
      onClick = { showNestedContent.value = false },
      modifier = Modifier.testTag(TAG_BUTTON_HIDE),
    ) {
      Text(text = "Hide child")
    }

    Button(
      onClick = { showNestedContent.value = true },
      modifier = Modifier.testTag(TAG_BUTTON_SHOW),
    ) {
      Text(text = "Show child")
    }

    // Keep the retained state registry around even if showNestedContent becomes false
    val holder = rememberRetainedStateHolder { false }
    if (showNestedContent.value) {
      holder.RetainedStateProvider("registry") { NestedRetainLevel1(useKeys) }
    }
  }
}

@Composable
private fun InputsContent(input: String) {
  var text1 by remember(input) { mutableStateOf("") }
  var retainedText: String by rememberRetained(input) { mutableStateOf("") }
  Column {
    TextField(
      modifier = Modifier.testTag(TAG_REMEMBER),
      value = text1,
      onValueChange = { text1 = it },
      label = {},
    )
    TextField(
      modifier = Modifier.testTag(TAG_RETAINED_1),
      value = retainedText,
      onValueChange = { retainedText = it },
      label = {},
    )
  }
}

@Composable
private fun ConditionalRetainContent(registry: RetainedStateRegistry) {
  CompositionLocalProvider(LocalRetainedStateRegistry provides registry) {
    var showContent by remember { mutableStateOf(false) }
    Column {
      Button(modifier = Modifier.testTag(TAG_BUTTON_HIDE), onClick = { showContent = false }) {
        Text(text = "Hide content")
      }
      Button(modifier = Modifier.testTag(TAG_BUTTON_SHOW), onClick = { showContent = true }) {
        Text(text = "Show content")
      }
      if (showContent) {
        var count by rememberRetained { mutableIntStateOf(0) }
        Button(modifier = Modifier.testTag(TAG_BUTTON_INC), onClick = { count += 1 }) {
          Text(text = "Increment")
        }
        Text(modifier = Modifier.testTag(TAG_RETAINED_1), text = count.toString())
      }
    }
  }
}

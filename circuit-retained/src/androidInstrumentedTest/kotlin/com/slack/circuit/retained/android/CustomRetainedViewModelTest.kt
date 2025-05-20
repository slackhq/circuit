// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained.android

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.test.core.app.ActivityScenario
import com.slack.circuit.retained.CanRetainChecker
import com.slack.circuit.retained.Continuity
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistryImpl
import com.slack.circuit.retained.RetainedValueProvider
import com.slack.circuit.retained.UpdatableRetainedStateRegistry
import com.slack.circuit.retained.ViewModelRetainedStateRegistryFactory
import com.slack.circuit.retained.continuityRetainedStateRegistry
import leakcanary.DetectLeaksAfterTestSuccess.Companion.detectLeaksAfterTestSuccessWrapping
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class CustomRetainedViewModelTest {
  private val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val rule =
    RuleChain.emptyRuleChain().detectLeaksAfterTestSuccessWrapping(tag = "ActivitiesDestroyed") {
      around(composeTestRule)
    }

  private val scenario: ActivityScenario<ComponentActivity>
    get() = composeTestRule.activityRule.scenario

  private val vmFactory = CustomContinuityViewModel.Factory()

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

  private fun setActivityContent(content: @Composable () -> Unit) {
    scenario.onActivity { activity ->
      activity.setContent {
        CompositionLocalProvider(
          LocalRetainedStateRegistry provides
            continuityRetainedStateRegistry(Continuity.KEY, vmFactory)
        ) {
          content()
        }
      }
    }
  }
}

private class CustomContinuityViewModel :
  ViewModel(), UpdatableRetainedStateRegistry, CanRetainChecker {
  private val delegate = RetainedStateRegistryImpl(this, null)
  private var canRetainChecker = CanRetainChecker.Never

  override fun update(canRetainChecker: CanRetainChecker) {
    this.canRetainChecker = canRetainChecker
  }

  override fun consumeValue(key: String): Any? {
    return delegate.consumeValue(key)
  }

  override fun registerValue(
    key: String,
    valueProvider: RetainedValueProvider,
  ): RetainedStateRegistry.Entry {
    return delegate.registerValue(key, valueProvider)
  }

  override fun saveAll(): Map<String, List<Any?>> {
    return delegate.saveAll()
  }

  override fun saveValue(key: String) {
    delegate.saveValue(key)
  }

  override fun forgetUnclaimedValues() {
    delegate.forgetUnclaimedValues()
  }

  override fun canRetain(): Boolean {
    return canRetainChecker.canRetain()
  }

  override fun onCleared() {
    delegate.forgetUnclaimedValues()
    delegate.valueProviders.clear()
  }

  class Factory : ViewModelRetainedStateRegistryFactory<CustomContinuityViewModel> {
    var continuity: UpdatableRetainedStateRegistry? = null

    override fun create(
      modelClass: Class<CustomContinuityViewModel>,
      extras: CreationExtras?,
    ): CustomContinuityViewModel {
      return CustomContinuityViewModel().also { continuity = it }
    }
  }
}

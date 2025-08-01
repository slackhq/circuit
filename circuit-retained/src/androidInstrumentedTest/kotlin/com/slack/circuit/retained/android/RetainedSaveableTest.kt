// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained.android

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.test.core.app.ActivityScenario
import com.slack.circuit.retained.CanRetainChecker
import com.slack.circuit.retained.LifecycleRetainedStateRegistry
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.NoOpRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistryViewModel
import com.slack.circuit.retained.ViewModelRetainedStateRegistryFactory
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.retained.viewModelRetainedStateRegistry
import kotlin.reflect.KClass
import leakcanary.DetectLeaksAfterTestSuccess.Companion.detectLeaksAfterTestSuccessWrapping
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class RetainedSaveableTest {
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

  private var canRetainOverride: Boolean? = null

  @Test
  fun retainedIsUsedWhenRecreating() {
    var id = 0
    lateinit var data: CacheableData

    val content =
      @Composable {
        data = rememberRetained(saver = CacheableData.Saver) { CacheableData(id++) }
        Text(modifier = Modifier.testTag("id"), text = "${data.id}")
        Text(modifier = Modifier.testTag("superBigData"), text = "${data.superBigData}")
      }

    setActivityContent(content)

    // Check initial state is correct
    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("null")

    data.superBigData = "Super big data"
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("Super big data")

    scenario.recreate()

    setActivityContent(content)

    // Retained state is preserved
    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("Super big data")
  }

  @Test
  fun saveableIsUsedWhenRecreatingWithProcessDeath() {
    var id = 0
    lateinit var data: CacheableData

    val content =
      @Composable {
        data = rememberRetained(saver = CacheableData.Saver) { CacheableData(id++) }
        Text(modifier = Modifier.testTag("id"), text = "${data.id}")
        Text(modifier = Modifier.testTag("superBigData"), text = "${data.superBigData}")
      }

    setActivityContent(content)

    // Check initial state is correct
    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("null")

    data.superBigData = "Super big data"
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("Super big data")

    // Override can retain to drop retaining values, to simulate process death
    canRetainOverride = false
    scenario.recreate()
    canRetainOverride = null

    setActivityContent(content)

    // Retained state is not preserved, but id is
    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("null")
  }

  @Test
  fun saveableIsUsedWhenRetainIsNoop() {
    var id = 0
    lateinit var data: CacheableData

    fun setContent() {
      scenario.onActivity { activity ->
        activity.setContent {
          CompositionLocalProvider(LocalRetainedStateRegistry provides NoOpRetainedStateRegistry) {
            data = rememberRetained(saver = CacheableData.Saver) { CacheableData(id++) }
            Text(modifier = Modifier.testTag("id"), text = "${data.id}")
            Text(modifier = Modifier.testTag("superBigData"), text = "${data.superBigData}")
          }
        }
      }
    }

    setContent()

    // Check initial state is correct
    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("null")

    data.superBigData = "Super big data"
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("Super big data")

    scenario.recreate()

    setContent()

    // Retained state is not preserved, but id is
    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("null")
  }

  private fun setActivityContent(content: @Composable () -> Unit) {
    scenario.onActivity { activity ->
      activity.setContent {
        CompositionLocalProvider(
          LocalRetainedStateRegistry provides
            viewModelRetainedStateRegistry(LifecycleRetainedStateRegistry.KEY, vmFactory) {
              canRetainOverride ?: CanRetainChecker.Always.canRetain()
            }
        ) {
          content()
        }
      }
    }
  }
}

@Stable
private class CacheableData(val id: Int) {
  var superBigData: String? by mutableStateOf(null)

  companion object {
    val Saver: Saver<CacheableData, *> = Saver({ it.id }, { CacheableData(it) })
  }
}

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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.retained.RetainedValueProvider
import com.slack.circuit.retained.rememberRetained

/**
 * Helps to test the retained state restoration for your Composable component.
 *
 * Instead of calling [ComposeContentTestRule.setContent] you need to use [setContent] on this
 * object, then change your state so there is some change to be restored, then execute
 * [emulateRetainedInstanceStateRestore] and assert your state is restored properly.
 *
 * Note that this tests only the restoration of the local state of the composable you passed to
 * [setContent] and useful for testing [rememberRetained] integration. It is not testing the
 * integration with any other life cycles or Activity callbacks.
 */
// TODO recreate for more realism? Need to save the content function to do that, or call it after
// TODO make this available in test utils?
class RetainedStateRestorationTester(private val composeTestRule: ComposeContentTestRule) {

  private var registry: RestorationRegistry? = null

  /**
   * This functions is a direct replacement for [ComposeContentTestRule.setContent] if you are going
   * to use [emulateRetainedInstanceStateRestore] in the test.
   *
   * @see ComposeContentTestRule.setContent
   */
  fun setContent(composable: @Composable () -> Unit) {
    composeTestRule.setContent {
      CompositionLocalProvider(
        LocalRetainedStateRegistry provides remember { RetainedStateRegistry() }
      ) {
        InjectRestorationRegistry { registry ->
          this.registry = registry
          composable()
        }
      }
    }
  }

  /**
   * Saves all the state stored via [rememberRetained], disposes current composition, and composes
   * again the content passed to [setContent]. Allows to test how your component behaves when the
   * state restoration is happening. Note that the state stored via regular state() or remember()
   * will be lost.
   */
  fun emulateRetainedInstanceStateRestore() {
    val registry = checkNotNull(registry) { "setContent should be called first!" }
    composeTestRule.runOnIdle { registry.saveStateAndDisposeChildren() }
    composeTestRule.runOnIdle { registry.emitChildrenWithRestoredState() }
    composeTestRule.runOnIdle {
      // we just wait for the children to be emitted
    }
  }

  @Composable
  private fun InjectRestorationRegistry(content: @Composable (RestorationRegistry) -> Unit) {
    val original =
      requireNotNull(LocalRetainedStateRegistry.current) {
        "StateRestorationTester requires composeTestRule.setContent() to provide " +
          "a RetainedStateRegistry implementation via LocalRetainedStateRegistry"
      }
    val restorationRegistry = remember { RestorationRegistry(original) }
    CompositionLocalProvider(LocalRetainedStateRegistry provides restorationRegistry) {
      if (restorationRegistry.shouldEmitChildren) {
        content(restorationRegistry)
      }
    }
  }

  private class RestorationRegistry(private val original: RetainedStateRegistry) :
    RetainedStateRegistry {

    var shouldEmitChildren by mutableStateOf(true)
      private set

    private var currentRegistry: RetainedStateRegistry = original
    private var savedMap: Map<String, List<Any?>> = emptyMap()

    fun saveStateAndDisposeChildren() {
      savedMap = currentRegistry.saveAll()
      shouldEmitChildren = false
    }

    fun emitChildrenWithRestoredState() {
      currentRegistry = RetainedStateRegistry(values = savedMap)
      shouldEmitChildren = true
    }

    override fun consumeValue(key: String): Any? {
      return currentRegistry.consumeValue(key)
    }

    override fun registerValue(
      key: String,
      valueProvider: RetainedValueProvider,
    ): RetainedStateRegistry.Entry {
      return currentRegistry.registerValue(key, valueProvider)
    }

    override fun saveAll(): Map<String, List<Any?>> {
      return currentRegistry.saveAll()
    }

    override fun saveValue(key: String) {
      currentRegistry.saveValue(key)
    }

    override fun forgetUnclaimedValues() {
      currentRegistry.forgetUnclaimedValues()
    }
  }
}

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
package com.slack.circuit.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.remember

/**
 * Allows to retain the state defined with [rememberRetained] for the subtree before disposing it to
 * make it possible to compose it back next time with the restored state. It allows different
 * navigation patterns to keep the ui state like scroll position for the currently not composed
 * screens from the backstack.
 *
 * The content should be composed using [RetainedStateProvider] while providing a key representing
 * this content. Next time [RetainedStateProvider] will be used with the same key its state will be
 * restored.
 */
public interface RetainedStateHolder {
  /**
   * Put your content associated with a [key] inside the [content]. This will automatically retain
   * all the states defined with [rememberRetained] before disposing the content and will restore
   * the states when you compose with this key again.
   *
   * @param key to be used for saving and restoring the states for the subtree. Note that on Android
   *   you can only use types which can be stored inside the Bundle.
   */
  @Composable public fun RetainedStateProvider(key: Any, content: @Composable () -> Unit)

  /** Removes the retained state associated with the passed [key]. */
  public fun removeState(key: Any)
}

/** Creates and remembers the instance of [RetainedStateHolder]. */
@Composable
public fun rememberRetainedStateHolder(): RetainedStateHolder =
  rememberRetained { RetainedStateHolderImpl() }
    .apply { parentRetainedStateRegistry = LocalRetainedStateRegistry.current }

private class RetainedStateHolderImpl(
  private val retainedStates: MutableMap<Any, Map<String, List<Any?>>> = mutableMapOf()
) : RetainedStateHolder {
  private val registryHolders = mutableMapOf<Any, RegistryHolder>()
  var parentRetainedStateRegistry: RetainedStateRegistry? = null

  @Composable
  override fun RetainedStateProvider(key: Any, content: @Composable () -> Unit) {
    ReusableContent(key) {
      val registryHolder = remember { RegistryHolder(key) }
      CompositionLocalProvider(
        LocalRetainedStateRegistry provides registryHolder.registry,
        content = content,
      )
      DisposableEffect(Unit) {
        require(key !in registryHolders) { "Key $key was used multiple times " }
        retainedStates -= key
        registryHolders[key] = registryHolder
        onDispose {
          registryHolder.saveTo(retainedStates)
          registryHolders -= key
        }
      }
    }
  }

  private fun saveAll(): MutableMap<Any, Map<String, List<Any?>>>? {
    val map = retainedStates.toMutableMap()
    registryHolders.values.forEach { it.saveTo(map) }
    return map.ifEmpty { null }
  }

  override fun removeState(key: Any) {
    val registryHolder = registryHolders[key]
    if (registryHolder != null) {
      registryHolder.shouldSave = false
    } else {
      retainedStates -= key
    }
  }

  inner class RegistryHolder(val key: Any) {
    var shouldSave = true
    val registry: RetainedStateRegistry = RetainedStateRegistry(retainedStates[key].orEmpty())

    fun saveTo(map: MutableMap<Any, Map<String, List<Any?>>>) {
      if (shouldSave) {
        val retainedData = registry.saveAll()
        if (retainedData.isEmpty()) {
          map -= key
        } else {
          map[key] = retainedData
        }
      }
    }
  }
}

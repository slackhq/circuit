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
package com.slack.circuit.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.slack.circuit.retained.RetainedStateRegistry.Entry

/**
 * Allows components to save and restore their state using the `onRetainNonConfigurationInstance`
 * mechanism.
 */
public interface RetainedStateRegistry {
  /**
   * Returns the restored value for the given key. Once being restored the value is cleared, so you
   * can't restore the same key twice.
   *
   * @param key Key used to save the value
   */
  public fun consumeValue(key: String): Any?

  /**
   * Registers the value provider.
   *
   * There are could be multiple providers registered for the same [key]. In this case the order in
   * which they were registered matters.
   *
   * Say we registered two values for the key. One provides "1", second provides "2". this case will
   * have listOf("1", "2) as a value for the key in the underlying map. And later, when the registry
   * will be recreated with the previously saved values, the first execution of [consumeValue] would
   * consume "1" and the second one "2".
   *
   * @param key Key to use for storing the value
   * @param value The current value
   * @return the registry entry which you can use to unregister the provider
   */
  public fun registerValue(key: String, value: Any?): Entry

  /** The registry entry which you get when you use [registerValue]. */
  public interface Entry {
    /** Unregister previously registered entry. */
    public fun unregister()
  }
}

internal interface MutableRetainedStateRegistry : RetainedStateRegistry {
  val retained: MutableMap<String, MutableList<Any?>>
}

/**
 * Creates [RetainedStateRegistry].
 *
 * @param values The map of the restored values
 */
public fun RetainedStateRegistry(values: Map<String, List<Any?>>?): RetainedStateRegistry =
  RetainedStateRegistryImpl(
    values?.mapValues { it.value.toMutableList() }?.toMutableMap() ?: mutableMapOf()
  )

/** CompositionLocal with a current [RetainedStateRegistry] instance. */
public object LocalRetainedStateRegistryOwner {
  private val LocalRetainedStateRegistry =
    staticCompositionLocalOf<RetainedStateRegistry> { NoOpRetainedStateRegistry }

  /**
   * Returns current composition local value for the owner or errors if one has not been provided.
   */
  public val current: RetainedStateRegistry
    @Composable get() = LocalRetainedStateRegistry.current

  /**
   * Associates a [LocalRetainedStateRegistry] key to a value in a call to
   * [CompositionLocalProvider].
   */
  @Composable
  public infix fun provides(registry: RetainedStateRegistry): ProvidedValue<RetainedStateRegistry> {
    return LocalRetainedStateRegistry.provides(registry)
  }
}

internal class RetainedStateRegistryImpl(
  override val retained: MutableMap<String, MutableList<Any?>> = mutableMapOf()
) : MutableRetainedStateRegistry {

  override fun consumeValue(key: String): Any? {
    val list = retained.remove(key)
    return if (!list.isNullOrEmpty()) {
      if (list.size > 1) {
        retained[key] = list.subList(1, list.size)
      }
      list[0]
    } else {
      null
    }
  }

  override fun registerValue(key: String, value: Any?): Entry {
    require(key.isNotBlank()) { "Registered key is empty or blank" }
    retained.getOrPut(key) { mutableListOf() }.add(value)
    return object : Entry {
      override fun unregister() {
        val list = retained.remove(key)
        list?.remove(value)
        if (!list.isNullOrEmpty()) {
          // if there are other providers for this key return list back to the map
          retained[key] = list
        }
      }
    }
  }
}

internal object NoOpRetainedStateRegistry : RetainedStateRegistry {
  override fun consumeValue(key: String): Any? = null
  override fun registerValue(key: String, value: Any?): Entry = NoOpEntry

  private object NoOpEntry : Entry {
    override fun unregister() {}
  }
}

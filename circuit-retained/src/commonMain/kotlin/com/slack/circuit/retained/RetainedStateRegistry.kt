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
   * @param valueProvider The value to provide
   * @return the registry entry which you can use to unregister the provider
   */
  public fun registerValue(key: String, valueProvider: () -> Any?): Entry

  /**
   * Executes all the registered value providers and combines these values into a map. We have a
   * list of values for each key as it is allowed to have multiple providers for the same key.
   */
  public fun performSave()

  /** Releases all currently unconsumed values. Useful as a GC mechanism for the registry. */
  public fun forgetUnclaimedValues()

  /** The registry entry which you get when you use [registerValue]. */
  public interface Entry {
    /** Unregister previously registered entry. */
    public fun unregister()
  }
}

internal interface MutableRetainedStateRegistry : RetainedStateRegistry {
  val retained: MutableMap<String, List<Any?>>
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

internal class RetainedStateRegistryImpl(retained: MutableMap<String, List<Any?>>?) :
  MutableRetainedStateRegistry {

  override val retained: MutableMap<String, List<Any?>> = retained?.toMutableMap() ?: mutableMapOf()
  internal val valueProviders = mutableMapOf<String, MutableList<() -> Any?>>()

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

  override fun registerValue(key: String, valueProvider: () -> Any?): Entry {
    require(key.isNotBlank()) { "Registered key is empty or blank" }
    valueProviders.getOrPut(key) { mutableListOf() }.add(valueProvider)
    return object : Entry {
      override fun unregister() {
        val list = valueProviders.remove(key)
        list?.remove(valueProvider)
        if (!list.isNullOrEmpty()) {
          // if there are other providers for this key return list back to the map
          valueProviders[key] = list
        }
      }
    }
  }

  override fun performSave() {
    val map = retained.toMutableMap()
    valueProviders.forEach { (key, list) ->
      if (list.size == 1) {
        val value = list[0].invoke()
        if (value != null) {
          map[key] = arrayListOf<Any?>(value)
        }
      } else {
        // if we have multiple providers we should store null values as well to preserve
        // the order in which providers were registered. say there were two providers.
        // the first provider returned null(nothing to save) and the second one returned
        // "1". when we will be restoring the first provider would restore null (it is the
        // same as to have nothing to restore) and the second one restore "1".
        map[key] =
          List(list.size) { index ->
            val value = list[index].invoke()
            value
          }
      }
    }
    valueProviders.clear()
    retained.putAll(map)
  }

  override fun forgetUnclaimedValues() {
    retained.clear()
  }
}

internal object NoOpRetainedStateRegistry : RetainedStateRegistry {
  override fun consumeValue(key: String): Any? = null
  override fun registerValue(key: String, valueProvider: () -> Any?): Entry = NoOpEntry
  override fun performSave() {}
  override fun forgetUnclaimedValues() {}

  private object NoOpEntry : Entry {
    override fun unregister() {}
  }
}

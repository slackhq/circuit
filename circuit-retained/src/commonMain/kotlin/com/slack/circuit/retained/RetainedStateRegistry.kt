// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.RememberObserver
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
  public fun registerValue(key: String, valueProvider: RetainedValueProvider): Entry

  /**
   * Executes all the registered value providers and combines these values into a map. We have a
   * list of values for each key as it is allowed to have multiple providers for the same key.
   */
  public fun saveAll()

  /** Executes the value providers registered with the given [key], and saves them for retrieval. */
  public fun saveValue(key: String)

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
public fun RetainedStateRegistry(
  values: Map<String, List<Any?>> = emptyMap()
): RetainedStateRegistry =
  RetainedStateRegistryImpl(values.mapValues { it.value.toMutableList() }.toMutableMap())

/** CompositionLocal with a current [RetainedStateRegistry] instance. */
public val LocalRetainedStateRegistry: ProvidableCompositionLocal<RetainedStateRegistry> =
  staticCompositionLocalOf {
    NoOpRetainedStateRegistry
  }

internal class RetainedStateRegistryImpl(retained: MutableMap<String, List<Any?>>?) :
  MutableRetainedStateRegistry {

  override val retained: MutableMap<String, List<Any?>> = retained?.toMutableMap() ?: mutableMapOf()
  internal val valueProviders = mutableMapOf<String, MutableList<RetainedValueProvider>>()

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

  override fun registerValue(key: String, valueProvider: RetainedValueProvider): Entry {
    require(key.isNotBlank()) { "Registered key is empty or blank" }
    valueProviders.getOrPut(key, ::mutableListOf).add(valueProvider)
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

  override fun saveAll() {
    val values =
      valueProviders.mapValues { (_, list) ->
        // If we have multiple providers we should store null values as well to preserve
        // the order in which providers were registered. Say there were two providers.
        // the first provider returned null(nothing to save) and the second one returned
        // "1". When we will be restoring the first provider would restore null (it is the
        // same as to have nothing to restore) and the second one restore "1".
        list.map(RetainedValueProvider::invoke)
      }

    if (values.isNotEmpty()) {
      // Store the values in our retained map
      retained.putAll(values)
    }
    // Clear the value providers now that we've stored the values
    valueProviders.clear()
  }

  override fun saveValue(key: String) {
    val providers = valueProviders[key]
    if (providers != null) {
      retained[key] = providers.map { it.invoke() }
      valueProviders.remove(key)
    }
  }

  override fun forgetUnclaimedValues() {
    fun clearValue(value: Any?) {
      when (value) {
        // If we get a RetainedHolder value, need to unwrap and call again
        is RetainedValueHolder<*> -> clearValue(value.value)
        // Dispatch the call to nested registries
        is RetainedStateRegistry -> value.forgetUnclaimedValues()
        // Dispatch onForgotten calls if the value is a RememberObserver
        is RememberObserver -> value.onForgotten()
      }
    }

    retained.values.forEach { it.forEach(::clearValue) }
    retained.clear()
  }
}

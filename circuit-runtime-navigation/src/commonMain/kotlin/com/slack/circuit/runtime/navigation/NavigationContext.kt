// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import com.slack.circuit.runtime.navigation.internal.NoOpMap
import kotlin.reflect.KClass

@Stable
public class NavigationContext
@InternalCircuitNavigationApi
public constructor(
  // Don't expose the raw map.
  internal val tags: MutableMap<KClass<*>, Any> = mutableMapOf()
) {

  /** Returns the tag attached with [type] as a key, or null if no tag is attached with that key. */
  public fun <T : Any> tag(type: KClass<T>): T? {
    @Suppress("UNCHECKED_CAST")
    return tags[type] as T?
  }

  /** Returns the tag attached with [T] as a key, or null if no tag is attached with that key. */
  public inline fun <reified T : Any> tag(): T? = tag(T::class)

  /**
   * Attaches [tag] to the request using [T] as a key. Tags can be read from a request using
   * [NavigationContext.tag]. Use `null` to remove any existing tag assigned for [T].
   *
   * Use this API to attach metadata, debugging, or other application data to a spec so that you may
   * read it in other APIs or callbacks.
   */
  public inline fun <reified T : Any> putTag(tag: T?): Unit = putTag(T::class, tag)

  /**
   * Attaches [tag] to the request using [type] as a key. Tags can be read from a request using
   * [NavigationContext.tag]. Use `null` to remove any existing tag assigned for [type].
   *
   * Use this API to attach metadata, debugging, or other application data to a spec so that you may
   * read it in other APIs or callbacks.
   */
  public fun <S : Any, T : S> putTag(type: KClass<S>, tag: T?) {
    if (tag == null) {
      this.tags.remove(type)
    } else {
      this.tags[type] = tag
    }
  }

  /** Clears all the current tags. */
  public fun clearTags() {
    tags.clear()
  }

  public companion object {
    /** An empty context */
    @Suppress("UNCHECKED_CAST")
    @OptIn(InternalCircuitNavigationApi::class)
    public val EMPTY: NavigationContext = NavigationContext(NoOpMap as MutableMap<KClass<*>, Any>)

    @OptIn(InternalCircuitNavigationApi::class)
    public val Saver: Saver<NavigationContext, Any> =
      Saver(
        save = { value -> mapOf("tags" to value.tags) },
        restore = { value ->
          @Suppress("UNCHECKED_CAST")
          NavigationContext((value as Map<String, Any>)["tags"] as MutableMap<KClass<*>, Any>)
        },
      )
  }
}

/**
 * Returns a new [NavigationContext] that contains all the tags from both [this] and [other].
 *
 * Tags in [other] will overwrite any matching tags in [this].
 */
@OptIn(InternalCircuitNavigationApi::class)
public operator fun NavigationContext.plus(other: NavigationContext): NavigationContext {
  val newTags = mutableMapOf<KClass<*>, Any>()
  newTags.putAll(this.tags)
  newTags.putAll(other.tags)
  return NavigationContext(newTags)
}

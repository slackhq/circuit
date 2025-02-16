// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.slack.circuit.runtime.navigation.internal.NoOpMap
import kotlin.reflect.KClass

@Stable
public class NavigationContext
@InternalCircuitNavigationApi
public constructor(
  // Don't expose the raw map.
  internal val tags: MutableMap<String, Any>
) {

  @Suppress("UnnecessaryOptInAnnotation") // Needed for the primary constructor.
  @OptIn(InternalCircuitNavigationApi::class)
  public constructor() : this(mutableMapOf())

  /** Returns the tag attached with [type] as a key, or null if no tag is attached with that key. */
  public fun <T : Any> tag(type: KClass<T>): T? {
    val key = tagKey(type) ?: return null
    @Suppress("UNCHECKED_CAST")
    return tags[key] as T?
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
    val key = tagKey(type) ?: return
    if (tag == null) {
      this.tags.remove(key)
    } else {
      this.tags[key] = tag
    }
  }

  /** Clears all the current tags. */
  public fun clearTags() {
    tags.clear()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is NavigationContext) return false
    return tags == other.tags
  }

  override fun hashCode(): Int {
    return tags.hashCode()
  }

  public companion object {
    /** An empty [NavigationContext] */
    @Suppress("UNCHECKED_CAST")
    @OptIn(InternalCircuitNavigationApi::class)
    public val Empty: NavigationContext = NavigationContext(NoOpMap as MutableMap<String, Any>)

    @OptIn(InternalCircuitNavigationApi::class)
    public val Saver: Saver<NavigationContext, Any> =
      // Same approach as mapSaver
      listSaver(
        save = { context ->
          mutableListOf<Any>().apply {
            context.tags.forEach { (key, value) ->
              if (canBeSaved(value)) {
                add(key)
                add(value)
              }
            }
          }
        },
        restore = { list ->
          val tags = mutableMapOf<String, Any>()
          check(list.size.rem(2) == 0) { "non-zero remainder" }
          var index = 0
          while (index < list.size) {
            val key = list[index] as String
            val value = list[index + 1] as Any
            tags[key] = value
            index += 2
          }
          NavigationContext(tags)
        },
      )
  }
}

internal expect fun tagKey(type: KClass<*>): String?

/**
 * Returns a new [NavigationContext] that contains all the tags from both [this] and [other].
 *
 * Tags in [other] will overwrite any matching tags in [this].
 */
@OptIn(InternalCircuitNavigationApi::class)
public operator fun NavigationContext.plus(other: NavigationContext): NavigationContext {
  val newTags = mutableMapOf<String, Any>()
  tags.putAll(this.tags)
  newTags.putAll(other.tags)
  return NavigationContext(newTags)
}

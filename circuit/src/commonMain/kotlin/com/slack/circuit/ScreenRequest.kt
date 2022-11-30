// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import kotlin.reflect.KClass

/**
 * A [ScreenRequest] is used in [Presenter.Factory] and [Ui.Factory] to inform creation of their
 * respective [Presenter] and [Ui] instances. The primary data is the [screen] itself, but
 * additional information can be passed in the [tag] API.
 */
public class ScreenRequest private constructor(builder: Builder) {

  /** The [Screen] that is being requested. */
  public val screen: Screen = builder.screen

  // Don't expose the raw map.
  private val tags: Map<KClass<*>, Any> = builder.tags.toMap()

  /** Returns the tag attached with [type] as a key, or null if no tag is attached with that key. */
  public fun <T : Any> tag(type: KClass<T>): T? {
    @Suppress("UNCHECKED_CAST") return tags[type] as T?
  }

  /** Returns the tag attached with [T] as a key, or null if no tag is attached with that key. */
  public inline fun <reified T : Any> tag(): T? = tag(T::class)

  /** Returns a new [Builder] representation of this request. */
  public fun toBuilder(): Builder = Builder(this)

  override fun toString(): String {
    return "ScreenRequest(screen=$screen, tags=${tags.size})"
  }

  public class Builder(
    public val screen: Screen,
  ) {
    internal constructor(screenRequest: ScreenRequest) : this(screenRequest.screen) {
      tags.putAll(screenRequest.tags)
    }

    internal val tags: MutableMap<KClass<*>, Any> = mutableMapOf()

    /**
     * Attaches [tag] to the request using [T] as a key. Tags can be read from a request using
     * [ScreenRequest.tag]. Use `null` to remove any existing tag assigned for [T].
     *
     * Use this API to attach metadata, debugging, or other application data to a spec so that you
     * may read it in other APIs or callbacks.
     */
    public inline fun <reified T : Any> tag(tag: Any?): Builder = tag(T::class, tag)

    /**
     * Attaches [tag] to the request using [type] as a key. Tags can be read from a request using
     * [ScreenRequest.tag]. Use `null` to remove any existing tag assigned for [type].
     *
     * Use this API to attach metadata, debugging, or other application data to a spec so that you
     * may read it in other APIs or callbacks.
     */
    public fun tag(type: KClass<*>, tag: Any?): Builder = apply {
      if (tag == null) {
        this.tags.remove(type)
      } else {
        this.tags[type] = tag
      }
    }

    public fun build(): ScreenRequest = ScreenRequest(this)
  }
}

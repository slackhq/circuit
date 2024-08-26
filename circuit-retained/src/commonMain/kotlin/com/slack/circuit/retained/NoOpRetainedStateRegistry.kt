// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

/** A no-op [RetainedStateRegistry]. Should generally only be used for testing and previews. */
public object NoOpRetainedStateRegistry : RetainedStateRegistry {
  override fun consumeValue(key: String): Any? = null

  override fun registerValue(
    key: String,
    valueProvider: RetainedValueProvider,
  ): RetainedStateRegistry.Entry = NoOpEntry

  override fun saveAll() {}

  override fun saveValue(key: String) {}

  override fun forgetUnclaimedValues() {}

  private object NoOpEntry : RetainedStateRegistry.Entry {
    override fun unregister() {}
  }
}

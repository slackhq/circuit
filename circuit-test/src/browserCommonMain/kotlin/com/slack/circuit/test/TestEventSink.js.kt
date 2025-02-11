// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

import com.slack.circuit.runtime.CircuitUiEvent

public actual sealed interface BaseTestEventSinkType<UiEvent> {
  public actual operator fun invoke(event: UiEvent)
}

/**
 * A helper function for creating a function wrapper around this [TestEventSink] for use in tests as
 * an event sink function. We have to do this workaround in JS due to Kotlin/JS not allowing
 * function type extension directly.
 */
public fun <UiEvent : CircuitUiEvent> TestEventSink<UiEvent>.asEventSinkFunction():
  (UiEvent) -> Unit = this::invoke

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.stateprototyping

import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.CircuitUiEvent

@Stable
interface EventSink<UiEvent : CircuitUiEvent> {
  fun send(event: UiEvent)
}

fun <UiEvent : CircuitUiEvent> fakeSink(): EventSink<UiEvent> =
  object : EventSink<UiEvent> {
    override fun send(event: UiEvent) {
      // Do nothing
    }
  }

/**
 * Creates an [EventSink] that calls the given [body] when [EventSink.send] is called.
 *
 * Note this inline function + [InlineEventSink] return type are a bit of bytecode trickery to avoid
 * creating a new class for every lambda passed to this function. The end result should be that the
 * lambda is inlined directly to the field in the implementing class and the inlined
 * [EventSink.send] method impl is inlined directly as well to call it.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <UiEvent : CircuitUiEvent> eventSink(
  noinline body: (UiEvent) -> Unit
): InlineEventSink<UiEvent> = InlineEventSink(body)

/** @see eventSink */
@JvmInline
value class InlineEventSink<UiEvent : CircuitUiEvent>
@PublishedApi
internal constructor(private val body: (UiEvent) -> Unit) : EventSink<UiEvent> {
  override fun send(event: UiEvent) {
    body(event)
  }
}

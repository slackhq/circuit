// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.slack.circuit.foundation.internal.PredictiveBack.Event
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.launch

@InternalCircuitApi
@Composable
public fun PredictiveBackEventHandler(
  isEnabled: Boolean = true,
  onBackProgress: suspend (Float, Offset) -> Unit,
  onBackCancelled: suspend () -> Unit,
  onBackCompleted: suspend () -> Unit,
) {
  val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return
  val scope = rememberStableCoroutineScope()
  val handler = remember(dispatcher) { PredictiveBackEventHandler(isEnabled, scope, dispatcher) }
  SideEffect {
    with(handler) {
      isBackEnabled = isEnabled
      onProgress = onBackProgress
      onCancelled = onBackCancelled
      onCompleted = onBackCompleted
    }
  }
}

@OptIn(InternalCircuitApi::class)
private class PredictiveBackEventHandler(
  isEnabled: Boolean,
  private val scope: CoroutineScope,
  private val dispatcher: NavigationEventDispatcher,
) :
  RememberObserver,
  NavigationEventHandler<NavigationEventInfo.None>(
    initialInfo = NavigationEventInfo.None,
    isBackEnabled = isEnabled,
  ) {

  var back: PredictiveBack? = null

  var onProgress: suspend (Float, Offset) -> Unit = { _, _ -> }
  var onCancelled: suspend () -> Unit = {}
  var onCompleted: suspend () -> Unit = {}

  override fun onBackStarted(event: NavigationEvent) {
    back?.cancel()
    back = PredictiveBack(scope) { event -> onEvent(event) }
  }

  override fun onBackProgressed(event: NavigationEvent) {
    val offset = Offset(event.touchX, event.touchY)
    val progress =
      when (event.swipeEdge) {
        NavigationEvent.EDGE_LEFT -> event.progress
        NavigationEvent.EDGE_RIGHT -> -event.progress
        else -> 0f
      }
    back?.send(Event.Progress(progress, offset))
  }

  override fun onBackCancelled() {
    back?.send(Event.Canceled)
    back = null
  }

  override fun onBackCompleted() {
    if (back == null) {
      // Can happen if the back event is just a single "onBackPressed".
      onBackStarted(NavigationEvent())
    }
    back?.send(Event.Completed)
    back = null
  }

  override fun onRemembered() {
    back = null
    dispatcher.addHandler(this)
  }

  override fun onForgotten() {
    back?.cancel()
    remove()
  }

  override fun onAbandoned() {
    onForgotten()
  }

  private suspend fun onEvent(event: Event) {
    try {
      if (isBackEnabled) {
        when (event) {
          is Event.Progress -> onProgress(event.progress, event.offset)
          is Event.Completed -> onCompleted()
          is Event.Canceled -> onCancelled()
        }
      } else {
        onCancelled()
      }
    } catch (e: CancellationException) {
      onCancelled()
      throw e
    }
  }
}

@InternalCircuitApi
public class PredictiveBack(scope: CoroutineScope, public val onEvent: suspend (Event) -> Unit) {

  private var initialTouch = Offset.Zero
  private val channel = Channel<Event>(capacity = BUFFERED)
  private val job =
    scope.launch {
      for (event in channel) {
        if (event is Event.Progress) {
          onEvent(progressAsDelta(event))
        } else {
          onEvent(event)
        }
      }
    }

  public fun send(event: Event) {
    channel.trySend(event)
  }

  public fun cancel() {
    channel.cancel()
    job.cancel()
  }

  private fun progressAsDelta(event: Event.Progress): Event.Progress {
    return if (initialTouch == Offset.Zero) {
      initialTouch = event.offset
      Event.Progress(0f, Offset.Zero)
    } else {
      Event.Progress(event.progress, event.offset - initialTouch)
    }
  }

  public sealed interface Event {
    public data class Progress(val progress: Float, val offset: Offset) : Event

    public object Completed : Event

    public object Canceled : Event
  }
}

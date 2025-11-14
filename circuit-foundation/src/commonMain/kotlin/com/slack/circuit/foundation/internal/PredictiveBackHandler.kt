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
import com.slack.circuit.foundation.internal.PredictiveNavProgress.Event
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.launch

@InternalCircuitApi
@Composable
public fun PredictiveNavEventHandler(
  isBackEnabled: Boolean = true,
  isForwardEnabled: Boolean = false,
  onProgress: suspend (PredictiveNavDirection, Float, Offset) -> Unit = { _, _, _ -> },
  onCancelled: suspend (PredictiveNavDirection) -> Unit = {},
  onCompleted: suspend (PredictiveNavDirection) -> Unit = {},
) {
  val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return
  val scope = rememberStableCoroutineScope()
  val handler =
    remember(dispatcher) {
      PredictiveNavEventHandler(isBackEnabled, isForwardEnabled, scope, dispatcher)
    }
  SideEffect {
    handler.isBackEnabled = isBackEnabled
    handler.onProgress = onProgress
    handler.onCancelled = onCancelled
    handler.onCompleted = onCompleted
  }
}

public enum class PredictiveNavDirection {
  Back,
  Forward,
}

@OptIn(InternalCircuitApi::class)
private class PredictiveNavEventHandler(
  isBackEnabled: Boolean,
  isForwardEnabled: Boolean,
  private val scope: CoroutineScope,
  private val dispatcher: NavigationEventDispatcher,
) :
  RememberObserver,
  NavigationEventHandler<NavigationEventInfo.None>(
    initialInfo = NavigationEventInfo.None,
    isBackEnabled = isBackEnabled,
    isForwardEnabled = isForwardEnabled,
  ) {

  var current: PredictiveNavProgress? = null

  var onProgress: suspend (PredictiveNavDirection, Float, Offset) -> Unit = { _, _, _ -> }
  var onCancelled: suspend (PredictiveNavDirection) -> Unit = {}
  var onCompleted: suspend (PredictiveNavDirection) -> Unit = {}

  override fun onBackStarted(event: NavigationEvent) {
    current?.cancel()
    current = PredictiveNavProgress(scope) { event -> onEvent(PredictiveNavDirection.Back, event) }
  }

  override fun onBackProgressed(event: NavigationEvent) {
    val offset = Offset(event.touchX, event.touchY)
    val progress =
      when (event.swipeEdge) {
        NavigationEvent.EDGE_LEFT -> event.progress
        NavigationEvent.EDGE_RIGHT -> -event.progress
        else -> 0f
      }
    current?.send(Event.Progress(progress, offset))
  }

  override fun onBackCancelled() {
    current?.send(Event.Canceled)
    current = null
  }

  override fun onBackCompleted() {
    if (current == null) {
      // Can happen if the back event is just a single "onBackPressed".
      onBackStarted(NavigationEvent())
    }
    current?.send(Event.Completed)
    current = null
  }

  override fun onForwardStarted(event: NavigationEvent) {
    current?.cancel()
    current = PredictiveNavProgress(scope) { event -> onEvent(PredictiveNavDirection.Back, event) }
  }

  override fun onForwardProgressed(event: NavigationEvent) {
    val offset = Offset(event.touchX, event.touchY)
    val progress =
      when (event.swipeEdge) {
        // todo No idea what to do here
        NavigationEvent.EDGE_LEFT -> -event.progress
        NavigationEvent.EDGE_RIGHT -> event.progress
        else -> 0f
      }
    current?.send(Event.Progress(progress, offset))
  }

  override fun onForwardCancelled() {
    current?.send(Event.Canceled)
    current = null
  }

  override fun onForwardCompleted() {
    if (current == null) {
      // Can happen if the back event is just a single "onBackPressed".
      onBackStarted(NavigationEvent())
    }
    current?.send(Event.Completed)
    current = null
  }

  override fun onRemembered() {
    current = null
    dispatcher.addHandler(this)
  }

  override fun onForgotten() {
    current?.cancel()
    remove()
  }

  override fun onAbandoned() {
    onForgotten()
  }

  private suspend fun onEvent(direction: PredictiveNavDirection, event: Event) {
    try {
      val isEnabled =
        when (direction) {
          PredictiveNavDirection.Back -> isBackEnabled
          PredictiveNavDirection.Forward -> isForwardEnabled
        }
      if (isEnabled) {
        when (event) {
          is Event.Progress -> onProgress(direction, event.progress, event.offset)
          is Event.Completed -> onCompleted(direction)
          is Event.Canceled -> onCancelled(direction)
        }
      } else {
        onCancelled(direction)
      }
    } catch (e: CancellationException) {
      onCancelled(direction)
      throw e
    }
  }
}

@InternalCircuitApi
public class PredictiveNavProgress(
  scope: CoroutineScope,
  public val onEvent: suspend (Event) -> Unit,
) {

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

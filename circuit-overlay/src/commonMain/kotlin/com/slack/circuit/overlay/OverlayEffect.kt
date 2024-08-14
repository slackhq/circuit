// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.slack.circuit.overlay.OverlayState.UNAVAILABLE
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope

/**
 * Calls the given [block] with the current [OverlayHost] if overlays are available. If overlays are
 * [unavailable][OverlayState.UNAVAILABLE], the [fallback] composable will be called instead.
 *
 * The [block] is executed inside a [LaunchedEffect] with the given [keys] and passes the current
 * [OverlayHost] as the only parameter. Callers should call [OverlayHost.show] within this block.
 *
 * @see [OverlayHost.show]
 */
@Composable
public fun OverlayEffect(
  vararg keys: Any?,
  fallback: (@Composable () -> Unit)? = null,
  block: suspend OverlayScope.() -> Unit,
) {
  if (LocalOverlayState.current == UNAVAILABLE) {
    fallback?.invoke()
  } else {
    val host = LocalOverlayHost.current
    LaunchedEffect(host, *keys) {
      val scope = OverlayScopeImpl(host, coroutineContext)
      scope.block()
    }
  }
}

@OptIn(ReadOnlyOverlayApi::class) public interface OverlayScope : OverlayHost, CoroutineScope

@OptIn(ReadOnlyOverlayApi::class)
private class OverlayScopeImpl(host: OverlayHost, override val coroutineContext: CoroutineContext) :
  OverlayScope, OverlayHost by host

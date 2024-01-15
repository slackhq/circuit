package com.slack.circuit.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import com.slack.circuit.overlay.OverlayState.UNAVAILABLE
import kotlinx.coroutines.CoroutineScope

/**
 * Calls the given [block] with the current [OverlayHost] if overlays are available. If overlays
 * are [unavailable][OverlayState.UNAVAILABLE], the [fallback] composable will be called instead.
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
  block: suspend CoroutineScope.(OverlayHost) -> Unit
) {
  if (LocalOverlayState.current == UNAVAILABLE) {
    fallback?.invoke()
  } else {
    val host = LocalOverlayHost.current
    key(host) {
      LaunchedEffect(
        keys = keys,
      ) { block(host) }
    }
  }
}
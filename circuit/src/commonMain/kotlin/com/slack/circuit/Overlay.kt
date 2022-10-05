/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** TODO doc this */
@Stable
public interface OverlayHost {
  public val currentOverlayData: OverlayHostData<Any>?
  public suspend fun <T : Any> show(overlay: Overlay<T>): T
}

/** TODO doc this */
@Composable public fun rememberOverlayHost(): OverlayHost = remember { OverlayHostImpl() }

private class OverlayHostImpl : OverlayHost {
  /**
   * Only one [Overlay] can be shown at a time. Since a suspending Mutex is a fair queue, this
   * manages our message queue and we don't have to maintain one.
   */
  private val mutex = Mutex()

  override var currentOverlayData: OverlayHostData<Any>? by mutableStateOf(null)
    private set

  override suspend fun <T : Any> show(overlay: Overlay<T>): T =
    mutex.withLock {
      try {
        return suspendCancellableCoroutine { continuation ->
          @Suppress("UNCHECKED_CAST")
          currentOverlayData =
            OverlayHostDataImpl(
              overlay as Overlay<Any>,
              continuation as CancellableContinuation<Any>
            )
        }
      } finally {
        currentOverlayData = null
      }
    }
}

/** TODO doc this */
@Stable
public interface OverlayHostData<T : Any> {
  public val overlay: Overlay<T>
  public fun finish(result: T)
}

private class OverlayHostDataImpl<T : Any>(
  override val overlay: Overlay<T>,
  private val continuation: CancellableContinuation<T>,
) : OverlayHostData<T> {
  override fun finish(result: T) {
    if (continuation.isActive) continuation.resume(result)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as OverlayHostDataImpl<*>

    if (overlay != other.overlay) return false
    if (continuation != other.continuation) return false

    return true
  }

  override fun hashCode(): Int {
    var result = overlay.hashCode()
    result = 31 * result + continuation.hashCode()
    return result
  }
}

/** TODO doc this */
@Stable
public fun interface OverlayNavigator<T : Any> {
  public fun finish(result: T)
}

/** TODO doc this */
public interface Overlay<T : Any> {
  @Composable public fun Content(navigator: OverlayNavigator<T>)
}

/** TODO doc this */
public val LocalOverlayHost: ProvidableCompositionLocal<OverlayHost> = compositionLocalOf {
  error("No OverlayHost provided")
}

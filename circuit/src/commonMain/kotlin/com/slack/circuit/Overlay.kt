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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * An OverlayHost can be used [show] [overlays][Overlay] with content on top of other content. This
 * is useful for one-off request/result flows such as:
 * - Bottom sheets
 * - Modals/dialogs
 * - Tooltips
 * - Full-screen takeover prompts
 * - etc.
 *
 * The suspend [show] function is generically typed and can be suspended on to await the result of
 * whatever overlay was launched.
 *
 * [currentOverlayData] is read-only and can be used to observe the current overlay data. This is
 * generally intended to be used wherever the [OverlayHost] is provided via [LocalOverlayHost].
 *
 * In Android, this can be managed via the `ContentWithOverlays` composable function.
 *
 * To avoid accidentally capturing unnecessary state, it's recommended to create extension functions
 * on `OverlayHost` that call [show] with the appropriate overlay and result type.
 *
 * ```
 * private suspend fun OverlayHost.confirm(message: String): ConfirmationResult {
 *   return show(ConfirmationOverlay(message))
 * }
 * ```
 */
@Stable
public interface OverlayHost {
  /**
   * The current [OverlayHostData] or null if no overlay is currently showing.
   *
   * ```
   * val overlayHostData by rememberUpdatedState(overlayHost.currentOverlayData)
   * Box(modifier) {
   *   content() // The regular content
   *   key(overlayHostData) { overlayHostData?.let { data -> data.overlay.Content(data::finish) } }
   * }
   * ```
   */
  public val currentOverlayData: OverlayHostData<Any>?

  /**
   * Shows the given [overlay] and suspends until the overlay is finished with a [Result]. The
   * overlay should _always_ signal a result (even if it's just something like `Result.Dismissed`)
   * so that the [OverlayHost] can properly clear its [currentOverlayData].
   *
   * If no data is needed in a result, use [Unit] for the result type.
   *
   * This function should only be called from UI contexts and _not_ presenters, as overlays are a UI
   * concern.
   */
//  public suspend fun <Result : Any> show(overlay: Overlay<Result>): Result
  public suspend fun <Result : Any> show(overlay: Overlay<Result>): Flow<Result>
}

/** Returns a remembered an [OverlayHost] that can be used to show overlays. */
@Composable public fun rememberOverlayHost(): OverlayHost = remember { OverlayHostImpl() }

private class OverlayHostImpl : OverlayHost {
  /**
   * Only one [Overlay] can be shown at a time. Since a suspending Mutex is a fair queue, this
   * manages our message queue and we don't have to maintain one.
   */
  private val mutex = Mutex()

  override var currentOverlayData: OverlayHostData<Any>? by mutableStateOf(null)
    private set

//  override suspend fun <T : Any> show(overlay: Overlay<T>): T =
//    mutex.withLock {
//      try {
//        return suspendCancellableCoroutine { continuation ->
//          @Suppress("UNCHECKED_CAST")
//          currentOverlayData =
//            OverlayHostDataImpl(
//              overlay as Overlay<Any>,
//              continuation as CancellableContinuation<Any>
//            )
//        }
//      } finally {
//        currentOverlayData = null
//      }
//    }
  override suspend fun <Result : Any> show(overlay: Overlay<Result>): Flow<Result> {
    return mutex.withLock {
      flow {
        @Suppress("UNCHECKED_CAST")
        currentOverlayData = OverlayHostDataImpl(
            overlay as Overlay<Any>,
            this as FlowCollector<Any>
        )
      }
    }
  }
}

/**
 * Data managed by an [OverlayHost] to track the current overlay state. This should rarely be
 * implemented by consumers!
 */
//@Stable
//public interface OverlayHostData<Result : Any> {
//  /** The [Overlay] that is currently being shown. Read-only. */
//  public val overlay: Overlay<Result>
//
//  /**
//   * Invoked to finish the current overlay with the given [result]. This should be called by
//   * wherever the [OverlayHost] is being managed.
//   */
//  public fun finish(result: Result)
//}

@Stable
public interface OverlayHostData<Result : Any> : FlowCollector<Result> {
  /** The [Overlay] that is currently being shown. Read-only. */
  public val overlay: Overlay<Result>
}

//private class OverlayHostDataImpl<T : Any>(
//  override val overlay: Overlay<T>,
//  private val continuation: CancellableContinuation<T>,
//) : OverlayHostData<T> {
//  override fun finish(result: T) {
//    if (continuation.isActive) continuation.resume(result)
//  }
//
//  override fun equals(other: Any?): Boolean {
//    if (this === other) return true
//    if (javaClass != other?.javaClass) return false
//
//    other as OverlayHostDataImpl<*>
//
//    if (overlay != other.overlay) return false
//    if (continuation != other.continuation) return false
//
//    return true
//  }
//
//  override fun hashCode(): Int {
//    var result = overlay.hashCode()
//    result = 31 * result + continuation.hashCode()
//    return result
//  }
//}

private class OverlayHostDataImpl<T : Any>(
    override val overlay: Overlay<T>,
    private val collector: FlowCollector<T>
) : OverlayHostData<T>, FlowCollector<T> by collector {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as OverlayHostDataImpl<*>

    if (overlay != other.overlay) return false
    if (collector != other.collector) return false

    return true
  }

  override fun hashCode(): Int {
    var result = overlay.hashCode()
    result = 31 * result + collector.hashCode()
    return result
  }
}

/**
 * An [OverlayNavigator] is a simple API offered to [overlays][Overlay] to call [finish] with a
 * result when they are done.
 */
@Stable
public interface OverlayNavigator<Result : Any> {
//  /** Called by the [Overlay] with a [result] it's done. */
//  public fun finish(result: Result)

  public fun emit(result: Result)
}

/**
 * An [Overlay] is composable content that can be shown on top of other content via an [OverlayHost]
 * . Overlays are typically used for one-off request/result flows and should not usually attempt to
 * do any sort of external navigation or make any assumptions about the state of the app. They
 * should only emit a [Result] to the given `OverlayNavigator` when they are done.
 *
 * For common overlays, it's useful to create a common `Overlay` subtype that can be reused. For
 * example: `BottomSheetOverlay`, `ModalOverlay`, `TooltipOverlay`, etc.
 */
public interface Overlay<Result : Any> {
  @Composable public fun Content(navigator: OverlayNavigator<Result>)
}

/** A [ProvidableCompositionLocal] to expose the current [OverlayHost] in the composition tree. */
public val LocalOverlayHost: ProvidableCompositionLocal<OverlayHost> = compositionLocalOf {
  error("No OverlayHost provided")
}

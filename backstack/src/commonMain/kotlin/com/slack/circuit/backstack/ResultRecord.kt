// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.backstack

import com.slack.circuit.runtime.screen.PopResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

// TODO Maybe this moves into the backstack impl and let it manage it instead?
public interface ResultRecord {
  @InternalBackStackApi public fun setResultKey(key: String)

  @InternalBackStackApi public fun clearResultKey()

  @InternalBackStackApi public fun updatePendingResult(result: PopResult)

  @InternalBackStackApi public fun clearPendingResult()

  public suspend fun awaitResult(key: String): PopResult?
}

internal class ResultRecordImpl : ResultRecord {
  /**
   * A [Channel] of pending results. Note we use this instead of a [CompletableDeferred] because we
   * may push and pop back to a given record multiple times, and thus need to be able to push and
   * receive multiple results.
   *
   * TODO what's the right behavior here?
   * - Capacity 1 + overflow drop oldest: we only care about the most recent result
   * - Conflated: only take one result until the presenter takes it.
   */
  private val pendingResultChannel =
    Channel<PopResult>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

  internal var pendingResultKey: String? = null

  internal var _pendingResult: PopResult? = null

  @InternalBackStackApi
  override fun setResultKey(key: String) {
    pendingResultKey = key
  }

  @InternalBackStackApi
  override fun clearResultKey() {
    pendingResultKey = null
  }

  @InternalBackStackApi
  override fun updatePendingResult(result: PopResult) {
    _pendingResult = result
    pendingResultChannel.trySend(result)
  }

  @InternalBackStackApi
  override fun clearPendingResult() {
    _pendingResult = null
    // TODO do we clear the channel's value here too?
  }

  override suspend fun awaitResult(key: String): PopResult? {
    return if (key == pendingResultKey) {
      pendingResultChannel.receive()
    } else {
      null
    }
  }
}

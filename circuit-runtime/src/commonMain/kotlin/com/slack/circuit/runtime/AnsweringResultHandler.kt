// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime

import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.screen.PopResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

/**
 * Handles result passing between navigation records.
 *
 * This class maintains pending results for each record and tracks which result key each record is
 * expecting. Foundation's `rememberAnsweringResultHandler` adds saveable-state persistence.
 */
@ExperimentalCircuitApi
@Stable
public class AnsweringResultHandler {
  private val recordStates = mutableMapOf<String, RecordResultState>()

  /** Prepares [recordKey] to receive a result tagged with [resultKey]. */
  public fun prepareForResult(recordKey: String, resultKey: String) {
    val state = recordStates.getOrPut(recordKey) { RecordResultState() }
    state.resultKey = resultKey
    state.readResult()
  }

  /** Returns whether [recordKey] is expecting a result. */
  public fun expectingResult(recordKey: String): Boolean {
    return recordStates[recordKey]?.resultKey != null
  }

  /** Sends [result] to [recordKey]. */
  public fun sendResult(recordKey: String, result: PopResult) {
    recordStates[recordKey]?.sendResult(result)
  }

  /** Awaits the result tagged with [resultKey] for [recordKey], or returns null for a stale key. */
  public suspend fun awaitResult(recordKey: String, resultKey: String): PopResult? {
    val state = recordStates[recordKey] ?: return null
    return if (resultKey == state.resultKey) {
      state.resultKey = null
      state.resultChannel.receive()
    } else {
      null
    }
  }

  /** Visits each expected result without consuming its pending result. */
  @InternalCircuitApi
  public fun forEachExpectedResult(
    block: (recordKey: String, resultKey: String, pendingResult: PopResult?) -> Unit
  ) {
    for ((recordKey, state) in recordStates) {
      val resultKey = state.resultKey ?: continue
      block(recordKey, resultKey, state.peekResult())
    }
  }

  private class RecordResultState {
    val resultChannel =
      Channel<PopResult>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    var resultKey: String? = null

    fun readResult() = resultChannel.tryReceive().getOrNull()

    fun peekResult() = resultChannel.tryReceive().getOrNull()?.also { resultChannel.trySend(it) }

    fun sendResult(result: PopResult) {
      resultChannel.trySend(result)
    }
  }
}

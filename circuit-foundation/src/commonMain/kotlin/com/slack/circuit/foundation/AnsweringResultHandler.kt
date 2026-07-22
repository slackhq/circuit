// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.slack.circuit.runtime.AnsweringResultHandler as RuntimeAnsweringResultHandler
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.LocalCircuitSaver
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.restorePopResult

@Deprecated(
  "Moved to com.slack.circuit.runtime.",
  ReplaceWith(
    "AnsweringResultHandler",
    "com.slack.circuit.runtime.AnsweringResultHandler",
  ),
)
@ExperimentalCircuitApi
public typealias AnsweringResultHandler = RuntimeAnsweringResultHandler

/**
 * Returns a saveable [AnsweringResultHandler] for use with [NavigableCircuitContent].
 *
 * @param circuitSaver the [CircuitSaver] used to persist results, defaulting to
 *   [LocalCircuitSaver].
 */
@ExperimentalCircuitApi
@Composable
public fun rememberAnsweringResultHandler(
  circuitSaver: CircuitSaver = LocalCircuitSaver.current
): RuntimeAnsweringResultHandler =
  rememberSaveable(saver = answeringResultHandlerSaver(circuitSaver)) {
    RuntimeAnsweringResultHandler()
  }

@OptIn(ExperimentalCircuitApi::class, InternalCircuitApi::class)
internal fun answeringResultHandlerSaver(
  circuitSaver: CircuitSaver
): Saver<RuntimeAnsweringResultHandler, Any> =
  mapSaver(
    save = { handler ->
      buildMap {
        handler.forEachExpectedResult { recordKey, resultKey, pendingResult ->
          val savedPendingResult = pendingResult?.let(circuitSaver::save)
          put(recordKey, listOf(resultKey, savedPendingResult, pendingResult != null))
        }
      }
    },
    restore = { map ->
      RuntimeAnsweringResultHandler().apply {
        for ((recordKey, value) in map) {
          val values = value as List<Any?>
          val resultKey = values.getOrNull(0) as? String ?: continue
          val savedPendingResult = values.getOrNull(1)
          // The legacy two-element form could only distinguish pending results by payload.
          val hadPendingResult =
            if (values.size >= 3) values[2] as Boolean else savedPendingResult != null

          if (!hadPendingResult) {
            prepareForResult(recordKey, resultKey)
            continue
          }

          val pendingResult =
            savedPendingResult?.let { circuitSaver.restorePopResult<PopResult>(it) } ?: continue
          // Order matters here because prepareForResult() clears the buffer.
          prepareForResult(recordKey, resultKey)
          sendResult(recordKey, pendingResult)
        }
      }
    },
  )

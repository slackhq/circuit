package com.slack.circuit.backstack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.slack.circuit.runtime.screen.PopResult
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

@Composable
public fun rememberResultHandler(): ResultHandler =
  rememberSaveable(saver = ResultHandler.Saver) { ResultHandler() }

/**
 * Handles result passing between records in a back stack.
 *
 * This class manages the state needed for records to send and receive [PopResult]s when navigating
 * between screens. It maintains channels for each record key and tracks which results each record
 * is expecting.
 */
public class ResultHandler {
  private val recordStates = mutableMapOf<String, RecordResultState>()

  /**
   * Prepares a record to receive a result with the given key.
   *
   * @param recordKey The unique key identifying the record.
   * @param resultKey The key that will be used to tag the result.
   */
  public fun prepareForResult(recordKey: String, resultKey: String) {
    val state = recordStates.getOrPut(recordKey) { RecordResultState() }
    state.resultKey = resultKey
    // Clear any pending result when preparing for a new one
    state.readResult()
  }

  /**
   * Checks if a record is expecting a result.
   *
   * @param recordKey The unique key identifying the record.
   * @return `true` if the record is expecting a result, `false` otherwise.
   */
  public fun expectingResult(recordKey: String): Boolean {
    return recordStates[recordKey]?.resultKey != null
  }

  /**
   * Sends a result to a record.
   *
   * @param recordKey The unique key identifying the record that should receive the result.
   * @param result The result to send.
   */
  public fun sendResult(recordKey: String, result: PopResult) {
    recordStates[recordKey]?.sendResult(result)
  }

  /**
   * Awaits a result for a record. This suspends until a result is received or returns null if the
   * result key doesn't match the expected key.
   *
   * @param recordKey The unique key identifying the record.
   * @param resultKey The key that was used to tag the result.
   * @return The [PopResult] if one was received with the matching key, null otherwise.
   */
  public suspend fun awaitResult(recordKey: String, resultKey: String): PopResult? {
    val state = recordStates[recordKey] ?: return null
    return if (resultKey == state.resultKey) {
      state.resultKey = null
      state.resultChannel.receive()
    } else {
      null
    }
  }

  private class RecordResultState {
    /**
     * A [Channel] of pending results. Note we use this instead of CompletableDeferred because we
     * may push and pop back to a given record multiple times, and thus need to be able to push and
     * receive multiple results. We use [BufferOverflow.DROP_OLDEST] to ensure we only care about
     * the most recent result.
     */
    val resultChannel =
      Channel<PopResult>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    var resultKey: String? = null

    fun readResult() = resultChannel.tryReceive().getOrNull()

    fun sendResult(result: PopResult) {
      resultChannel.trySend(result)
    }
  }

  public companion object {
    @Suppress("UNCHECKED_CAST")
    public val Saver: Saver<ResultHandler, Any> =
      mapSaver(
        save = { handler ->
          buildMap {
            for ((recordKey, state) in handler.recordStates) {
              put(recordKey, state.resultKey to state.readResult())
            }
          }
        },
        restore = { map ->
          ResultHandler().apply {
            for ((recordKey, value) in map) {
              val (resultKey, pendingResult) = value as Pair<String?, PopResult?>
              // NOTE order matters here, prepareForResult() clears the buffer
              resultKey?.let { prepareForResult(recordKey, it) }
              pendingResult?.let { sendResult(recordKey, it) }
            }
          }
        },
      )
  }
}

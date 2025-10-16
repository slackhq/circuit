package com.slack.circuit.backstack

import androidx.compose.runtime.snapshots.Snapshot
import com.slack.circuit.backstack.BackStack.Record
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

public interface AnsweringBackStack<R : Record> : BackStack<R> {
  /**
   * Awaits a result for a record. This suspends until a result is received or returns null if the
   * result key doesn't match the expected key.
   *
   * @param recordKey The unique key identifying the record.
   * @param resultKey The key that was used to tag the result.
   * @return The [PopResult] if one was received with the matching key, null otherwise.
   */
  public suspend fun awaitResult(recordKey: String, resultKey: String): PopResult?

  /**
   * Checks if a record is expecting a result.
   *
   * @param recordKey The unique key identifying the record.
   * @return `true` if the record is expecting a result, `false` otherwise.
   */
  public fun expectingResult(recordKey: String): Boolean
}

public class DelegatingAnsweringBackStack<R : Record>(
  private val delegate: BackStack<R>,
  private val resultHandler: ResultHandler = ResultHandler(),
) : AnsweringBackStack<R>, BackStack<R> by delegate {

  override fun push(screen: Screen, resultKey: String?): Boolean =
    push(resultKey) { delegate.push(screen, resultKey) }

  public override fun push(record: R, resultKey: String?): Boolean =
    push(resultKey) { delegate.push(record, resultKey) }

  private fun push(resultKey: String?, delegatePush: () -> Boolean): Boolean {
    val previousTopRecord = Snapshot.withoutReadObservation { topRecord }
    val success = delegatePush()
    if (success) {
      // Clear the cached pending result from the previous top record
      if (previousTopRecord != null && resultKey != null) {
        resultHandler.prepareForResult(previousTopRecord.key, resultKey)
      }
    }
    return success
  }

  override fun pop(result: PopResult?): R? {
    // Run in a snapshot to ensure the sendResult doesn't get missed.
    return Snapshot.withMutableSnapshot {
      val popped = delegate.pop(result)
      if (result != null) {
        // Send the pending result to our new top record, but only if it's expecting one
        topRecord?.apply {
          if (resultHandler.expectingResult(key)) {
            resultHandler.sendResult(key, result)
          }
        }
      }
      popped
    }
  }

  override fun expectingResult(recordKey: String): Boolean {
    return resultHandler.expectingResult(recordKey)
  }

  override suspend fun awaitResult(recordKey: String, resultKey: String): PopResult? {
    return resultHandler.awaitResult(recordKey, resultKey)
  }
}

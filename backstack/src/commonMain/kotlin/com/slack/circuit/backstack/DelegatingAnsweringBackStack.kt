package com.slack.circuit.backstack

import androidx.compose.runtime.snapshots.Snapshot
import com.slack.circuit.backstack.BackStack.Record
import com.slack.circuit.runtime.screen.PopResult

public class DelegatingAnsweringBackStack<R : Record>(
  private val delegate: BackStack<R>,
  private val resultHandler: ResultHandler = ResultHandler(),
) : BackStack<R> by delegate {

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
}

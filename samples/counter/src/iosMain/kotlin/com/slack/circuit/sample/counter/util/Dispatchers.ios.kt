package com.slack.circuit.sample.counter.util

import kotlinx.coroutines.*
import platform.darwin.*
import kotlin.coroutines.CoroutineContext

actual val Dispatchers.IO: CoroutineDispatcher
  get() = IODispatcher

// Borrowed from KotlinConf app
// https://github.com/JetBrains/kotlinconf-app/blob/642404f3454d384be966c34d6b254b195e8d2892/shared/src/iosMain/kotlin/org/jetbrains/kotlinconf/utils/Dispatchers.ios.kt
@OptIn(InternalCoroutinesApi::class)
private object IODispatcher : CoroutineDispatcher(), Delay {
  override fun dispatch(context: CoroutineContext, block: Runnable) {
    val queue = dispatch_get_main_queue()
    dispatch_async(queue) {
      block.run()
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun scheduleResumeAfterDelay(
    timeMillis: Long,
    continuation: CancellableContinuation<Unit>
  ) {
    val queue = dispatch_get_main_queue()

    val time = dispatch_time(DISPATCH_TIME_NOW, (timeMillis * NSEC_PER_MSEC.toLong()))
    dispatch_after(time, queue) {
      with(continuation) {
        resumeUndispatched(Unit)
      }
    }
  }
}
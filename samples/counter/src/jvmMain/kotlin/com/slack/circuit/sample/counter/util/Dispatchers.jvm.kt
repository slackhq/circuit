package com.slack.circuit.sample.counter.util;

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val Dispatchers.IO: CoroutineDispatcher
  get() = Dispatchers.IO
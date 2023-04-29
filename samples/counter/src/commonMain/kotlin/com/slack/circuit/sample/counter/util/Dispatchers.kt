package com.slack.circuit.sample.counter.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

expect val Dispatchers.IO: CoroutineDispatcher
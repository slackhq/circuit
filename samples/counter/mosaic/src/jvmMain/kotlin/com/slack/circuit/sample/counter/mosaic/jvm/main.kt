// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:JvmName("Main")

package com.slack.circuit.sample.counter.mosaic.jvm

import com.slack.circuit.sample.counter.mosaic.runCounterScreen

suspend fun main(args: Array<String>) {
  // TODO change to true once circuit's impl is working
  val useCircuit = "--use-circuit" in args
  runCounterScreen(useCircuit)
}

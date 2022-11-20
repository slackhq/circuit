// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import com.slack.circuit.backstack.SaveableBackStack

public fun SaveableBackStack.push(screen: Screen) {
  push(
    SaveableBackStack.Record(
      route = screen.javaClass.simpleName,
      args = mapOf("screen" to screen),
      key = screen.hashCode().toString()
    )
  )
}

public fun SaveableBackStack.setScreenResult(result: ScreenResult?) {
  if (size == 1) return // TODO should we throw here instead??

  val ancestor = get(1)
  val newArgs = ancestor.args.plus("result" to result)

  set(1, ancestor.copy(args = newArgs))
}

public fun SaveableBackStack.processPendingScreenResult() {
  val curRecord = topRecord ?: return
  val pendingResult = curRecord.screenResult ?: return
  val curScreen = curRecord.screen

  val newScreen = curScreen.update(pendingResult)
  val newArgs = curRecord.args
    .toMutableMap()
    .apply {
      remove("result")
      put("screen", newScreen)
    }
  val newRecord = curRecord.copy(args = newArgs)

  // replace the current top with the updated record
  set(0, newRecord)
}

public val SaveableBackStack.Record.screen: Screen
  get() = args.getValue("screen") as Screen

private val SaveableBackStack.Record.screenResult: ScreenResult?
  get() = args["result"] as ScreenResult?
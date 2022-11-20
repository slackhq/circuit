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

  val index = size - 2
  val ancestor = get(index)
  val newArgs = ancestor.args.plus("result" to result)

  set(index, ancestor.copy(args = newArgs))
}

public val SaveableBackStack.Record.screen: Screen
  get() = args.getValue("screen") as Screen

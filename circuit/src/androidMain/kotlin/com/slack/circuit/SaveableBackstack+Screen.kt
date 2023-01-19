// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import com.slack.circuit.backstack.SaveableBackStack

public fun SaveableBackStack.push(screen: Screen) {
  push(
    SaveableBackStack.Record(
      route = (screen as? NavigableScreen)?.route ?: "",
      args = mapOf("screen" to screen),
      key = screen.hashCode().toString()
    )
  )
}

public val SaveableBackStack.Record.screen: Screen
  get() = args.getValue("screen") as Screen

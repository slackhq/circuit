// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.runtime.screen.Screen

@Deprecated(
  "Use SaveableBackStack.push instead",
  ReplaceWith("this.push(screen)", "com.slack.circuit.backstack.push"),
  level = DeprecationLevel.ERROR
)
public fun SaveableBackStack.push(screen: Screen) {
  push(SaveableBackStack.Record(screen = screen))
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use Record.screen instead", level = DeprecationLevel.HIDDEN)
public val SaveableBackStack.Record.screen: Screen
  get() = args.getValue("screen") as Screen

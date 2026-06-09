// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph

@DependencyGraph(scope = AppScope::class)
actual interface InboxAppGraph : InboxCommonAppGraph {
  actual companion object {
    actual fun create(): InboxAppGraph = createGraph<InboxAppGraph>()
  }
}

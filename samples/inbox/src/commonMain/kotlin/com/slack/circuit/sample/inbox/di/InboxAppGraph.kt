// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.di

// expect/actual due to https://github.com/google/ksp/issues/929
expect interface InboxAppGraph : InboxCommonAppGraph {
  companion object {
    fun create(): InboxAppGraph
  }
}

// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

// expect/actual due to https://github.com/google/ksp/issues/929
expect interface AppGraph : CommonAppGraph {
  companion object {
    fun create(): AppGraph
  }
}

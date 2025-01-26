// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import dev.zacsweers.lattice.DependencyGraph
import dev.zacsweers.lattice.SingleIn

@DependencyGraph(
  scope = AppScope::class,
)
@SingleIn(AppScope::class)
interface AppComponent : CommonAppComponent

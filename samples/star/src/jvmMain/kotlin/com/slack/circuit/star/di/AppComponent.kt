// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.SingleIn

@DependencyGraph(
  scope = AppScope::class,
)
@SingleIn(AppScope::class)
interface AppComponent : CommonAppComponent

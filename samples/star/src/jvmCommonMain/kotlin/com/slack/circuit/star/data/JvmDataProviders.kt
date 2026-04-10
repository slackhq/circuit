// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import okio.FileSystem

@ContributesTo(AppScope::class)
interface JvmDataProviders {
  @Provides @SingleIn(AppScope::class) fun provideFileSystem(): FileSystem = FileSystem.SYSTEM
}

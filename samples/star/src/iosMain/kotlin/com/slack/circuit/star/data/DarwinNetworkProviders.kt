// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*

@ContributesTo(AppScope::class)
interface DarwinNetworkProviders {
  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpClientEngineFactory(): HttpClientEngineFactory<*> = Darwin
}

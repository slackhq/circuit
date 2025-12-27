// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okio.FileSystem

@ContributesTo(AppScope::class)
interface CommonDataProviders {
  @Provides
  @SingleIn(AppScope::class)
  fun provideJson(): Json {
    return Json { ignoreUnknownKeys = true }
  }

  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpClient(
    httpClientEngineFactory: HttpClientEngineFactory<*>,
    json: Json,
  ): HttpClient =
    HttpClient(httpClientEngineFactory) {
      expectSuccess = true
      install(HttpRequestRetry) {
        retryOnExceptionOrServerErrors(maxRetries = 2)
        exponentialDelay()
      }
      install(ContentNegotiation) { json(json) }
    }

  @Provides @SingleIn(AppScope::class) fun provideFileSystem(): FileSystem = FileSystem.SYSTEM
}

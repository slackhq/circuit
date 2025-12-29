// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.engine.okhttp.OkHttpEngine
import okhttp3.Cache
import okhttp3.OkHttpClient

private const val MAX_CACHE_SIZE = 1024L * 1024L * 25L // 25 MB

@ContributesTo(AppScope::class)
interface JvmNetworkProviders {
  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpCache(appDirs: StarAppDirs): Cache =
    Cache(appDirs.fs, appDirs.userCache / "http_cache", MAX_CACHE_SIZE)

  @Provides
  @SingleIn(AppScope::class)
  fun provideOkHttpClient(cache: Cache): OkHttpClient = OkHttpClient.Builder().cache(cache).build()

  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpClientEngineFactory(
    okHttpClientLazy: Lazy<OkHttpClient>
  ): HttpClientEngineFactory<*> =
    object : HttpClientEngineFactory<OkHttpConfig> {
      override fun create(block: OkHttpConfig.() -> Unit): HttpClientEngine {
        return OkHttpEngine(
          OkHttpConfig().apply { preconfigured = okHttpClientLazy.value }.apply(block)
        )
      }
    }
}

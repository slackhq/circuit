// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import platform.Foundation.NSURLCache
import platform.Foundation.NSURLSessionConfiguration

@ContributesTo(AppScope::class)
interface IosNetworkProviders {
  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpClientEngineFactory(
    appDirs: StarAppDirs,
    configBlock: DarwinClientEngineConfig.() -> Unit,
  ): HttpClientEngineFactory<*> =
    object : HttpClientEngineFactory<DarwinClientEngineConfig> {
      override fun create(block: DarwinClientEngineConfig.() -> Unit): HttpClientEngine {
        return Darwin.create {
          configureSession {
            val configuration = NSURLSessionConfiguration.defaultSessionConfiguration()

            // Configure NSURLCache similar to OkHttp cache
            val cacheDirectory = CommonDataProviders.httpCacheDir(appDirs)
            appDirs.fs.createDirectories(cacheDirectory)

            val cache =
              NSURLCache(
                memoryCapacity = CommonDataProviders.MEMORY_CACHE_SIZE.toULong(),
                diskCapacity = CommonDataProviders.MAX_CACHE_SIZE.toULong(),
                diskPath = cacheDirectory.toString(),
              )
            configuration.setURLCache(cache)
          }
          block()
        }
      }
    }
}

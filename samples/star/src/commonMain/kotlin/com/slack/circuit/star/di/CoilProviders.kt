// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.network.CacheStrategy
import coil3.network.NetworkFetcher
import coil3.network.ktor3.asNetworkClient
import com.slack.circuit.star.data.StarAppDirs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient

private const val MAX_CACHE_SIZE = 1024L * 1024L * 100L // 100 MB

@ContributesTo(AppScope::class)
interface CoilProviders {
  @SingleIn(AppScope::class)
  @Provides
  fun provideImageLoader(
    @ApplicationContext platformContext: PlatformContext,
    httpClient: Lazy<HttpClient>,
    starAppDirs: StarAppDirs,
  ): ImageLoader =
    ImageLoader.Builder(platformContext)
      .diskCache {
        DiskCache.Builder()
          .directory(starAppDirs.userCache / "image_cache")
          .fileSystem(starAppDirs.fs)
          .maxSizeBytes(MAX_CACHE_SIZE)
          .build()
      }
      // Disable noisy logging
      .logger(null)
      .components {
        add(NetworkFetcher.Factory({ httpClient.value.asNetworkClient() }, CacheStrategy::DEFAULT))
      }
      .build()
}

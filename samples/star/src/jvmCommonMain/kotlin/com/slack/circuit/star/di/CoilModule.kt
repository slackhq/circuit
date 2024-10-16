// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.network.CacheStrategy
import coil3.network.NetworkFetcher
import coil3.network.ktor2.asNetworkClient
import com.slack.circuit.star.data.StarAppDirs
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.Module
import dagger.Provides
import io.ktor.client.HttpClient

private const val MAX_CACHE_SIZE = 1024L * 1024L * 100L // 100 MB

@ContributesTo(AppScope::class)
@Module
object CoilModule {
  @SingleIn(AppScope::class)
  @Provides
  fun provideImageLoader(
    @ApplicationContext platformContext: PlatformContext,
    httpClient: dagger.Lazy<HttpClient>,
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
        add(NetworkFetcher.Factory({ httpClient.get().asNetworkClient() }, CacheStrategy::DEFAULT))
      }
      .build()
}

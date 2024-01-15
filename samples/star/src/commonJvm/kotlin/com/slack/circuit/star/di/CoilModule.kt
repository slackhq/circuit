// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.NetworkFetcher
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.Module
import dagger.Provides
import io.ktor.client.HttpClient

@ContributesTo(AppScope::class)
@Module
object CoilModule {
  @SingleIn(AppScope::class)
  @Provides
  fun provideImageLoader(
    @ApplicationContext platformContext: PlatformContext,
    httpClient: dagger.Lazy<HttpClient>
  ): ImageLoader =
    ImageLoader.Builder(platformContext)
      .components { add(NetworkFetcher.Factory(lazy { httpClient.get() })) }
      .build()
}

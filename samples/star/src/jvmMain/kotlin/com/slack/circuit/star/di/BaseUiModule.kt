// Copyright (C) 2022 Slack Technologies, LLC
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
object BaseUiModule {
  @SingleIn(AppScope::class)
  @Provides
  fun provideImageLoader(httpClient: dagger.Lazy<HttpClient>): ImageLoader =
    ImageLoader.Builder(PlatformContext.INSTANCE)
      .components { add(NetworkFetcher.Factory(lazy { httpClient.get() })) }
      .build()
}

// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import android.app.Activity
import android.content.Context
import coil3.ImageLoader
import coil3.fetch.NetworkFetcher
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.Module
import dagger.Provides
import dagger.multibindings.Multibinds
import io.ktor.client.HttpClient

@ContributesTo(AppScope::class)
@Module
interface BaseUiModule {
  @Multibinds fun provideActivityProviders(): Map<Class<out Activity>, Activity>

  companion object {
    @SingleIn(AppScope::class)
    @Provides
    fun provideImageLoader(
      @ApplicationContext context: Context,
      httpClient: dagger.Lazy<HttpClient>
    ): ImageLoader = ImageLoader.Builder(context)
      .components {
        add(NetworkFetcher.Factory(lazy { httpClient.get() }))
      }
      .build()
  }
}

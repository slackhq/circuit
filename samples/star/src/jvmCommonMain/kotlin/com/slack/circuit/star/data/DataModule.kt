// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.petdetail.PetBioParser
import com.slack.eithernet.ApiResultCallAdapterFactory
import com.slack.eithernet.ApiResultConverterFactory
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.engine.okhttp.OkHttpEngine
import io.ktor.client.plugins.HttpRequestRetry
import javax.inject.Qualifier
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.FileSystem
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

private const val MAX_CACHE_SIZE = 1024L * 1024L * 25L // 25 MB

@ContributesTo(AppScope::class)
@Module
object DataModule {
  @Provides
  @SingleIn(AppScope::class)
  fun provideMoshi(): Moshi {
    return Moshi.Builder().build()
  }

  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpCache(appDirs: StarAppDirs): Cache {
    return Cache(appDirs.userCache / "http_cache", MAX_CACHE_SIZE, appDirs.fs)
  }

  @Provides
  @SingleIn(AppScope::class)
  fun provideOkHttpClient(cache: Cache): OkHttpClient {
    return OkHttpClient.Builder()
      .cache(cache)
      .addInterceptor(
        HttpLoggingInterceptor().apply {
          level = HttpLoggingInterceptor.Level.BASIC
          redactHeader("Authorization")
        }
      )
      .build()
  }

  /** Qualifier to denote that a provided type is authenticated. */
  @Qualifier annotation class Authenticated

  @Provides
  @SingleIn(AppScope::class)
  fun provideRetrofit(moshi: Moshi, okHttpClientLazy: dagger.Lazy<OkHttpClient>): Retrofit {
    return Retrofit.Builder()
      .addCallAdapterFactory(ApiResultCallAdapterFactory)
      .addConverterFactory(ApiResultConverterFactory)
      .addConverterFactory(JsoupConverter.newFactory(PetBioParser::parse))
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .baseUrl("https://api.petfinder.com/v2/")
      .callFactory { okHttpClientLazy.get().newCall(it) }
      .build()
  }

  @Authenticated
  @Provides
  @SingleIn(AppScope::class)
  fun provideAuthedOkHttpClient(
    baseRetrofit: Retrofit,
    tokenStorage: TokenStorage,
    okHttpClient: OkHttpClient,
  ): OkHttpClient {
    val authApi = baseRetrofit.create<PetfinderAuthApi>()
    val tokenManager = TokenManager(authApi, tokenStorage)
    val authInterceptor = AuthInterceptor(tokenManager)
    return okHttpClient.newBuilder().addInterceptor(authInterceptor).build()
  }

  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpClient(okHttpClientLazy: dagger.Lazy<OkHttpClient>): HttpClient {
    return HttpClient(
      object : HttpClientEngineFactory<OkHttpConfig> {
        override fun create(block: OkHttpConfig.() -> Unit): HttpClientEngine {
          return OkHttpEngine(
            OkHttpConfig().apply { preconfigured = okHttpClientLazy.get() }.apply(block)
          )
        }
      }
    ) {
      install(HttpRequestRetry) {
        retryOnExceptionOrServerErrors(maxRetries = 2)
        exponentialDelay()
      }
    }
  }

  @Provides
  @SingleIn(AppScope::class)
  fun providePetfinderApi(
    baseRetrofit: Retrofit,
    @Authenticated okHttpClientLazy: dagger.Lazy<OkHttpClient>,
  ): PetfinderApi {
    @Suppress("RemoveExplicitTypeArguments") // Necessary for R8
    return baseRetrofit
      .newBuilder()
      .callFactory { okHttpClientLazy.get().newCall(it) }
      .build()
      .create<PetfinderApi>()
  }

  @Provides @SingleIn(AppScope::class) fun provideFileSystem(): FileSystem = FileSystem.SYSTEM
}

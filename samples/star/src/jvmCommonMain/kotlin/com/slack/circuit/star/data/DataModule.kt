// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import com.slack.circuit.star.data.petfinder.PetBioParserApi
import com.slack.circuit.star.data.petfinder.PetfinderApi
import com.slack.circuit.star.di.AppScope
import dev.zacsweers.lattice.ContributesTo
import dev.zacsweers.lattice.Provides
import dev.zacsweers.lattice.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.engine.okhttp.OkHttpEngine
import javax.inject.Qualifier
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.FileSystem

private const val MAX_CACHE_SIZE = 1024L * 1024L * 25L // 25 MB

@ContributesTo(AppScope::class)
interface DataModule {
  companion object {
    @Provides @SingleIn(AppScope::class) fun provideJson(): Json = CommonDataModule.provideJson()

    @Provides
    @SingleIn(AppScope::class)
    fun provideHttpCache(appDirs: StarAppDirs): Cache =
      Cache(appDirs.fs, appDirs.userCache / "http_cache", MAX_CACHE_SIZE)

    @Provides
    @SingleIn(AppScope::class)
    fun provideOkHttpClient(cache: Cache): OkHttpClient =
      OkHttpClient.Builder()
        .cache(cache)
        .addInterceptor(
          HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
          }
        )
        .build()

    /** Qualifier to denote that a provided type is authenticated. */
    @Qualifier annotation class Authenticated

    @Provides
    @SingleIn(AppScope::class)
    fun provideTokenManager(httpClient: HttpClient, tokenStorage: TokenStorage): TokenManager =
      CommonDataModule.provideTokenManager(httpClient, tokenStorage)

    @Authenticated
    @Provides
    @SingleIn(AppScope::class)
    fun provideAuthedHttpClient(httpClient: HttpClient, tokenManager: TokenManager): HttpClient =
      CommonDataModule.provideAuthedHttpClient(httpClient, tokenManager)

    @Provides
    @SingleIn(AppScope::class)
    fun provideHttpClient(okHttpClientLazy: Lazy<OkHttpClient>, json: Json): HttpClient =
      CommonDataModule.provideHttpClient(
        object : HttpClientEngineFactory<OkHttpConfig> {
          override fun create(block: OkHttpConfig.() -> Unit): HttpClientEngine {
            return OkHttpEngine(
              OkHttpConfig().apply { preconfigured = okHttpClientLazy.value }.apply(block)
            )
          }
        },
        json,
      )

    @Provides
    @SingleIn(AppScope::class)
    fun providePetfinderApi(@Authenticated httpClient: HttpClient): PetfinderApi =
      CommonDataModule.providePetfinderApi(httpClient)

    @Provides
    @SingleIn(AppScope::class)
    fun providePetBioApi(httpClient: HttpClient): PetBioParserApi =
      CommonDataModule.providePetBioApi(httpClient)

    @Provides
    @SingleIn(AppScope::class)
    fun provideFileSystem(): FileSystem = CommonDataModule.provideFileSystem()
  }
}

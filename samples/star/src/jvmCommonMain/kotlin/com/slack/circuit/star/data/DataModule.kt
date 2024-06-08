// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import com.slack.circuit.star.data.petfinder.PetBioParserApiImpl
import com.slack.circuit.star.data.petfinder.PetfinderApiImpl
import com.slack.circuit.star.data.petfinder.PetfinderAuthApiImpl
import com.slack.circuit.star.data.petfinder.PetBioParserApi
import com.slack.circuit.star.data.petfinder.PetfinderApi
import com.slack.circuit.star.di.AppScope
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.Module
import dagger.Provides
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.engine.okhttp.OkHttpEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Qualifier
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.FileSystem

private const val MAX_CACHE_SIZE = 1024L * 1024L * 25L // 25 MB

@ContributesTo(AppScope::class)
@Module
object DataModule {
  @Provides
  @SingleIn(AppScope::class)
  fun provideJson(): Json {
    return Json { ignoreUnknownKeys = true }
  }

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
  fun provideTokenManager(httpClient: HttpClient, tokenStorage: TokenStorage): TokenManager {
    val authApi = PetfinderAuthApiImpl(httpClient)
    val tokenManager = TokenManager(authApi, tokenStorage)
    return tokenManager
  }

  @Authenticated
  @Provides
  @SingleIn(AppScope::class)
  fun provideAuthedHttpClient(httpClient: HttpClient, tokenManager: TokenManager): HttpClient =
    httpClient.config {
      install(Auth) {
        bearer {
          loadTokens { tokenManager.last() }
          refreshTokens {
            tokenManager.refreshToken()
            tokenManager.last()
          }
        }
      }
    }

  @Provides
  @SingleIn(AppScope::class)
  fun provideHttpClient(okHttpClientLazy: dagger.Lazy<OkHttpClient>, json: Json): HttpClient =
    HttpClient(
      object : HttpClientEngineFactory<OkHttpConfig> {
        override fun create(block: OkHttpConfig.() -> Unit): HttpClientEngine {
          return OkHttpEngine(
            OkHttpConfig().apply { preconfigured = okHttpClientLazy.get() }.apply(block)
          )
        }
      }
    ) {
      expectSuccess = true
      install(HttpRequestRetry) {
        retryOnExceptionOrServerErrors(maxRetries = 2)
        exponentialDelay()
      }
      install(ContentNegotiation) { json(json) }
    }

  @Provides
  @SingleIn(AppScope::class)
  fun providePetfinderApi(@Authenticated httpClient: HttpClient): PetfinderApi =
    PetfinderApiImpl(httpClient)

  @Provides
  @SingleIn(AppScope::class)
  fun providePetBioApi(httpClient: HttpClient): PetBioParserApi = PetBioParserApiImpl(httpClient)

  @Provides @SingleIn(AppScope::class) fun provideFileSystem(): FileSystem = FileSystem.SYSTEM
}

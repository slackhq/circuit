// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import com.slack.circuit.star.data.petfinder.PetBioParserApi
import com.slack.circuit.star.data.petfinder.PetBioParserApiImpl
import com.slack.circuit.star.data.petfinder.PetfinderApi
import com.slack.circuit.star.data.petfinder.PetfinderApiImpl
import com.slack.circuit.star.data.petfinder.PetfinderAuthApiImpl
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okio.FileSystem

@ContributesTo(AppScope::class)
interface CommonDataProviders {
  @Provides
  @SingleIn(AppScope::class)
  fun provideJson(): Json {
    return Json { ignoreUnknownKeys = true }
  }

  @Provides
  @SingleIn(AppScope::class)
  fun provideTokenManager(httpClient: HttpClient, tokenStorage: TokenStorage): TokenManager {
    val authApi = PetfinderAuthApiImpl(httpClient)
    return TokenManager(authApi, tokenStorage)
  }


  /** Qualifier to denote that a provided type is authenticated. */
  @Qualifier
  annotation class Authenticated

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
  fun provideHttpClient(
    httpClientEngineFactory: HttpClientEngineFactory<*>,
    json: Json,
  ): HttpClient =
    HttpClient(httpClientEngineFactory) {
      expectSuccess = true
      install(HttpRequestRetry) {
        retryOnExceptionOrServerErrors(maxRetries = 2)
        exponentialDelay()
      }
      install(ContentNegotiation) { json(json) }
    }

  @Provides
  @SingleIn(AppScope::class)
  fun providePetfinderApi(@Authenticated httpClient: HttpClient): PetfinderApi = PetfinderApiImpl(httpClient)

  @Provides
  @SingleIn(AppScope::class)
  fun providePetBioApi(httpClient: HttpClient): PetBioParserApi = PetBioParserApiImpl(httpClient)

  @Provides
  @SingleIn(AppScope::class)
  fun provideFileSystem(): FileSystem = FileSystem.SYSTEM
}

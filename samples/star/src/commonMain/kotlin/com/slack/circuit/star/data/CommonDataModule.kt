// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import com.slack.circuit.star.data.petfinder.PetBioParserApi
import com.slack.circuit.star.data.petfinder.PetBioParserApiImpl
import com.slack.circuit.star.data.petfinder.PetfinderApi
import com.slack.circuit.star.data.petfinder.PetfinderApiImpl
import com.slack.circuit.star.data.petfinder.PetfinderAuthApiImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okio.FileSystem

object CommonDataModule {
  fun provideJson(): Json {
    return Json { ignoreUnknownKeys = true }
  }

  fun provideTokenManager(httpClient: HttpClient, tokenStorage: TokenStorage): TokenManager {
    val authApi = PetfinderAuthApiImpl(httpClient)
    return TokenManager(authApi, tokenStorage)
  }

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

  fun providePetfinderApi(httpClient: HttpClient): PetfinderApi = PetfinderApiImpl(httpClient)

  fun providePetBioApi(httpClient: HttpClient): PetBioParserApi = PetBioParserApiImpl(httpClient)

  fun provideFileSystem(): FileSystem = FileSystem.SYSTEM
}

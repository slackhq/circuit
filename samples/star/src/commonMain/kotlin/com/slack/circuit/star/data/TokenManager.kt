// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import com.slack.circuit.star.data.petfinder.PetfinderAuthApi
import com.slack.circuit.star.data.petfinder.updateAuthData
import com.slack.eithernet.ApiResult.Failure
import com.slack.eithernet.ApiResult.Success
import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A hypothetical token manager that stores an auth token and can refresh the token as needed. */
class TokenManager(private val api: PetfinderAuthApi, private val tokenStorage: TokenStorage) {
  private val mutex = Mutex()

  /** Returns the current [BearerTokens] (if present) that can be used to authenticate requests. */
  suspend fun last(): BearerTokens? {
    val (_, _, token) = tokenStorage.getAuthData() ?: return null
    return BearerTokens(token, "never used")
  }

  /** Refreshes the current auth token. */
  suspend fun refreshToken() =
    mutex.withLock {
      println("INFO: Refreshing token")
      when (val result = api.authenticate()) {
        is Success -> tokenStorage.updateAuthData(result.value)
        is Failure -> {
          println("ERROR: Failed to refresh token: $result")
        }
      }
    }
}

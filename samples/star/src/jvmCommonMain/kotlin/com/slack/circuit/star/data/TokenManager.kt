// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import com.slack.circuit.star.data.petfinder.PetfinderAuthApi
import com.slack.circuit.star.data.petfinder.updateAuthData
import com.slack.eithernet.ApiResult.Failure
import com.slack.eithernet.ApiResult.Success
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock.System

/**
 * A hypothetical token manager that stores an auth token. Just in-memory and not thread-safe for
 * now.
 */
class TokenManager(private val api: PetfinderAuthApi, private val tokenStorage: TokenStorage) {
  private val mutex = Mutex()

  data class AuthHeader(val name: String, val value: String)

  /** Returns an [AuthHeader] that can be used to authenticate requests. */
  suspend fun requestAuthHeader(): AuthHeader {
    return requestAuthHeader(false)
  }

  private suspend fun requestAuthHeader(isAfterRefresh: Boolean): AuthHeader {
    println("INFO: Authenticating request")
    val (tokenType, expiration, token) =
      tokenStorage.getAuthData()
        ?: run {
          refreshToken()
          return requestAuthHeader(isAfterRefresh)
        }
    if (System.now() > expiration) {
      check(!isAfterRefresh)
      refreshToken()
      return requestAuthHeader(isAfterRefresh)
    } else {
      return AuthHeader("Authorization", "$tokenType $token")
    }
  }

  private suspend fun refreshToken() =
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

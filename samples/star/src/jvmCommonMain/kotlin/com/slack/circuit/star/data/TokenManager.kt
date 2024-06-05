package com.slack.circuit.star.data

import com.slack.circuit.star.data.petfinder.PetfinderAuthApi
import com.slack.circuit.star.data.petfinder.updateAuthData
import com.slack.eithernet.ApiResult.Failure
import com.slack.eithernet.ApiResult.Success
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock.System
import okhttp3.Request

/**
 * A hypothetical token manager that stores an auth token. Just in-memory and not thread-safe for
 * now.
 */
class TokenManager(private val api: PetfinderAuthApi, private val tokenStorage: TokenStorage) {
  private val mutex = Mutex()

  /** Authenticates a [request]. */
  suspend fun authenticate(request: Request): Request {
    return authenticate(request, false)
  }

  private suspend fun authenticate(request: Request, isAfterRefresh: Boolean): Request {
    println("INFO: Authenticating request ${request.url}")
    val newBuilder = request.newBuilder()
    val (tokenType, expiration, token) =
      tokenStorage.getAuthData()
        ?: run {
          refreshToken()
          return authenticate(request, isAfterRefresh)
        }
    if (System.now() > expiration) {
      check(!isAfterRefresh)
      refreshToken()
      return authenticate(request, isAfterRefresh)
    } else {
      newBuilder.addHeader("Authorization", "$tokenType $token")
    }

    return newBuilder.build()
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
// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.data

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import org.junit.Test

class TokenManagerTest {
  @Test
  fun happyPath() = runTest {
    val authApi = FakeAuthApi()
    val tokenStorage =
      FakeTokenStorage().apply { updateAuthData(AuthenticationResponse("Bearer", 1000, "token")) }
    val tokenManager = TokenManager(authApi, tokenStorage)
    val request = Request.Builder().url("https://api.petfinder.com/v2/animals").build()
    val authenticated = tokenManager.authenticate(request)
    assertThat(authenticated.header("Authorization")).isEqualTo("Bearer token")
  }

  @Test
  fun expires() = runTest {
    val authApi = FakeAuthApi()
    authApi.enqueuedResponses.add(AuthenticationResponse("Bearer", 1000, "queuedToken"))
    val tokenStorage =
      FakeTokenStorage().apply { updateAuthData(AuthenticationResponse("Bearer", 0, "token")) }
    val tokenManager = TokenManager(authApi, tokenStorage)
    val request = Request.Builder().url("https://api.petfinder.com/v2/animals").build()
    val authenticated = tokenManager.authenticate(request)
    assertThat(authenticated.header("Authorization")).isEqualTo("Bearer queuedToken")
    assertThat(authApi.enqueuedResponses).isEmpty()
    assertThat(tokenStorage.getAuthData()!!.token).isEqualTo("queuedToken")
  }
}

private class FakeAuthApi : PetfinderAuthApi {
  val enqueuedResponses = mutableListOf<AuthenticationResponse>()

  override suspend fun authenticate(
    clientId: String,
    secret: String,
    grantType: String,
  ): AuthenticationResponse {
    return enqueuedResponses.removeFirst()
  }
}

private class FakeTokenStorage : TokenStorage {
  var authData: AuthenticationData? = null
  override suspend fun updateAuthData(authData: AuthenticationData) {
    this.authData = authData
  }

  override suspend fun getAuthData(): AuthenticationData? = authData
}

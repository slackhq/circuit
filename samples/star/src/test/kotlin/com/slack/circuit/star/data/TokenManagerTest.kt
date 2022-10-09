/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
